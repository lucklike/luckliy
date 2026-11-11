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
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.reference.SpelDeclaration

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
        val reference = literal.references.firstOrNull { containsOffset(it, offsetInLiteral) } ?: return null
        return resolveDeclaration(reference) ?: unresolvedDeclaration(reference)
    }

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
}
