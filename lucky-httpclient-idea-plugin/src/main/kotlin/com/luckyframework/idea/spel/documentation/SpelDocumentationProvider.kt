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
import com.luckyframework.idea.spel.lexer.SpelElementScopes
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.reference.SpelDeclaration
import com.luckyframework.idea.spel.reference.SpelNavigation
import com.luckyframework.idea.spel.reference.SpelReferenceCollector
import com.luckyframework.idea.spel.reference.SpelReferenceProvider

/**
 * 悬停文档（11.1–11.4）。
 *
 * `getCustomDocumentationElement` 把光标处的 SpEL 引用解析为 [SpelDeclaration]，
 * 未解析时创建带「未解析」说明的虚拟声明，保证悬停不出现空白文档；
 * `generateDoc` 只处理虚拟声明，普通 Java 元素仍交给 Java 自带文档。
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
            val html = SpelDocumentationRenderer.render(documentationOf(it))
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
            SpelDocumentationRenderer.render(documentationOf(declaration)),
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
        return declaration
    }

    /**
     * 补全文档预览的第一步：平台按 `lookupElement.getObject()` 询问文档元素。
     * 函数/方法候选携带虚拟声明，原样返回即可由 [SpelPsiDocumentationTargetProvider]
     * 产出默认样式的文档，不依赖 `CompletionUtil.getTargetElement` 的物理元素回退。
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
        return declaration
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
        if (!literal.text.contains("#{")) {
            return null
        }
        if (DumbService.isDumb(literal.project) || !LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)) {
            return null
        }
        val offsetInLiteral = offset - literal.textRange.startOffset
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
                    SpelTypeResolver(literal.project, SpelReferenceProvider().contextOf(literal))
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
            SpelReferenceProvider().contextOf(literal),
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
        return SpelDeclaration(
            project = element.project,
            name = name,
            kindLabel = "未解析",
            documentation = SpelDocumentation.unresolved(name),
            anchor = element
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
