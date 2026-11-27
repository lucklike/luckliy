package com.luckyframework.idea.spel.spring

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import javax.swing.Icon

/**
 * Spring Bean 模型中的一个 Bean。
 *
 * [name] 是主 Bean 名，[aliases] 是 `@Bean({"a", "b"})` 等声明的别名；
 * [beanClass] 是 Bean 的实际类型（`@Bean` 方法返回类型 / 组件类），
 * [declaration] 是声明 PSI（`@Bean` 方法、`@Component` 类或 XML 标签），
 * [icon] 是 Spring 插件给出的 Bean 图标（@Bean 方法/组件类/抽象 Bean 各不相同），
 * 补全文档与 Ctrl+Click 都指向 [declaration]。
 */
internal class SpringBeanDefinition(
    val name: String,
    val aliases: List<String>,
    val typeText: String?,
    val beanClass: PsiClass?,
    val declaration: PsiElement?,
    val fileName: String?,
    val icon: Icon?
) {
    /** 主名与别名都可以用于 `#{@name}` 引用。 */
    val names: List<String>
        get() = if (aliases.isEmpty()) listOf(name) else listOf(name) + aliases
}

/**
 * Spring 插件 Bean 模型桥接（可选 Ultimate 能力）。
 *
 * 直接 API 调用放在 [SpringBeanModelAccessImpl]：本类只持有中性接口
 * [SpringBeanModelAccess]，Spring 插件缺失时不会请求该服务，实现类也就
 * 不会加载，社区版因此不会触发 [NoClassDefFoundError]。
 *
 * 相比内置的「类名首字母小写 + Impl」启发式，官方模型能识别：
 * `@Bean` 方法名、`@Bean("name")`/`@Bean(name = {...})`、`@Component("name")`、
 * 别名集合与 XML Bean，并能拿到真实的 Bean 类型与声明 PSI。
 *
 * 返回值约定：`null` 表示桥接不可用（没有 Spring 插件、模块没有 Spring 模型、
 * dumb mode 或读取失败），调用方应回退内置启发式；空列表表示模型存在，
 * 但模块里确实没有可识别的 Bean。
 */
internal object SpringBeanModelBridge {

    private const val SPRING_PLUGIN_ID = "com.intellij.spring"

    /**
     * Spring 插件存在时才向平台请求实现服务：实现类直接引用
     * `com.intellij.spring.*`，社区版不会触碰它。
     */
    private val access: SpringBeanModelAccess? by lazy {
        if (PluginManagerCore.getPlugin(PluginId.getId(SPRING_PLUGIN_ID)) == null) {
            return@lazy null
        }
        runCatching { service<SpringBeanModelAccess>() }.getOrNull()
    }

    private val cacheKey: Key<CachedValue<List<SpringBeanDefinition>?>> =
        Key.create("lucky.spring.bean.model")

    /** Spring 插件可用时才尝试桥接；单个模块的读取失败在 [beansOf] 内转为 null。 */
    val isAvailable: Boolean
        get() = access != null

    /** 当前元素所属模块的全部 Bean；null 表示桥接不可用。 */
    fun beans(anchor: PsiElement?): List<SpringBeanDefinition>? {
        val module = moduleOf(anchor) ?: return null
        return beansOf(module)
    }

    /**
     * Spring 插件对 Bean 声明给出的图标；桥接不可用（社区版）时返回 null，
     * 由调用方回退平台图标。
     */
    fun beanIcon(beanClass: PsiClass?, declaration: PsiElement?): Icon? =
        access?.let { runCatching { it.iconOf(beanClass, declaration) }.getOrNull() }

    /** 按主名或别名查找 Bean。 */
    fun bean(anchor: PsiElement?, name: String): SpringBeanDefinition? {
        if (name.isBlank()) {
            return null
        }
        return beans(anchor)?.firstOrNull { name == it.name || name in it.aliases }
    }

    /**
     * 名字匹配的 Bean 声明类；null 表示桥接不可用。
     *
     * `@Bean` 方法的声明 PSI 是方法本身，类型来自 `getBeanClass()`；
     * 只有拿不到 Bean 类型时才退回声明 PSI 中的类。
     */
    fun beanClasses(anchor: PsiElement?, name: String): List<PsiClass>? {
        val matched = beans(anchor)?.filter { name == it.name || name in it.aliases } ?: return null
        return matched
            .mapNotNull { it.beanClass ?: it.declaration as? PsiClass }
            .distinctBy { it.qualifiedName }
    }

    /** 当前元素所属模块；跨模块的 YAML/Java 文本都以所在模块的模型为准。 */
    fun moduleOf(anchor: PsiElement?): Module? =
        anchor?.let { runCatching { ModuleUtilCore.findModuleForPsiElement(it) }.getOrNull() }

    /**
     * Spring 模型本身按 PSI 版本缓存，这里再缓存「扁平化后的 Bean 列表」，
     * 避免每次补全都重新遍历整个模型；PSI 变化后由平台丢弃缓存。
     *
     * Lucky 的 `@HttpClient`/`@LuckyComponent` 接口不是 Spring 静态模型里的
     * Bean（运行期由 `ImportBeanDefinitionRegistrar` 注册），这里按注册规则
     * 合并 [LuckyBeanIndex] 的结果：Spring 模型没有该 Bean 时补充，已有同名
     * Bean 时不重复添加；没有 Spring 插件时也返回 Lucky 组件，让 `@Bean`
     * 检查与补全在社区版同样可用。
     */
    private fun beansOf(module: Module): List<SpringBeanDefinition>? {
        val springBeans = springBeansOf(module)
        val luckyBeans = LuckyBeanIndex.beansOf(module)
        if (springBeans == null) {
            return luckyBeans.ifEmpty { null }
        }
        if (luckyBeans.isEmpty()) {
            return springBeans
        }
        val names = springBeans.mapTo(HashSet()) { it.name }
        return springBeans + luckyBeans.filter { it.name !in names }
    }

    private fun springBeansOf(module: Module): List<SpringBeanDefinition>? {
        val access = access ?: return null
        if (DumbService.isDumb(module.project)) {
            return null
        }
        return CachedValuesManager.getManager(module.project).getCachedValue(
            module,
            cacheKey,
            CachedValueProvider<List<SpringBeanDefinition>?> {
                CachedValueProvider.Result.create(
                    runCatching { access.beans(module) }.getOrNull(),
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            },
            false
        )
    }
}
