package com.luckyframework.idea.spel.documentation

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.completion.SpelElementContext
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.inspection.SpelUsage
import com.luckyframework.idea.spel.inspection.SpelUsageScanner
import com.luckyframework.idea.spel.lexer.SpelElementScopes
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelInjectionScope
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.reference.SpelDeclaration
import com.luckyframework.idea.spel.reference.SpelNavigation
import com.luckyframework.idea.spel.reference.SpelReferenceCollector
import com.luckyframework.idea.spel.reference.SpelReferenceProvider
import com.luckyframework.idea.spel.symbol.SpelSymbolKind

/**
 * 悬停文档（11.1–11.4）。
 *
 * `getCustomDocumentationElement` 把光标处的 SpEL 引用解析为 [SpelDeclaration]，
 * 未解析时创建带「未解析」说明的虚拟声明，保证悬停不出现空白文档；
 * 真实 PSI 声明直接返回声明元素，由平台 Java 文档链生成与 IDEA 内置
 * 完全一致的内容；`generateDoc` 只处理没有真实声明的虚拟符号。
 *
 * 2026.3 起平台的悬停与 Quick Doc 改为优先询问
 * `com.intellij.platform.backend.documentation.targetProvider`，
 * 只有所有目标提供器都返回空才回退到本类的旧路径，因此另由
 * [SpelDocumentationTargetProvider] 直接产出文档目标。
 */
class SpelDocumentationProvider : DocumentationProvider {

    private val log = Logger.getInstance("com.luckyframework.idea.spel.doc")

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? =
        declarationOf(element, originalElement)?.let {
            val html = SpelDocumentationRenderer.render(documentationOf(it), it.target)
            log.debug("[SpelDoc] generateDoc decl=${it.name} len=${html.length}")
            html
        }

    /** 新版平台从 [DocumentationProvider.DocumentationParts] 取文档，第二段为可空的定义详情。 */
    override fun getDocumentationParts(
        element: PsiElement,
        originalElement: PsiElement?
    ): DocumentationProvider.DocumentationParts? {
        val declaration = declarationOf(element, originalElement) ?: return null
        // 旧版 API 只提供单段 HTML，这里把定义与正文放进第一段保持内容不变
        return DocumentationProvider.DocumentationParts(
            SpelDocumentationRenderer.render(documentationOf(declaration), declaration.target),
            null
        )
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        val declaration = SpelDocumentationResolver.declarationAt(file, targetOffset, contextElement)
        log.debug("[SpelDoc] customElement file=${file.name} offset=$targetOffset hit=${declaration != null}")
        if (declaration == null) {
            return null
        }
        // 真实声明交给平台 Java 文档链（注解/签名/Javadoc/Returns/See Also/版本来源），
        // 与编辑器内置 Quick Doc 完全一致；虚拟声明才由本类兜底渲染。
        return SpelNativeDocumentation.nativeElementOf(declaration) ?: declaration
    }

    /**
     * 补全文档预览的第一步：平台按 `lookupElement.getObject()` 询问文档元素。
     * 真实声明直接返回 PSI，让平台按 Java 文档链渲染；虚拟声明原样返回，
     * 由 [SpelPsiDocumentationTargetProvider] 产出兜底文档。
     */
    override fun getDocumentationElementForLookupItem(
        psiManager: PsiManager,
        item: Any?,
        element: PsiElement?
    ): PsiElement? {
        val declaration = item as? SpelDeclaration
        log.debug(
            "[SpelDoc] lookupItem item=${item?.javaClass?.name} hit=${declaration != null} " +
                "element=${element?.javaClass?.name}"
        )
        if (declaration == null) {
            return null
        }
        return SpelNativeDocumentation.nativeElementOf(declaration) ?: declaration
    }

    private fun declarationOf(element: PsiElement?, originalElement: PsiElement?): SpelDeclaration? =
        (element as? SpelDeclaration) ?: (originalElement as? SpelDeclaration)

    private fun documentationOf(declaration: SpelDeclaration): SpelDocumentation =
        declaration.documentation ?: SpelDocumentationResolver.fallback(declaration)
}

/**
 * 光标位置 → SpEL 虚拟声明的共享解析逻辑。
 *
 * 旧版 `DocumentationProvider` 与新版的 [com.intellij.platform.backend.documentation.DocumentationTarget]
 * 都需要同一套定位规则，集中在此避免两处行为漂移。
 */
internal object SpelDocumentationResolver {

    fun declarationAt(file: PsiFile, offset: Int, contextElement: PsiElement?): SpelDeclaration? {
        val literal = literalAt(contextElement, file, offset) ?: return null
        val hasFragment = literal.text.contains("#{")
        // 函数名属性与参数注入名字属性（@Rar/@Var/@Bean/@Env）整段不含 `#{}`，
        // 但同样参与解析：悬停要展示函数/变量/Bean/环境键的声明
        val resolver = SpelAnnotationResolver()
        if (!hasFragment && !resolver.isFunctionNameLiteral(literal) &&
            !resolver.isInjectionNameAttribute(literal)
        ) {
            return null
        }
        if (DumbService.isDumb(literal.project) || !LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)) {
            return null
        }
        // 未标注 @SpelAttribute 的属性（以及 LITERAL/NONE/UNKNOWN）运行期不求值，
        // 不建引用也不解析，悬停与 Quick Doc 不应展示任何 SpEL 文档。
        if (!SpelReferenceProvider.contextOf(literal).known) {
            return null
        }
        val offsetInLiteral = offset - literal.textRange.startOffset
        if (!hasFragment) {
            val reference = literal.references.firstOrNull { containsOffset(it, offsetInLiteral) }
                ?: return null
            return resolveDeclaration(reference)
                ?: environmentKeyDeclaration(literal, resolver)
                ?: unresolvedDeclaration(reference)
        }
        // `#this`/`#root` 不在符号表中，也没有引用覆盖，需按 token 位置直接命中
        builtinObjectDeclarationAt(literal, offsetInLiteral)?.let { return it }
        val reference = literal.references.firstOrNull { containsOffset(it, offsetInLiteral) }
        if (reference != null) {
            return resolveDeclaration(reference)
                ?: typeReferenceDeclaration(literal, offsetInLiteral)
                ?: unresolvedDeclaration(reference)
        }
        // `T(...)` 的引用只覆盖括号内的类型名，悬停在 `T` 或括号上时没有引用命中；
        // `new` 后的裸类型名与 `instanceof` 的 `T(...)` 同样需要补一次类型解析。
        return typeReferenceDeclaration(literal, offsetInLiteral)
    }

    /**
     * `@Env("key:default")` 的键未在配置中找到时的悬停声明。
     *
     * 环境配置键不是 SpEL 符号，不能给出「未解析的 SpEL 符号」提示；
     * 带默认值时键缺失是正常用法，悬停应说明运行期会回退到默认值。
     */
    private fun environmentKeyDeclaration(
        literal: PsiLiteralExpression,
        resolver: SpelAnnotationResolver
    ): SpelDeclaration? {
        val injection = resolver.resolveInjection(literal) ?: return null
        if (injection.scope != SpelInjectionScope.ENV || !injection.explicitName) {
            return null
        }
        val name = injection.referenceName
        val separator = name.indexOf(':')
        val key = if (separator < 0) name else name.substring(0, separator)
        val defaultText = if (separator < 0) null else name.substring(separator + 1).trim()
        return SpelDeclaration(
            project = literal.project,
            name = key,
            kindLabel = "环境配置键",
            documentation = SpelDocumentation.environmentKeyNotInConfig(key, defaultText),
            anchor = literal
        )
    }

    /**
     * `#this`/`#root` 的悬停声明。
     *
     * 二者是 SpEL 运行期内置对象，不在变量表/命名空间表中，引用收集
     * 也不会为它们创建引用，因此按 token 位置直接命中：
     * 选择/投影方括号内的 `#this` 是当前元素，按元素类型展示；
     * 其余位置的 `#this`/`#root` 都是表达式根对象。
     */
    private fun builtinObjectDeclarationAt(
        literal: PsiLiteralExpression,
        offsetInLiteral: Int
    ): SpelDeclaration? {
        for (fragment in SpelScanner().scanLiteral(literal.text)) {
            val tokens = SpelTokenizer().tokenize(literal.text, fragment)
            val token = tokens.firstOrNull { candidate ->
                candidate.type in BUILTIN_OBJECT_TOKENS &&
                    candidate.text in BUILTIN_OBJECTS &&
                    offsetInLiteral >= candidate.range.startOffset &&
                    offsetInLiteral <= candidate.range.endOffset
            } ?: continue
            val scopes = SpelElementScopes.enclosing(
                literal.text,
                fragment.expressionRange.startOffset,
                token.range.startOffset
            )
            val elementType = if (scopes.isEmpty()) {
                null
            } else {
                SpelElementContext.elementType(
                    scopes,
                    SpelTypeResolver(literal.project, SpelReferenceProvider.contextOf(literal))
                )
            }
            return builtinObjectDeclaration(literal, token.text, elementType)
        }
        return null
    }

    private fun builtinObjectDeclaration(
        literal: PsiLiteralExpression,
        name: String,
        elementType: PsiType?
    ): SpelDeclaration {
        val typeLabel = elementType?.presentableText ?: "Object"
        val description = when {
            name == "#this" && elementType != null -> "选择/投影方括号中的当前元素"
            name == "#this" -> "SpEL 表达式的当前对象"
            else -> "SpEL 表达式的根对象"
        }
        return SpelDeclaration(
            project = literal.project,
            name = name,
            kindLabel = "内置变量",
            typeLabel = typeLabel,
            description = description,
            anchor = literal,
            documentation = SpelDocumentation(
                name = name,
                category = "内置变量",
                type = typeLabel,
                scope = "所有 SpEL 表达式",
                description = description
            )
        )
    }

    /**
     * 光标位于 `T(...)`（含 `instanceof T(...)`）或 `new` 的类型名时按类型解析：
     * 成功展示类型的签名与注释，失败给出类型专属的未解析提示。
     */
    private fun typeReferenceDeclaration(literal: PsiLiteralExpression, offsetInLiteral: Int): SpelDeclaration? {
        val typeName = typeReferenceAt(literal, offsetInLiteral) ?: return null
        val navigation = SpelNavigation(
            literal.project,
            SpelReferenceProvider.contextOf(literal),
            anchor = literal
        )
        return navigation.types(typeName).firstOrNull() ?: unresolvedTypeDeclaration(literal, typeName)
    }

    /** 覆盖 [offsetInLiteral] 的类型引用；不在类型引用内时返回 null。 */
    private fun typeReferenceAt(literal: PsiLiteralExpression, offsetInLiteral: Int): String? {
        val text = literal.text
        for (fragment in SpelScanner().scanLiteral(text)) {
            val tokens = SpelTokenizer().tokenize(text, fragment)
            for (index in tokens.indices) {
                val typeReference = SpelReferenceCollector.typeReferenceAt(tokens, text, index) ?: continue
                if (typeReference.hitRange.containsOffset(offsetInLiteral)) {
                    return typeReference.name
                }
            }
        }
        return null
    }

    private fun unresolvedTypeDeclaration(literal: PsiLiteralExpression, typeName: String): SpelDeclaration =
        SpelDeclaration(
            project = literal.project,
            name = typeName,
            kindLabel = "未解析类型",
            typeLabel = typeName,
            documentation = SpelDocumentation.unresolvedType(typeName),
            anchor = literal
        )

    fun literalAt(contextElement: PsiElement?, file: PsiFile, offset: Int): PsiLiteralExpression? {
        val fromContext = contextElement?.let {
            PsiTreeUtil.getParentOfType(it, PsiLiteralExpression::class.java, false)
        }
        if (fromContext != null && fromContext.textRange.contains(offset)) {
            return fromContext
        }
        val element = file.findElementAt(offset) ?: return null
        return PsiTreeUtil.getParentOfType(element, PsiLiteralExpression::class.java, false)
    }

    fun containsOffset(reference: PsiReference, offset: Int): Boolean {
        val range = reference.rangeInElement
        return offset >= range.startOffset && offset <= range.endOffset
    }

    /** 多目标（同名函数重载/别名与内置同名）时 `resolve()` 为 null，回退到首个多目标结果。 */
    fun resolveDeclaration(reference: PsiReference): SpelDeclaration? {
        (reference.resolve() as? SpelDeclaration)?.let { return it }
        val multi = reference as? PsiPolyVariantReference ?: return null
        return multi.multiResolve(false).firstOrNull()?.element as? SpelDeclaration
    }

    fun unresolvedDeclaration(reference: PsiReference): SpelDeclaration {
        val element = reference.element
        val name = reference.rangeInElement.substring(element.text)
        val reason = unresolvedReason(element, reference)
        return SpelDeclaration(
            project = element.project,
            name = name,
            kindLabel = reason?.kindLabel ?: "未解析",
            documentation = reason?.documentation ?: SpelDocumentation.unresolved(name),
            anchor = element
        )
    }

    /**
     * 悬停的引用未解析时，先看是否为可解释的未知：
     * 符号存在但书写形式不对（`#param` 引用 Root 变量、裸名称引用普通变量、
     * `#fn(...)` 调用内置函数或用命名空间函数），或来自 `@If(condition = "#{false}")` 的
     * 条件导入（类未被导入）。两种情况都展示准确原因，而不是「未解析」。
     */
    private fun unresolvedReason(element: PsiElement, reference: PsiReference): UnresolvedReason? {
        val literal = element as? PsiLiteralExpression ?: return null
        val context = SpelReferenceProvider.contextOf(literal)
        val usage = SpelUsageScanner().scan(literal, context)
            .firstOrNull { it.range == reference.rangeInElement } ?: return null
        if (usage.alternativeSymbols.isNotEmpty()) {
            return UnresolvedReason("引用形式错误", formError(usage))
        }
        val kind = symbolKindOf(usage.kind) ?: return null
        if (context.disabled(usage.symbolName, kind) == null) {
            return null
        }
        return UnresolvedReason("条件导入未生效", disabledFormError(usage))
    }

    private fun disabledFormError(usage: SpelUsage): SpelDocumentation {
        val label = labelOf(usage)
        return SpelDocumentation(
            name = usage.name,
            category = "条件导入未生效",
            scope = "当前表达式",
            unresolved = true,
            unresolvedTitle = "条件导入未生效：$label '${usage.name}' 来自条件导入（@If），" +
                "condition 求值为 false，类未被导入",
            unresolvedHint = "该${label}运行期不存在，请删除引用或调整 @If 的 condition"
        )
    }

    private fun symbolKindOf(type: SpelTokenType): SpelSymbolKind? = when (type) {
        SpelTokenType.VARIABLE -> SpelSymbolKind.VARIABLE
        SpelTokenType.ROOT_VARIABLE -> SpelSymbolKind.ROOT_VARIABLE
        SpelTokenType.FUNCTION -> SpelSymbolKind.FUNCTION
        SpelTokenType.NAMESPACE -> SpelSymbolKind.NAMESPACE
        else -> null
    }

    private fun labelOf(usage: SpelUsage): String = when (usage.kind) {
        SpelTokenType.FUNCTION -> "函数"
        SpelTokenType.NAMESPACE -> "命名空间"
        else -> "符号"
    }

    private data class UnresolvedReason(val kindLabel: String, val documentation: SpelDocumentation)

    private fun formError(usage: SpelUsage): SpelDocumentation {
        val name = usage.symbolName
        val namespace = usage.alternativeNamespace
        val title = when {
            usage.kind == SpelTokenType.FUNCTION && namespace != null ->
                "函数 '$name' 属于命名空间 '#$namespace'，只能以 #$namespace.$name(...) 形式调用"

            usage.kind == SpelTokenType.FUNCTION ->
                "内置函数 '$name' 只能以 $name(...) 形式调用，不支持 #$name(...)"

            usage.name.startsWith("#") -> "Root 变量 '$name' 只能用裸名称引用，请写成 $name"
            else -> "变量 '$name' 必须使用 # 引用，请写成 #$name"
        }
        return SpelDocumentation(
            name = usage.name,
            category = "引用形式错误",
            scope = "当前表达式",
            unresolved = true,
            unresolvedTitle = "引用形式错误：$title",
            unresolvedHint = "符号存在，只需调整书写形式"
        )
    }

    fun fallback(declaration: SpelDeclaration): SpelDocumentation = SpelDocumentation(
        name = declaration.presentation.presentableText ?: declaration.kindLabel,
        category = declaration.kindLabel
    )

    /** SpEL 运行期内置对象，不出现在变量表/命名空间表中。 */
    private val BUILTIN_OBJECTS = setOf("#this", "#root")

    /** 内置对象在词法层的 token 类型：独立出现是变量，后跟 `.` 时是命名空间。 */
    private val BUILTIN_OBJECT_TOKENS = setOf(SpelTokenType.VARIABLE, SpelTokenType.NAMESPACE)
}
