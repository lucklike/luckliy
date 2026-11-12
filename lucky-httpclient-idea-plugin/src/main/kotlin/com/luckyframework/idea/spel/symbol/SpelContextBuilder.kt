package com.luckyframework.idea.spel.symbol

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.metadata.ParamNameResolver
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 从字符串字面量推导 SpEL 上下文（design D5）。
 *
 * 符号层按优先级叠加，同名符号由更近的来源胜出：
 * 方法参数 > 方法级 `@SpELImport` > 类级 `@SpELImport` > 内置符号 > 全局变量。
 *
 * 内置与全局来源通过 [SpelSymbolSource] 注入：内置符号来自
 * [BuiltinSymbolsLoader] 生成的表；全局变量只纳入可静态解析的配置部分。
 */
class SpelContextBuilder(
    private val resolver: SpelAnnotationResolver = SpelAnnotationResolver(),
    private val paramNameResolver: ParamNameResolver = ParamNameResolver(),
    private val importParser: SpelImportParser = SpelImportParser(),
    private val importedClasses: ImportedClassSymbols = ImportedClassSymbols(),
    private val builtinSource: SpelSymbolSource = BuiltinSymbolsLoader.source(),
    private val globalSource: SpelSymbolSource = EmptySpelSymbolSource
) {

    fun build(literal: PsiLiteralExpression): SpelContext = build(literal, false)

    /**
     * 构建字面量的符号上下文。
     *
     * [lenient] 为 true 时，即使字面量不在已知注解属性中也照常返回符号层
     * （补全用；`known` 保持 false，阶段不参与过滤）。
     */
    fun build(literal: PsiLiteralExpression, lenient: Boolean): SpelContext {
        val attribute = resolver.resolveLiteral(literal)
        if (attribute == null && !lenient) {
            return SpelContext.unknown()
        }
        val stage = attribute?.lifecycle ?: SpelLifecycle.UNKNOWN
        val method = PsiTreeUtil.getParentOfType(literal, PsiMethod::class.java, true)
        val containingClass = PsiTreeUtil.getParentOfType(literal, PsiClass::class.java, true)

        val parameters = method?.let { parameterSymbols(it) } ?: emptyList()
        val methodLayer = method?.let { ownerLayer(it, SpelSymbolOrigin.METHOD_IMPORT) } ?: emptyList()
        val classLayer = containingClass?.let { classLayer(it) } ?: emptyList()
        val builtin = builtinSource.symbols().map {
            it.copy(lifecycle = LifecycleTable.availableFrom(it.name))
        }
        val global = globalSource.symbols()

        val merged = merge(parameters, methodLayer, classLayer, builtin, global)
        return SpelContext(
            known = attribute != null,
            stage = stage,
            attribute = attribute,
            symbols = applyVisibility(merged, stage)
        )
    }

    private fun ownerLayer(owner: PsiModifierListOwner, origin: SpelSymbolOrigin): List<SpelSymbol> =
        importParser.symbols(owner, origin) + importedClasses.collect(owner, origin)

    /**
     * 类层符号：类上的 `@SpELImport` 声明，加上当前类（含父类、父接口）
     * 声明的 public static 方法。
     *
     * 后者对应运行期 `ClassContext.loadClassSpELFun`：它通过
     * `ClassStaticElement` 把继承体系里所有 public static 方法注册成
     * SpEL 函数，因此 `#{chunk_success(...)}` 这类调用可以平铺使用。
     * `allMethods` 已包含父类与父接口的方法，且统一按当前类的
     * `@Namespace` 归组，与 `ClassStaticElement.create(clazz)` 一致。
     */
    private fun classLayer(psiClass: PsiClass): List<SpelSymbol> =
        classHierarchy(psiClass).flatMap { ownerLayer(it, SpelSymbolOrigin.CLASS_IMPORT) } +
            importedClasses.symbolsOf(psiClass, SpelSymbolOrigin.CLASS_STATIC)

    /**
     * 方法参数八种命名：`name`、`$name`、`$n`、`$$n`、`name::type`、`$n::type`
     * 以及参数上下文对象 `$pc$n`、`$pc$name`。
     *
     * 与运行期 `ParameterContext.initContext` 一致，`n` 从 0 开始。
     * 参数名不是直接取方法签名里的名字：运行期由
     * `ParameterUtils.getParamName` 取**合并后的** `@Param#value`，
     * 它可能来自参数上的组合注解经 `@AliasFor` 链传递的值
     * （例如 `@Query("req")` 注入的是 `req`）。具体语义见 [ParamNameResolver]。
     * 描述按运行期语义区分最终值/原始值/类型/上下文。
     */
    private fun parameterSymbols(method: PsiMethod): List<SpelSymbol> {
        val result = ArrayList<SpelSymbol>()
        method.parameterList.parameters.forEachIndexed { index, parameter ->
            val declaredName = parameter.name ?: return@forEachIndexed
            val effectiveName = paramNameResolver.resolve(parameter) ?: declaredName
            val type = parameter.type.presentableText
            // sourceRange 相对声明元素，供跳转精确定位到参数名
            val declarationRange = parameter.nameIdentifier?.textRange
                ?.shiftLeft(parameter.textRange.startOffset)
            val names = listOf(
                effectiveName to "参数 ${effectiveName} 的最终值",
                "\$$effectiveName" to "参数 ${effectiveName} 的原始值",
                "\$$index" to "第 $index 个参数的最终值",
                "\$\$$index" to "第 $index 个参数的原始值",
                "$effectiveName::type" to "参数 ${effectiveName} 的类型",
                "\$$index::type" to "第 $index 个参数的类型",
                "$PARAM_CONTEXT_PREFIX$index" to "第 $index 个参数的参数上下文",
                "$PARAM_CONTEXT_PREFIX$effectiveName" to "参数 ${effectiveName} 的参数上下文"
            )
            for ((name, description) in names) {
                result += SpelSymbol(
                    name = name,
                    kind = SpelSymbolKind.VARIABLE,
                    // `::type` 的值是参数的类型对象（ResolvableType），不是参数值类型；
                    // `$pc$n`/`$pc$name` 的值是 `ParameterContext` 本身
                    type = when {
                        name.endsWith(TYPE_SUFFIX) -> TYPE_INFO_TYPE
                        name.startsWith(PARAM_CONTEXT_PREFIX) -> PARAM_CONTEXT_TYPE
                        else -> type
                    },
                    origin = SpelSymbolOrigin.PARAMETER,
                    declaration = parameter,
                    lifecycle = SpelLifecycle.REQUEST_INIT,
                    sourceRange = declarationRange,
                    description = description
                )
            }
        }
        return result
    }

    private fun classHierarchy(psiClass: PsiClass): List<PsiClass> {
        val result = ArrayList<PsiClass>()
        val visited = HashSet<String>()
        fun visit(current: PsiClass?) {
            if (current == null) {
                return
            }
            val key = current.qualifiedName ?: current.name ?: return
            if (!visited.add(key)) {
                return
            }
            result += current
            visit(current.superClass)
            current.interfaces.forEach { visit(it) }
        }
        visit(psiClass)
        return result
    }

    /**
     * 按「就近优先」合并：同名同类同命名空间只保留第一个。
     *
     * 函数按签名区分，保证同名重载全部保留（spec 要求重载补全全部列出）。
     */
    private fun merge(vararg layers: List<SpelSymbol>): List<SpelSymbol> {
        val seen = HashSet<SymbolKey>()
        val result = ArrayList<SpelSymbol>()
        for (layer in layers) {
            for (symbol in layer) {
                val signature = if (symbol.kind == SpelSymbolKind.FUNCTION) symbol.signature else null
                if (seen.add(SymbolKey(symbol.kind, symbol.namespace, symbol.name, signature))) {
                    result += symbol
                }
            }
        }
        return result
    }

    private fun applyVisibility(symbols: List<SpelSymbol>, stage: SpelLifecycle): List<SpelSymbol> {
        if (!LifecycleTable.isStageFilterable(stage)) {
            return symbols
        }
        return symbols.map { symbol ->
            if (LifecycleTable.isAvailable(stage, symbol.lifecycle)) {
                symbol
            } else {
                symbol.copy(visibility = SpelVisibility.STAGE_UNAVAILABLE)
            }
        }
    }

    private data class SymbolKey(
        val kind: SpelSymbolKind,
        val namespace: String?,
        val name: String,
        val signature: String?
    )

    companion object {
        /** 参数类型引用的后缀；`name::type`/`$n::type` 的值来自 `ParameterContext.getType()`。 */
        private const val TYPE_SUFFIX = "::type"
        private const val TYPE_INFO_TYPE = "ResolvableType"

        /** 参数上下文对象的前缀：`InternalRootVarName.$_PARAM_CONTEXT_$`。 */
        private const val PARAM_CONTEXT_PREFIX = "\$pc\$"
        private const val PARAM_CONTEXT_TYPE = "ParameterContext"
    }
}
