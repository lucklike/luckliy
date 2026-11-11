package com.luckyframework.idea.spel.symbol

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.PsiTreeUtil
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
     * 方法参数六种命名：`name`、`$name`、`$n`、`$$n`、`name::type`、`$n::type`。
     *
     * 与运行期一致，`n` 从 0 开始；`@Param`（`com.luckyframework.reflect.Param`）
     * 的 `value` 覆盖编译期参数名。
     */
    private fun parameterSymbols(method: PsiMethod): List<SpelSymbol> {
        val result = ArrayList<SpelSymbol>()
        method.parameterList.parameters.forEachIndexed { index, parameter ->
            val declaredName = parameter.name ?: return@forEachIndexed
            val effectiveName = paramNameOverride(parameter) ?: declaredName
            val type = parameter.type.presentableText
            // sourceRange 相对声明元素，供跳转精确定位到参数名
            val declarationRange = parameter.nameIdentifier?.textRange
                ?.shiftLeft(parameter.textRange.startOffset)
            val names = listOf(
                effectiveName,
                "\$$effectiveName",
                "\$$index",
                "\$\$$index",
                "$effectiveName::type",
                "\$$index::type"
            )
            for (name in names) {
                result += SpelSymbol(
                    name = name,
                    kind = SpelSymbolKind.VARIABLE,
                    type = type,
                    origin = SpelSymbolOrigin.PARAMETER,
                    declaration = parameter,
                    lifecycle = SpelLifecycle.REQUEST_INIT,
                    sourceRange = declarationRange
                )
            }
        }
        return result
    }

    private fun paramNameOverride(parameter: PsiParameter): String? {
        readAnnotationString(parameter.getAnnotation(REFLECT_PARAM), "value")?.let { return it }
        // @Param 支持标注在组合注解上
        for (annotation in parameter.modifierList?.annotations ?: return null) {
            val merged = annotation.resolveAnnotationType()?.getAnnotation(REFLECT_PARAM) ?: continue
            readAnnotationString(merged, "value")?.let { return it }
        }
        return null
    }

    private fun readAnnotationString(annotation: PsiAnnotation?, attribute: String): String? {
        if (annotation == null) {
            return null
        }
        val value = (annotation.findAttributeValue(attribute) as? PsiLiteralExpression)?.value as? String
        return value?.takeIf { it.isNotBlank() }
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
        private const val REFLECT_PARAM = "com.luckyframework.reflect.Param"
    }
}
