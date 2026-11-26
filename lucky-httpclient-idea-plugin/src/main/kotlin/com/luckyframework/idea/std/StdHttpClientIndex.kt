package com.luckyframework.idea.std

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.util.Alarm
import com.luckyframework.idea.LuckyModificationTrackers
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.util.concurrent.atomic.AtomicBoolean

/**
 * `@StdHttpClient` 接口与配置文件的关联索引。
 *
 * Java 侧通过 `AnnotatedElementsSearch` 查询直接标注的类，再沿组合注解做 BFS
 * 传播；YAML 侧由 [StdConfigSource] 扩展点提供（未安装 YAML 插件时为空）。
 * 快照以 Java 声明结构计数、配置文件修改计数与项目根版本号为缓存键：
 * 方法体编辑或 YAML 编辑不会让另一侧的扫描重做；YAML 文件级缓存避免重复
 * 解析未修改的文件。版本变化后先返回旧快照，由后台重扫替换，避免高亮线程
 * 等待全项目扫描。
 *
 * dumb 模式下返回空快照且不写入缓存，索引就绪后自动恢复。
 */
@Service(Service.Level.PROJECT)
class StdHttpClientIndex(private val project: Project) {

    private class CacheEntry(
        val stamp: Long,
        val configStamp: Long,
        val rootsStamp: Long,
        val snapshot: StdClientIndexSnapshot
    ) {
        fun matches(stamp: Long, configStamp: Long, rootsStamp: Long): Boolean =
            this.stamp == stamp && this.configStamp == configStamp && this.rootsStamp == rootsStamp
    }

    @Volatile
    private var cache: CacheEntry? = null

    /** 正在后台重扫的标记，避免失效期间重复排队。 */
    private val rebuilding = AtomicBoolean(false)

    /** 重建去抖器：连续失效只保留最后一次。 */
    private val rebuildAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)

    fun snapshot(): StdClientIndexSnapshot {
        if (!LuckyHttpClientSettings.getInstance().isEnabled || DumbService.isDumb(project)) {
            return StdClientIndexSnapshot.EMPTY
        }
        val stamp = LuckyModificationTrackers.structure(project).modificationCount
        val configStamp = LuckyModificationTrackers.configFiles(project).modificationCount
        val rootsStamp = LuckyModificationTrackers.roots(project)
        val cached = cache
        if (cached != null && cached.matches(stamp, configStamp, rootsStamp)) {
            return cached.snapshot
        }
        if (cached != null && !ApplicationManager.getApplication().isUnitTestMode) {
            // 版本变化后先返回旧快照并在后台重扫：注解列表与配置节点的
            // 全项目扫描放在高亮线程上，会让每次编辑都等待扫描完成。
            scheduleRebuild()
            return cached.snapshot
        }
        synchronized(this) {
            val current = cache
            if (current != null && current.matches(stamp, configStamp, rootsStamp)) {
                return current.snapshot
            }
            val built = build()
            cache = CacheEntry(stamp, configStamp, rootsStamp, built)
            return built
        }
    }

    /** 同一时刻只跑一个后台重扫；写入的版本号取自重扫时刻。 */
    private fun scheduleRebuild() {
        // 注解列表与配置节点的全项目扫描与正在跑的 daemon pass 争抢 CPU 时，
        // 高亮会晚到数秒：连续编辑时每次失效都重置计时，只在停手后跑一次。
        rebuildAlarm.cancelAllRequests()
        rebuildAlarm.addRequest({ rebuild() }, REBUILD_DELAY_MS)
    }

    private fun rebuild() {
        if (!rebuilding.compareAndSet(false, true)) {
            return
        }
        try {
            ReadAction.compute<Unit, RuntimeException> {
                if (project.isDisposed || DumbService.isDumb(project)) {
                    return@compute
                }
                val stamp = LuckyModificationTrackers.structure(project).modificationCount
                val configStamp = LuckyModificationTrackers.configFiles(project).modificationCount
                val rootsStamp = LuckyModificationTrackers.roots(project)
                if (cache?.matches(stamp, configStamp, rootsStamp) == true) {
                    return@compute
                }
                cache = CacheEntry(stamp, configStamp, rootsStamp, build())
            }
        } finally {
            rebuilding.set(false)
        }
    }

    fun entryOf(psiClass: PsiClass): StdClientEntry? = snapshot().byClass[psiClass]

    fun entriesByConfigId(configId: String): List<StdClientEntry> =
        snapshot().byConfigId[configId].orEmpty()

    fun nodesOf(configId: String): List<StdConfigNode> =
        snapshot().configNodesByConfigId[configId].orEmpty()

    fun nodesOf(psiClass: PsiClass): List<StdConfigNode> =
        entryOf(psiClass)?.configNodes.orEmpty()

    /**
     * 方法级配置节点：`method-configs.<apiId>` 对应的 YAML 键。
     *
     * 返回 [PsiElement] 而不是 YAML 类型，保证 Java 侧类加载不依赖 YAML 插件。
     */
    fun methodNodesOf(method: PsiMethod): List<PsiElement> {
        val entry = entryOf(method.containingClass ?: return emptyList()) ?: return emptyList()
        val apiId = snapshot().apiIdByMethod[method] ?: return emptyList()
        return entry.configNodes.mapNotNull { it.methodConfigs[apiId] }
    }

    fun apiIdOf(method: PsiMethod): String? = snapshot().apiIdByMethod[method]

    /** 设置变更后清空缓存，下一次访问按新配置重建。 */
    fun invalidate() {
        cache = null
    }

    private fun build(): StdClientIndexSnapshot {
        val annotation = JavaPsiFacade.getInstance(project)
            .findClass(STD_HTTP_CLIENT, GlobalSearchScope.allScope(project))
            ?: return StdClientIndexSnapshot.EMPTY
        val configNodes = StdConfigSource.EP_NAME.extensionList
            .firstOrNull()
            ?.nodes(project)
            .orEmpty()
        val entries = scanClients(annotation, configNodes)
        val byClass = LinkedHashMap<PsiClass, StdClientEntry>()
        val byConfigId = LinkedHashMap<String, MutableList<StdClientEntry>>()
        val apiIdByMethod = LinkedHashMap<PsiMethod, String>()
        for (entry in entries) {
            byClass[entry.clientClass] = entry
            byConfigId.getOrPut(entry.configId) { mutableListOf() }.add(entry)
            for (method in entry.methods) {
                apiIdByMethod[method.method] = method.apiId
            }
        }
        return StdClientIndexSnapshot(
            active = true,
            entries = entries,
            byClass = byClass,
            byConfigId = byConfigId,
            configNodesByConfigId = configNodes,
            apiIdByMethod = apiIdByMethod
        )
    }

    /**
     * 直接标注的接口/类 + 组合注解传播。
     *
     * 注解类型本身只作为传播载体，不产出行标记（它的 `configId` 是默认值，
     * 不对应任何具体接口）。
     */
    private fun scanClients(
        annotation: PsiClass,
        configNodes: Map<String, List<StdConfigNode>>
    ): List<StdClientEntry> {
        val result = LinkedHashMap<PsiClass, StdClientEntry>()
        val queue = ArrayDeque<PsiClass>()
        val visited = HashSet<String>()
        queue.add(annotation)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val fqName = current.qualifiedName ?: continue
            if (!visited.add(fqName)) {
                continue
            }
            for (user in searchUsers(current)) {
                if (user.isAnnotationType) {
                    queue.add(user)
                    continue
                }
                if (result.containsKey(user)) {
                    continue
                }
                entryOf(user, configNodes)?.let { result[user] = it }
            }
        }
        return result.values.toList()
    }

    private fun searchUsers(annotation: PsiClass): List<PsiClass> =
        ArrayList(
            AnnotatedElementsSearch.searchPsiClasses(annotation, GlobalSearchScope.projectScope(project))
                .findAll()
        )

    private fun entryOf(
        psiClass: PsiClass,
        configNodes: Map<String, List<StdConfigNode>>
    ): StdClientEntry? {
        val name = psiClass.name ?: return null
        val configId = StdAnnotationValues.stdClientConfigId(psiClass)
            ?.takeIf { it.isNotEmpty() }
            ?: name
        val methods = psiClass.methods
            .filterNot { it.isConstructor }
            .map { method -> StdMethodEntry(method, StdAnnotationValues.apiId(method) ?: method.name) }
        return StdClientEntry(psiClass, configId, methods, configNodes[configId].orEmpty())
    }

    companion object {
        private const val STD_HTTP_CLIENT = "io.github.lucklike.httpclient.std.StdHttpClient"
        private const val REBUILD_DELAY_MS = 1000L

        fun getInstance(project: Project): StdHttpClientIndex = project.service()
    }
}
