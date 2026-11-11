package com.luckyframework.idea.spel.symbol

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierListOwner
import com.luckyframework.idea.spel.lexer.JavaStringEscapeMapper
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * `@SpELImport` 声明解析中的问题，供格式检查复用。
 */
enum class SpelImportProblem {
    MISSING_SEPARATOR,
    EMPTY_NAME
}

/**
 * 一条 `@SpELImport` 变量声明。
 *
 * [name] 为 `null` 表示声明无法静态确定（缺少分隔符）；
 * [nameRange]/[valueRange] 相对 [literal] 元素本身，便于跳转与检查定位。
 */
data class SpelImportDeclaration(
    val name: String?,
    val value: String,
    val kind: SpelSymbolKind,
    val origin: SpelSymbolOrigin,
    val literal: PsiLiteralExpression,
    val nameRange: TextRange? = null,
    val valueRange: TextRange? = null,
    val valueIsSpel: Boolean = true,
    val problem: SpelImportProblem? = null
)

/**
 * 解析 `@SpELImport`（含 `@SpELImports` 容器、重复注解与元注解上的声明）。
 *
 * 与运行期 `Context.analyticExpression` 对齐：
 * 在第一个分隔符处拆分，名称与值都 trim；`root`/`var` 的值按 SpEL 解析，
 * `rootLit`/`varLit` 按字面量处理。
 *
 * 已知偏差：运行期把 `varLit` 错误地加入 Root 变量（design 已记录），
 * 插件按公开语义把它当作普通变量。
 */
class SpelImportParser {

    /**
     * 收集修饰符列表上（含元注解）的所有 `@SpELImport` 使用位置。
     */
    fun findImportAnnotations(owner: PsiModifierListOwner): List<PsiAnnotation> {
        val result = LinkedHashSet<PsiAnnotation>()
        val visited = HashSet<String>()
        val annotations = owner.modifierList?.annotations ?: return emptyList()
        for (annotation in annotations) {
            collect(annotation, result, visited, 0)
        }
        return result.toList()
    }

    /**
     * 解析一个 `@SpELImport` 上的全部变量声明。
     */
    fun parse(annotation: PsiAnnotation, origin: SpelSymbolOrigin): List<SpelImportDeclaration> {
        if (annotation.qualifiedName != SPEL_IMPORT) {
            return emptyList()
        }
        val separator = (annotation.findAttributeValue("separator") as? PsiLiteralExpression)
            ?.value as? String ?: DEFAULT_SEPARATOR
        val declarations = ArrayList<SpelImportDeclaration>()
        parseAttribute(annotation, "root", SpelSymbolKind.ROOT_VARIABLE, origin, separator, true, declarations)
        parseAttribute(annotation, "rootLit", SpelSymbolKind.ROOT_VARIABLE, origin, separator, false, declarations)
        parseAttribute(annotation, "var", SpelSymbolKind.VARIABLE, origin, separator, true, declarations)
        parseAttribute(annotation, "varLit", SpelSymbolKind.VARIABLE, origin, separator, false, declarations)
        return declarations
    }

    /**
     * 解析 `pack` 导入的包名。
     */
    fun parsePackages(annotation: PsiAnnotation): List<String> =
        annotationMemberElements(annotation.findAttributeValue("pack")).mapNotNull { element ->
            (element as? PsiLiteralExpression)?.value as? String
        }.filter { it.isNotBlank() }

    /**
     * 解析 `value` 导入的类。
     */
    fun parseClasses(annotation: PsiAnnotation): List<PsiClass> =
        resolveClassValues(annotation.findAttributeValue("value"))

    /**
     * 解析 `ifs` 条件导入的类（条件本身不参与静态判断）。
     */
    fun parseConditionalClasses(annotation: PsiAnnotation): List<PsiClass> {
        val result = ArrayList<PsiClass>()
        for (element in annotationMemberElements(annotation.findAttributeValue("ifs"))) {
            val ifAnnotation = element as? PsiAnnotation ?: continue
            result += resolveClassValues(ifAnnotation.findAttributeValue("classes"))
        }
        return result
    }

    /**
     * 把修饰符列表上的 `@SpELImport` 声明转换为符号。
     */
    fun symbols(owner: PsiModifierListOwner, origin: SpelSymbolOrigin): List<SpelSymbol> =
        findImportAnnotations(owner)
            .flatMap { parse(it, origin) }
            .mapNotNull { declaration ->
                val name = declaration.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SpelSymbol(
                    name = name,
                    kind = declaration.kind,
                    origin = origin,
                    declaration = declaration.literal,
                    lifecycle = SpelLifecycle.REQUEST_INIT,
                    sourceRange = declaration.nameRange
                )
            }

    private fun collect(
        annotation: PsiAnnotation,
        result: MutableSet<PsiAnnotation>,
        visited: MutableSet<String>,
        depth: Int
    ) {
        if (depth > MAX_DEPTH) {
            return
        }
        when (annotation.qualifiedName) {
            SPEL_IMPORT -> result += annotation
            SPEL_IMPORTS -> expandContainer(annotation, result)
        }
        val declaration = annotation.resolveAnnotationType() ?: return
        val declarationName = declaration.qualifiedName ?: return
        if (!visited.add(declarationName)) {
            return
        }
        for (meta in declaration.modifierList?.annotations ?: return) {
            collect(meta, result, visited, depth + 1)
        }
    }

    private fun expandContainer(container: PsiAnnotation, result: MutableSet<PsiAnnotation>) {
        for (element in elementsOf(container.findAttributeValue("value"))) {
            if (element is PsiAnnotation && element.qualifiedName == SPEL_IMPORT) {
                result += element
            }
        }
    }

    private fun parseAttribute(
        annotation: PsiAnnotation,
        attribute: String,
        kind: SpelSymbolKind,
        origin: SpelSymbolOrigin,
        separator: String,
        valueIsSpel: Boolean,
        declarations: MutableList<SpelImportDeclaration>
    ) {
        for (element in elementsOf(annotation.findAttributeValue(attribute))) {
            if (element is PsiLiteralExpression) {
                declarations += parseLiteral(element, kind, origin, separator, valueIsSpel)
            }
        }
    }

    private fun parseLiteral(
        literal: PsiLiteralExpression,
        kind: SpelSymbolKind,
        origin: SpelSymbolOrigin,
        separator: String,
        valueIsSpel: Boolean
    ): SpelImportDeclaration {
        val mapped = JavaStringEscapeMapper.map(literal.text)
        val raw = mapped.value
        if (separator.isEmpty()) {
            return invalid(literal, kind, origin, valueIsSpel, SpelImportProblem.MISSING_SEPARATOR)
        }
        val separatorIndex = raw.indexOf(separator)
        if (separatorIndex < 0) {
            return invalid(literal, kind, origin, valueIsSpel, SpelImportProblem.MISSING_SEPARATOR)
        }

        val rawName = raw.substring(0, separatorIndex)
        val valueOffset = separatorIndex + separator.length
        val rawValue = raw.substring(valueOffset)

        val nameStart = firstNonWhitespace(rawName)
        val nameEnd = lastNonWhitespace(rawName) + 1
        val valueStart = firstNonWhitespace(rawValue)
        val valueEnd = lastNonWhitespace(rawValue) + 1

        val name = rawName.trim()
        val value = rawValue.trim()
        val problem = if (name.isEmpty()) SpelImportProblem.EMPTY_NAME else null
        return SpelImportDeclaration(
            name = name,
            value = value,
            kind = kind,
            origin = origin,
            literal = literal,
            nameRange = rangeOf(mapped, nameStart, nameEnd),
            valueRange = rangeOf(mapped, valueOffset + valueStart, valueOffset + valueEnd),
            valueIsSpel = valueIsSpel,
            problem = problem
        )
    }

    private fun invalid(
        literal: PsiLiteralExpression,
        kind: SpelSymbolKind,
        origin: SpelSymbolOrigin,
        valueIsSpel: Boolean,
        problem: SpelImportProblem
    ): SpelImportDeclaration = SpelImportDeclaration(
        name = null,
        value = "",
        kind = kind,
        origin = origin,
        literal = literal,
        valueIsSpel = valueIsSpel,
        problem = problem
    )

    /** 第一处非空白字符的值偏移；全空白时返回 0。 */
    private fun firstNonWhitespace(text: String): Int =
        text.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }

    /** 最后一处非空白字符之后的值偏移；空串或全空白时返回 [firstNonWhitespace]。 */
    private fun lastNonWhitespace(text: String): Int =
        text.indexOfLast { !it.isWhitespace() }.let { if (it < 0) firstNonWhitespace(text) - 1 else it }

    private fun rangeOf(mapped: JavaStringEscapeMapper.Mapped, start: Int, end: Int): TextRange =
        TextRange(mapped.sourceOffset(start), mapped.sourceOffset(end))

    private fun elementsOf(value: PsiAnnotationMemberValue?): List<PsiAnnotationMemberValue> =
        annotationMemberElements(value)

    companion object {
        const val SPEL_IMPORT = "com.luckyframework.httpclient.proxy.spel.SpELImport"
        const val SPEL_IMPORTS = "com.luckyframework.httpclient.proxy.spel.SpELImports"
        private const val DEFAULT_SEPARATOR = "="
        private const val MAX_DEPTH = 16
    }
}

/** 把注解成员值展开为元素列表（字面量或数组初始化器）。 */
internal fun annotationMemberElements(value: PsiAnnotationMemberValue?): List<PsiAnnotationMemberValue> = when (value) {
    null -> emptyList()
    is PsiArrayInitializerMemberValue -> value.initializers.toList()
    else -> listOf(value)
}

/** 从注解成员值中解析 Class 字面量。 */
internal fun resolveClassValues(value: PsiAnnotationMemberValue?): List<PsiClass> =
    annotationMemberElements(value).mapNotNull { element ->
        val access = element as? PsiClassObjectAccessExpression ?: return@mapNotNull null
        (access.operand.type as? PsiClassType)?.resolve()
    }
