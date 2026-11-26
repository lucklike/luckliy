package com.luckyframework.idea.spel.symbol

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.LuckyModificationTrackers
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelValueKind
import java.util.Collections
import java.util.WeakHashMap

/**
 * 参数注入字面量的注入源：调用方方法的 SpEL 上下文。
 *
 * 运行期 `Context#getMethodParamObject(funcMethod)` 在**调用方**方法的 `Context`
 * 上求值注入表达式：`@Post(func = "indexFunc")` 执行函数时复用调用方的变量表，
 * `@Rar("a")` 拼出的 `#{a}` 取的是调用方参数 `a`，函数自身的参数不在求值
 * 上下文中（它们正被逐个计算）。
 *
 * 因此按以下规则定位注入源：
 *
 * - 在函数所在类的继承体系（自身、父类型、项目内子类型）内查找
 *   `kind = FUNCTION` 且值等于函数名的注解属性字面量，其所在方法即调用方；
 * - 每个调用方的符号上下文（方法参数、方法级/类级 `@SpELImport`、运行时符号）
 *   即该次调用的注入求值上下文，多个调用方取并集；
 * - 找不到调用方（函数未被项目内引用、只在外部被调用）时返回空，由
 *   [SpelContextBuilder] 回退为不含函数自身参数的上下文。
 */
internal object SpelInjectionSources {

    /**
     * 注入字面量对应的调用方上下文列表；字面量不在参数上或找不到调用方时为空。
     */
    fun callerContextsOf(literal: PsiLiteralExpression): List<SpelContext> {
        val parameter = PsiTreeUtil.getParentOfType(literal, PsiParameter::class.java, true)
            ?: return emptyList()
        val function = PsiTreeUtil.getParentOfType(parameter, PsiMethod::class.java, false)
            ?: return emptyList()
        if (function.name.isEmpty()) {
            return emptyList()
        }
        return contextsOf(function)
    }

    /**
     * 按函数方法缓存调用方上下文。
     *
     * 绑定签名计数（结构 + 声明类型）：调用方参数类型变化后注入上下文必须
     * 重算，但符号表与组件清单不受影响，不能因此重启全项目扫描。
     * 方法体编辑不改变签名，直接命中缓存。
     */
    private fun contextsOf(function: PsiMethod): List<SpelContext> {
        if (DumbService.isDumb(function.project)) {
            return emptyList()
        }
        val stamp = LuckyModificationTrackers.signature(function.project).modificationCount
        val cached = STALE[function]
        if (cached != null && cached.stamp == stamp) {
            return cached.contexts
        }
        val startNs = System.nanoTime()
        val computed = computeCallerContexts(function)
        val costMs = (System.nanoTime() - startNs) / 1_000_000
        // 扫描在高亮线程上同步执行，慢调用需要可定位；默认级别不为每次重建付费
        logger.debug(
            "[LuckyPerf] injection rebuild: method=${function.name} " +
                "cost=${costMs}ms callers=${computed.size} first=${cached == null}"
        )
        STALE[function] = Entry(stamp, computed)
        return computed
    }

    private class Entry(val stamp: Long, val contexts: List<SpelContext>)

    private fun computeCallerContexts(function: PsiMethod): List<SpelContext> =
        callerLiterals(function).mapNotNull { literal ->
            CONTEXT_BUILDER.build(literal).takeIf { it.known }
        }

    /**
     * 源码中按函数名调用该函数的全部字面量（`@Post(func = "...")` 等）。
     *
     * 扫描函数所在类的继承体系：函数通常声明在 API 接口（或父接口）中，
     * 调用方方法写在同一接口、父接口或子接口/实现类上。只查找项目内源码类，
     * 依赖 jar 中的类不可能引用项目内的函数名。
     */
    private fun callerLiterals(function: PsiMethod): List<PsiLiteralExpression> {
        val owner = function.containingClass ?: return emptyList()
        val resolver = SpelAnnotationResolver()
        val result = ArrayList<PsiLiteralExpression>()
        for (psiClass in candidateClasses(owner)) {
            if (!psiClass.isValid) {
                continue
            }
            collectLiterals(psiClass.modifierList?.annotations, function, resolver, result)
            for (method in psiClass.methods) {
                // 函数自身声明上的属性不是「调用」
                if (method == function) {
                    continue
                }
                collectLiterals(method.modifierList?.annotations, function, resolver, result)
            }
        }
        return result.distinct()
    }

    /** 自身、父类型与项目内全部子类型（含接口实现）。 */
    private fun candidateClasses(owner: PsiClass): List<PsiClass> {
        val result = LinkedHashSet<PsiClass>()
        collectSupertypes(owner, result)
        runCatching {
            ClassInheritorsSearch.search(owner, GlobalSearchScope.projectScope(owner.project), true)
                .forEach { result += it }
        }
        return result.toList()
    }

    private fun collectSupertypes(psiClass: PsiClass?, result: MutableSet<PsiClass>) {
        if (psiClass == null || !result.add(psiClass)) {
            return
        }
        collectSupertypes(psiClass.superClass, result)
        psiClass.interfaces.forEach { collectSupertypes(it, result) }
    }

    /**
     * 收集注解属性中「函数名等于 [function] 名字」的字面量。
     *
     * 用 `kind = FUNCTION` 判定属性参与值解析，再按函数名精确匹配；
     * `@Post(func = "...")`/`@HttpRequest(func = "...")` 等组合注解经
     * `@AliasFor` 链统一命中。
     */
    private fun collectLiterals(
        annotations: Array<PsiAnnotation>?,
        function: PsiMethod,
        resolver: SpelAnnotationResolver,
        result: MutableList<PsiLiteralExpression>
    ) {
        if (annotations == null) {
            return
        }
        for (annotation in annotations) {
            for (pair in annotation.parameterList.attributes) {
                val literal = pair.value as? PsiLiteralExpression ?: continue
                if (resolver.resolveLiteral(literal)?.kind != SpelValueKind.FUNCTION) {
                    continue
                }
                if (resolver.functionNameOf(literal) != function.name) {
                    continue
                }
                result += literal
            }
        }
    }

    private val CONTEXT_BUILDER = SpelContextBuilder()

    private val logger = Logger.getInstance(SpelInjectionSources::class.java)

    /** 弱键快照：PsiMethod 重新解析后旧条目随键一起回收，不持有失效 PSI。 */
    private val STALE: MutableMap<PsiMethod, Entry> =
        Collections.synchronizedMap(WeakHashMap<PsiMethod, Entry>())
}
