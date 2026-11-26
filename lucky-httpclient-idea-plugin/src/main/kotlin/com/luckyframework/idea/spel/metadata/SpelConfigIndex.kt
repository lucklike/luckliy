package com.luckyframework.idea.spel.metadata

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.InheritanceUtil
import com.intellij.util.Alarm
import com.luckyframework.idea.LuckyModificationTrackers
import java.util.concurrent.atomic.AtomicBoolean

/**
 * `@SpelConfig` 配置模型的项目级索引。
 *
 * 扫描项目中（含依赖）所有标注 `@SpelConfig` 的类，把类上的
 * `prefix`/`aliases`/`naming`/`scope` 与字段上的 `@SpelAttribute` 组合成
 * [SpelConfigModel]。字段 key 显式声明优先，未声明时按模型 `naming` 由
 * 字段名（getter 取属性名）推导；继承字段一并纳入，子类同名优先。
 *
 * 快照以 Java 声明结构计数为版本号：只有类、成员或注解声明变化才触发重扫，
 * 方法体编辑与 YAML 等其他语言编辑不会让索引失效；过期快照由后台重建，
 * 重建期间访问者继续使用旧模型，避免高亮线程等待全项目扫描；dumb 模式下
 * 返回空结果且不写缓存。
 */
@Service(Service.Level.PROJECT)
class SpelConfigIndex(private val project: Project) {

    private class CacheEntry(val stamp: Long, val models: List<SpelConfigModel>)

    @Volatile
    private var cache: CacheEntry? = null

    /** 正在后台重扫的标记，避免失效期间重复排队。 */
    private val rebuilding = AtomicBoolean(false)

    /** 重建去抖器：连续失效只保留最后一次。 */
    private val rebuildAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)

    fun models(): List<SpelConfigModel> {
        if (DumbService.isDumb(project)) {
            return emptyList()
        }
        val stamp = LuckyModificationTrackers.structure(project).modificationCount
        val cached = cache
        if (cached != null && cached.stamp == stamp) {
            return cached.models
        }
        if (cached != null && !ApplicationManager.getApplication().isUnitTestMode) {
            // 声明结构变化后先返回旧模型并在后台重扫：全项目 @SpelConfig 扫描
            // 放在高亮线程上，会让注解编辑的每一次击键都等待扫描完成。
            scheduleRebuild()
            return cached.models
        }
        synchronized(this) {
            val current = cache
            if (current != null && current.stamp == stamp) {
                return current.models
            }
            val built = build()
            cache = CacheEntry(stamp, built)
            return built
        }
    }

    /** 同一时刻只跑一个后台重扫；写入的版本号取自重扫时刻。 */
    private fun scheduleRebuild() {
        // 全项目 @SpelConfig 扫描与正在跑的 daemon pass 争抢 CPU 时，高亮会
        // 晚到数秒：连续编辑时每次失效都重置计时，只在停手后跑一次。
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
                if (cache?.stamp == stamp) {
                    return@compute
                }
                cache = CacheEntry(stamp, build())
            }
        } finally {
            rebuilding.set(false)
        }
    }

    fun model(fqName: String): SpelConfigModel? = models().firstOrNull { it.fqName == fqName }

    fun modelOf(psiClass: PsiClass): SpelConfigModel? {
        val name = psiClass.qualifiedName ?: return null
        return model(name)
    }

    /** 设置或项目模型变化后清空缓存。 */
    fun invalidate() {
        cache = null
    }

    private fun build(): List<SpelConfigModel> {
        val annotation = JavaPsiFacade.getInstance(project)
            .findClass(SPEL_CONFIG, GlobalSearchScope.allScope(project))
            ?: return emptyList()
        val reader = SpelMetaAnnotationReader()
        val classes = runCatching {
            AnnotatedElementsSearch.searchPsiClasses(annotation, GlobalSearchScope.allScope(project))
                .findAll()
                .toList()
        }.getOrElse { return emptyList() }
        return classes.mapNotNull { modelOf(it, reader) }
    }

    private fun modelOf(psiClass: PsiClass, reader: SpelMetaAnnotationReader): SpelConfigModel? {
        val config = reader.readConfig(psiClass) ?: return null
        val fields = LinkedHashMap<String, SpelConfigField>()
        for (field in fieldsOf(psiClass)) {
            addField(fields, reader, config.naming, field, field.name, field.type)
        }
        for (method in methodsOf(psiClass)) {
            val property = propertyNameOf(method.name)
            addField(fields, reader, config.naming, method, property, method.returnType)
        }
        return SpelConfigModel(config, psiClass, fields)
    }

    /** 本类字段在前、父类字段在后：同名字段保留子类声明。 */
    private fun fieldsOf(psiClass: PsiClass): List<PsiField> {
        val result = ArrayList<PsiField>()
        result.addAll(psiClass.fields)
        for (parent in psiClass.supers) {
            result.addAll(parent.allFields)
        }
        return result
    }

    /** 本类方法在前、父类方法在后；构造器不参与。 */
    private fun methodsOf(psiClass: PsiClass): List<PsiMethod> {
        val result = ArrayList<PsiMethod>()
        result.addAll(psiClass.methods.filterNot { it.isConstructor })
        for (parent in psiClass.supers) {
            result.addAll(parent.allMethods.filterNot { it.isConstructor })
        }
        return result
    }

    private fun addField(
        target: MutableMap<String, SpelConfigField>,
        reader: SpelMetaAnnotationReader,
        naming: String,
        owner: PsiModifierListOwner,
        property: String,
        type: PsiType?
    ) {
        val attribute = reader.readAttribute(owner) ?: return
        val key = attribute.key.takeIf { it.isNotBlank() } ?: keyOf(naming, property)
        if (key.isEmpty()) {
            return
        }
        val names = typeNamesOf(type)
        target.putIfAbsent(key, SpelConfigField(key, attribute, names.declared, names.value, owner, names.map))
    }

    // ---------------------------------------------------------------------------------------------
    // key 推导与类型解析
    // ---------------------------------------------------------------------------------------------

    /** 字段名/属性名按模型 `naming` 归一为配置 key。 */
    private fun keyOf(naming: String, property: String): String = when (naming) {
        NAMING_KEBAB_CASE -> camelToDelimited(property, '-')
        NAMING_SNAKE_CASE -> camelToDelimited(property, '_')
        NAMING_CAMEL_CASE -> delimitedToCamel(property)
        else -> property
    }

    /** `baseUrl` → `base-url`；连续大写按最后一个大写字母断开（`URLPath` → `url-path`）。 */
    private fun camelToDelimited(name: String, delimiter: Char): String {
        val builder = StringBuilder(name.length + 4)
        for ((index, c) in name.withIndex()) {
            if (c.isUpperCase()) {
                val nextLower = index + 1 < name.length && name[index + 1].isLowerCase()
                if (index > 0 && (name[index - 1].isLowerCase() || nextLower)) {
                    builder.append(delimiter)
                }
                builder.append(c.lowercaseChar())
            } else {
                builder.append(c)
            }
        }
        return builder.toString()
    }

    /** `base_url` / `base-url` / `base url` → `baseUrl`。 */
    private fun delimitedToCamel(name: String): String {
        val builder = StringBuilder(name.length)
        var upper = false
        for (c in name) {
            if (c == '_' || c == '-' || c == ' ') {
                upper = builder.isNotEmpty()
                continue
            }
            builder.append(if (upper) c.uppercaseChar() else c)
            upper = false
        }
        return builder.toString()
    }

    /** getter 取 JavaBean 属性名；非 getter 原样返回。 */
    private fun propertyNameOf(method: String): String = when {
        method.length > 3 && method.startsWith("get") -> decapitalize(method.substring(3))
        method.length > 2 && method.startsWith("is") -> decapitalize(method.substring(2))
        else -> method
    }

    /** JavaBean 规则：前两个字母都大写时原样保留（`URL` → `URL`）。 */
    private fun decapitalize(name: String): String =
        if (name.length > 1 && name[0].isUpperCase() && name[1].isUpperCase()) {
            name
        } else {
            name.replaceFirstChar { it.lowercaseChar() }
        }

    private class TypeNames(val declared: String?, val value: String?, val map: Boolean)

    /**
     * 声明类型与嵌套解析类型：数组/集合取元素类型、Map 取 value 类型，
     * 其余取声明类型本身。
     *
     * 这里不能用 `isInheritor` 单独判断：`Map`/`Collection` 自身不算是自己的子类型。
     */
    private fun typeNamesOf(type: PsiType?): TypeNames {
        if (type == null) {
            return TypeNames(null, null, false)
        }
        val declared = qualifiedNameOf(type)
        val resolved = (type as? PsiClassType)?.resolve()
        val map = resolved != null && isType(resolved, JAVA_UTIL_MAP)
        val nested = when {
            type is PsiArrayType -> type.componentType
            type is PsiClassType && resolved != null -> when {
                map -> type.parameters.getOrNull(1)
                isType(resolved, JAVA_UTIL_COLLECTION) -> type.parameters.getOrNull(0)
                else -> null
            }
            else -> null
        }
        return TypeNames(declared, nested?.let(::qualifiedNameOf) ?: declared, map)
    }

    private fun isType(resolved: PsiClass, fqName: String): Boolean =
        resolved.qualifiedName == fqName || InheritanceUtil.isInheritor(resolved, fqName)

    private fun qualifiedNameOf(type: PsiType): String? = when (type) {
        is PsiArrayType -> qualifiedNameOf(type.componentType)?.plus("[]")
        is PsiClassType -> type.resolve()?.qualifiedName ?: type.canonicalText
        else -> type.canonicalText
    }

    companion object {
        private const val SPEL_CONFIG = SpelMetaAnnotationReader.SPEL_CONFIG
        private const val REBUILD_DELAY_MS = 1000L
        private const val JAVA_UTIL_MAP = "java.util.Map"
        private const val JAVA_UTIL_COLLECTION = "java.util.Collection"
        private const val NAMING_KEBAB_CASE = "KEBAB_CASE"
        private const val NAMING_SNAKE_CASE = "SNAKE_CASE"
        private const val NAMING_CAMEL_CASE = "CAMEL_CASE"

        fun getInstance(project: Project): SpelConfigIndex = project.service()
    }
}
