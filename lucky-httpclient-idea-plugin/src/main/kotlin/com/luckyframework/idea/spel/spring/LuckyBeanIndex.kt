package com.luckyframework.idea.spel.spring

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import java.beans.Introspector

/**
 * Lucky HttpClient 组件（`@HttpClient` / `@LuckyComponent`）的 Bean 索引。
 *
 * Spring 插件只把「直接标注 `@Component` 的具体类」当作 Bean：Lucky 的
 * `@HttpClient` 通过 `@HttpClientComponent` 间接带 `@Component`，且组件必须是
 * 接口/抽象类，真正的 Bean 由 `LuckyHttpClientImportBeanDefinitionRegistrar`
 * 在运行期注册，静态 Spring 模型看不到。这里按运行期的注册规则补一份：
 *
 * - 只收集接口/抽象类（`isLuckyHttpComponent` 要求非具体类）；
 * - Bean 名取 `@HttpClient#beanId` / `@LuckyComponent#name` /
 *   `@HttpClientComponent#name`，缺省为 `Introspector.decapitalize(类名)`
 *   （与 `generateBeanName` 一致，`MockApi` → `mockApi`）；
 * - 声明 PSI 用接口本身，`@Bean` 补全、跳转和类型推导都能复用。
 *
 * 结果按模块缓存，PSI 变化后由平台丢弃；dumb mode 返回空，避免访问索引。
 */
internal object LuckyBeanIndex {

    /** 直接标注在组件接口上的注解及其 Bean 名属性。 */
    private val NAME_ATTRIBUTES = listOf(
        "io.github.lucklike.httpclient.discovery.HttpClient" to "beanId",
        "io.github.lucklike.httpclient.annotation.LuckyComponent" to "name",
        "io.github.lucklike.httpclient.annotation.HttpClientComponent" to "name"
    )

    /** 参与搜索的组件注解；元注解链上的 `@HttpClientComponent` 也单独搜索。 */
    private val COMPONENT_ANNOTATIONS = listOf(
        "io.github.lucklike.httpclient.discovery.HttpClient",
        "io.github.lucklike.httpclient.annotation.LuckyComponent",
        "io.github.lucklike.httpclient.annotation.HttpClientComponent"
    )

    private val cacheKey: Key<CachedValue<List<SpringBeanDefinition>>> =
        Key.create("lucky.httpclient.spring.bean.model")

    fun beansOf(module: Module): List<SpringBeanDefinition> {
        if (DumbService.isDumb(module.project)) {
            return emptyList()
        }
        return CachedValuesManager.getManager(module.project).getCachedValue(
            module,
            cacheKey,
            CachedValueProvider<List<SpringBeanDefinition>> {
                CachedValueProvider.Result.create(
                    scan(module),
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            },
            false
        )
    }

    private fun scan(module: Module): List<SpringBeanDefinition> {
        val project = module.project
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, true)
        val result = LinkedHashMap<String, SpringBeanDefinition>()
        for (annotation in componentAnnotations(project, scope)) {
            val classes = runCatching {
                AnnotatedElementsSearch.searchPsiClasses(annotation, scope).findAll()
            }.getOrElse { emptyList() }
            for (psiClass in classes) {
                // 组合注解（如 `@StdHttpClient`）也是被 `@HttpClient` 标注的注解，
                // 会出现在搜索结果里，但不是组件
                if (psiClass.isAnnotationType || !isComponent(psiClass)) {
                    continue
                }
                val name = beanNameOf(psiClass) ?: continue
                if (result.containsKey(name)) {
                    continue
                }
                result[name] = SpringBeanDefinition(
                    name = name,
                    aliases = emptyList(),
                    typeText = psiClass.qualifiedName ?: psiClass.name,
                    beanClass = psiClass,
                    declaration = psiClass,
                    fileName = psiClass.containingFile?.name,
                    // 与 Spring 模型里的接口 Bean 一致：取 Spring 的抽象 Bean 图标；
                    // 社区版桥接不可用时为 null，由补全渲染回退平台图标
                    icon = SpringBeanModelBridge.beanIcon(psiClass, psiClass)
                )
            }
        }
        return result.values.toList()
    }

    /**
     * 参与搜索的注解：三个根注解本身，以及标注了它们的组合注解
     * （`@StdHttpClient` 等）。运行期 `isLuckyHttpComponent` 用
     * `AnnotatedElementUtils.isAnnotated` 会追踪元注解，这里保持一致，
     * 否则组合注解接口不会被补充为 Bean。
     */
    private fun componentAnnotations(project: Project, scope: GlobalSearchScope): List<PsiClass> {
        val facade = JavaPsiFacade.getInstance(project)
        val result = LinkedHashSet<PsiClass>()
        val queue = ArrayDeque<PsiClass>()
        for (fqn in COMPONENT_ANNOTATIONS) {
            facade.findClass(fqn, scope)?.let { queue += it }
        }
        while (queue.isNotEmpty() && result.size < MAX_ANNOTATIONS) {
            val annotation = queue.removeFirst()
            if (!result.add(annotation)) {
                continue
            }
            val annotated = runCatching {
                AnnotatedElementsSearch.searchPsiClasses(annotation, scope).findAll()
            }.getOrElse { emptyList() }
            for (psiClass in annotated) {
                // 只有注解类型才可能继续携带元注解；组件类在 scan 里统一处理
                if (psiClass.isAnnotationType) {
                    queue += psiClass
                }
            }
        }
        return result.toList()
    }

    /** 与 `isLuckyHttpComponent` 一致：非注解、非具体类（接口或抽象类）。 */
    private fun isComponent(psiClass: PsiClass): Boolean {
        if (!psiClass.isValid || psiClass.isAnnotationType) {
            return false
        }
        return psiClass.isInterface || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)
    }

    private fun beanNameOf(psiClass: PsiClass): String? {
        for ((fqn, attribute) in NAME_ATTRIBUTES) {
            val annotation = findAnnotation(psiClass, fqn, 0) ?: continue
            val value = attributeText(annotation, attribute)
            if (value != null) {
                return value
            }
        }
        val shortName = psiClass.name ?: return null
        return Introspector.decapitalize(shortName)
    }

    /**
     * 在类的注解链上查找注解，追踪组合注解：`@StdHttpClient` 上的
     * `@HttpClient` 也要能找到，运行期的 `AnnotatedElementUtils` 就是这样解析的。
     */
    private fun findAnnotation(psiClass: PsiClass, fqn: String, depth: Int): PsiAnnotation? {
        if (depth > MAX_ANNOTATION_DEPTH) {
            return null
        }
        psiClass.getAnnotation(fqn)?.let { return it }
        for (annotation in psiClass.annotations) {
            val owner = annotation.nameReferenceElement?.resolve() as? PsiClass ?: continue
            if (owner.qualifiedName == fqn) {
                continue
            }
            findAnnotation(owner, fqn, depth + 1)?.let { return it }
        }
        return null
    }

    private fun attributeText(annotation: PsiAnnotation, name: String): String? {
        val value = annotation.findAttributeValue(name) ?: return null
        val text = if (value is PsiLiteralExpression) {
            value.value as? String
        } else {
            value.text.trim().trim('"', '\'')
        }
        return text?.takeIf { it.isNotBlank() }
    }

    /** 组合注解最多追踪的层数，避免异常注解链死循环。 */
    private const val MAX_ANNOTATION_DEPTH = 4

    /** 一次扫描最多参与搜索的注解数量，防止误把整条元注解链都当成组件注解。 */
    private const val MAX_ANNOTATIONS = 32
}
