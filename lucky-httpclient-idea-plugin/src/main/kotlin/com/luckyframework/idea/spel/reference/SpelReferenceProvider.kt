package com.luckyframework.idea.spel.reference

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.LuckyModificationTrackers
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.environment.SpringEnvironmentService
import com.luckyframework.idea.spel.environment.SpringPlaceholderReferences
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelInjection
import com.luckyframework.idea.spel.metadata.SpelInjectionScope
import com.luckyframework.idea.spel.metadata.SpelValueKind
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelContextBuilder

/**
 * 为字符串字面量中的 SpEL 片段创建引用（design D8）。
 *
 * 扫描、词法与引用收集由 [SpelReferenceCollector] 提供，YAML 侧共用；
 * 符号视图由 [SpelContextBuilder] 统一提供，未解析引用只返回空目标。
 *
 * `kind = FUNCTION` 的属性值可以不写 `#{}`，整段就是一个函数名：运行期
 * `Context#autoExecuteSpELOrFunc` 先解析属性值，不含 `#{}` 时原样作为函数名
 * 从变量表取 `Method`，因此这里把整段建为一个引用。参数注入名字属性
 * （`@Rar`/`@Var`/`@Bean`/`@Env`）同样整段是引用名，一并参与解析。
 */
class SpelReferenceProvider(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer(),
    private val resolver: SpelAnnotationResolver = SpelAnnotationResolver()
) : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val literal = element as? PsiLiteralExpression ?: return PsiReference.EMPTY_ARRAY
        val hasFragment = literal.text.contains("#{")
        // 函数名属性与参数注入名字属性整段不含 `#{}`，但同样参与解析；
        // 其它普通字符串直接跳过
        if (!hasFragment && !resolver.isFunctionNameLiteral(literal) &&
            !resolver.isInjectionNameAttribute(literal)
        ) {
            return PsiReference.EMPTY_ARRAY
        }
        if (DumbService.isDumb(literal.project) ||
            !LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)
        ) {
            return PsiReference.EMPTY_ARRAY
        }

        val symbols = contextOf(literal)
        // 不在 SpEL 求值上下文（普通字符串、未标注 @SpelAttribute 的属性）：
        // 不建立任何引用，避免悬停/跳转把普通文本当成 SpEL 符号。
        if (!symbols.known) {
            return PsiReference.EMPTY_ARRAY
        }

        if (!hasFragment) {
            val injection = resolver.resolveInjection(literal)
            if (injection != null && injection.kind == SpelValueKind.PARAMETER_INJECTION_VAR) {
                return injectionNameReferences(literal, symbols, injection)
            }
            return functionNameReferences(literal, symbols)
        }
        val navigation = SpelNavigation(literal.project, symbols, anchor = literal)
        return SpelReferenceCollector.collect(literal, literal.text, navigation, scanner, tokenizer)
            .toTypedArray()
    }

    /**
     * 函数名字面量整体作为一个引用，目标为同名函数。
     *
     * 空内容（属性未填写）不建引用；重载命中多个目标时由
     * [SpelReference] 的多目标解析提供选择。
     */
    private fun functionNameReferences(
        literal: PsiLiteralExpression,
        symbols: SpelContext
    ): Array<PsiReference> {
        val name = resolver.functionNameOf(literal) ?: return PsiReference.EMPTY_ARRAY
        if (name.isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }
        val range = TextRange(1, literal.textLength - 1)
        if (range.isEmpty) {
            return PsiReference.EMPTY_ARRAY
        }
        val navigation = SpelNavigation(literal.project, symbols, anchor = literal)
        return arrayOf(SpelReference(literal, range) { navigation.functionNames(name) })
    }

    /**
     * 参数注入变量名（`@Rar`/`@Var`/`@Bean`/`@Env`）：整段名字建一个引用。
     *
     * 目标按 [SpelInjection.scope] 分流：Root 变量、普通变量、Bean 类与环境
     * 配置 key。value 未书写时运行期回退参数名，源码中没有可跳转的文本，不建引用。
     */
    private fun injectionNameReferences(
        literal: PsiLiteralExpression,
        symbols: SpelContext,
        injection: SpelInjection
    ): Array<PsiReference> {
        if (!injection.explicitName) {
            return PsiReference.EMPTY_ARRAY
        }
        val range = injectionNameRange(literal)
        if (range.isEmpty) {
            return PsiReference.EMPTY_ARRAY
        }
        val name = injection.referenceName
        val navigation = SpelNavigation(literal.project, symbols, anchor = literal)
        return when (injection.scope) {
            SpelInjectionScope.ROOT_VAR ->
                arrayOf(SpelReference(literal, range) { navigation.rootVariables(name) })

            SpelInjectionScope.VAR ->
                arrayOf(SpelReference(literal, range) { navigation.variables(name) })

            SpelInjectionScope.BEAN ->
                arrayOf(SpelReference(literal, range) { navigation.beans(name) })

            SpelInjectionScope.ENV -> environmentKeyReferences(literal, name, range)

            SpelInjectionScope.EXPRESSION -> PsiReference.EMPTY_ARRAY
        }
    }

    /** `@Env` 的配置键引用：目标为配置文件中键名的声明位置。 */
    private fun environmentKeyReferences(
        literal: PsiLiteralExpression,
        name: String,
        range: TextRange
    ): Array<PsiReference> {
        val service = SpringEnvironmentService.getInstance(literal.project)
        if (!service.isEnabledFor(literal)) {
            return PsiReference.EMPTY_ARRAY
        }
        val model = service.modelFor(literal) ?: return PsiReference.EMPTY_ARRAY
        return SpringPlaceholderReferences.keyReferencesOf(
            literal,
            name.substringBefore(':'),
            range,
            model
        )
    }

    /** 注入名的源码范围：引号之间的整段文本；`@Env` 的 `key:default` 只覆盖 key。 */
    private fun injectionNameRange(literal: PsiLiteralExpression): TextRange {
        val text = literal.text
        val end = text.indexOf(':', 1).takeIf { it > 0 } ?: (text.length - 1)
        return if (end <= 1) TextRange.EMPTY_RANGE else TextRange(1, end)
    }

    companion object {

        private val CONTEXT_BUILDER = SpelContextBuilder()

        private val CONTEXT_KEY: Key<CachedValue<SpelContext>> =
            Key.create("lucky.spel.literal.context")

        /** 无状态可共享：所有构造参数都是纯解析器，缓存入口 [contextOf] 也与实例无关。 */
        private val SHARED = SpelReferenceProvider()

        /**
         * 直接收取本提供器为字面量生成的 SpEL 引用。
         *
         * 引用搜索中不能调用 `literal.references`：该入口会执行所有
         * contributor，其中 IntelliLang 针对 `@Language` 变量的拼接注入会在内部
         * 再次发起 `ReferencesSearch`，重入本插件的引用搜索器后无限递归
         * （StackOverflowError）。调用方只关心 SpEL 引用，直接走本提供器即可。
         */
        internal fun referencesOf(literal: PsiLiteralExpression): Array<PsiReference> =
            SHARED.getReferencesByElement(literal, ProcessingContext())

        /**
         * 按 Java 声明结构版本缓存上下文，避免高亮、悬停与引用重复构建符号层。
         * 方法体编辑不影响结果，不会让每个字面量的上下文重新构建。
         *
         * 入口必须与调用方实例无关：同一个字面量的 `CachedValue` 会被引用提供器、
         * 文档提供器等不同实例访问，provider lambda 捕获 `this` 时平台会判定
         * 「same CV with different captured context」并抛 `PluginException`。
         * 因此构建器放在伴生对象里共享，lambda 只捕获元素本身。
         */
        internal fun contextOf(literal: PsiLiteralExpression): SpelContext =
            CachedValuesManager.getCachedValue(literal, CONTEXT_KEY) {
                CachedValueProvider.Result.create(
                    CONTEXT_BUILDER.build(literal),
                    LuckyModificationTrackers.signature(literal.project)
                )
            }
    }
}
