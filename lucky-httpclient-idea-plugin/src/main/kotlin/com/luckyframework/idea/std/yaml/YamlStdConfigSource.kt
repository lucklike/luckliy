package com.luckyframework.idea.std.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.std.StdConfigNode
import com.luckyframework.idea.std.StdConfigSource
import org.jetbrains.yaml.YAMLFileType

/**
 * 从项目 YAML 文件收集 `standard-client-configs` 节点（YAML 插件存在时加载）。
 *
 * 文件级 `CachedValue` 以 `PsiFile` 为依赖：未修改的文件在索引重建时直接命中，
 * 只有发生编辑的文件才重新解析。文件范围受设置页控制。
 */
class YamlStdConfigSource : StdConfigSource {

    override fun nodes(project: Project): Map<String, List<StdConfigNode>> {
        val result = LinkedHashMap<String, MutableList<StdConfigNode>>()
        val scope = GlobalSearchScope.projectScope(project)
        val files = LinkedHashSet<VirtualFile>()
        files.addAll(FileTypeIndex.getFiles(YAMLFileType.YML, scope))
        for (virtualFile in files) {
            if (!virtualFile.isValid || !inScope(virtualFile)) {
                continue
            }
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
            for (node in cachedNodes(psiFile)) {
                result.getOrPut(node.configId) { mutableListOf() }.add(node)
            }
        }
        return result
    }

    private fun cachedNodes(file: PsiFile): List<StdConfigNode> =
        CachedValuesManager.getCachedValue(
            file,
            CachedValueProvider<List<StdConfigNode>> {
                CachedValueProvider.Result.create(StdConfigLocator.locate(file), file)
            }
        )

    companion object {

        /**
         * 文件是否在配置节点收集范围内（设置页控制）。
         *
         * 索引与配置文件侧行标记共用同一判定，避免出现「接口不认这份配置、
         * 配置文件却显示反跳图标」的不一致。
         */
        fun inScope(file: VirtualFile): Boolean {
            if (LuckyHttpClientSettings.getInstance().stdConfigFileScope !=
                LuckyHttpClientSettings.SCOPE_APPLICATION_ONLY
            ) {
                return true
            }
            val name = file.name.lowercase()
            val isApplication = name.startsWith("application") || name.startsWith("bootstrap")
            return isApplication && (name.endsWith(".yml") || name.endsWith(".yaml"))
        }
    }
}
