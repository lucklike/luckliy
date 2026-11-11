package com.luckyframework.idea.spel.refactoring

import com.intellij.openapi.editor.Editor
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.MultiMap
import com.luckyframework.idea.spel.reference.SpelRenameDeclaration
import com.luckyframework.idea.spel.symbol.SpelImportParser
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import com.luckyframework.idea.spel.symbol.SpelSymbolOrigin

/**
 * SpEL 声明重命名入口（13.1/13.2/13.4）。
 *
 * `@SpELImport` 变量名与 `@FunctionAlias` 别名位于字符串内部，平台默认的
 * 字面量重命名会替换整个字符串；这里把光标所在的名字片段包装成
 * [SpelRenameDeclaration]，引用收集与写回交给统一的 SpEL 引用层。
 *
 * 冲突时在写入前抛出 [IncorrectOperationException] 中止，源文件保持不变。
 */
class SpelRenameProcessor(
    private val importParser: SpelImportParser = SpelImportParser()
) : RenamePsiElementProcessor() {

    override fun canProcessElement(element: PsiElement): Boolean = when (element) {
        is SpelRenameDeclaration -> true
        is PsiLiteralExpression -> targetAt(element) != null
        else -> false
    }

    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? {
        if (element is SpelRenameDeclaration) {
            return element
        }
        val literal = element as? PsiLiteralExpression ?: return element
        val target = targetAt(literal) ?: return element
        if (editor == null || !isSameDocument(editor, literal)) {
            return target
        }
        val offset = editor.caretModel.offset - literal.textRange.startOffset
        // 光标不在名字片段上时不接管，避免把整个注解值误当作声明
        return if (target.nameRange.containsOffset(offset)) target else null
    }

    /** 新名字与同作用域声明冲突时由平台中止重命名并展示原因。 */
    override fun findExistingNameConflicts(
        element: PsiElement,
        newName: String,
        conflicts: MultiMap<PsiElement, String>
    ) {
        if (element is SpelRenameDeclaration && element.hasConflict(newName)) {
            conflicts.putValue(element, "SpEL 名称 '$newName' 已存在")
        }
    }

    override fun findExistingNameConflicts(
        element: PsiElement,
        newName: String,
        conflicts: MultiMap<PsiElement, String>,
        allRenames: MutableMap<PsiElement, String>
    ) {
        findExistingNameConflicts(element, newName, conflicts)
    }

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<UsageInfo>,
        listener: RefactoringElementListener?
    ) {
        if (element is SpelRenameDeclaration && element.hasConflict(newName)) {
            throw IncorrectOperationException("SpEL 名称 '$newName' 已存在")
        }
        super.renameElement(element, newName, usages, listener)
    }

    private fun isSameDocument(editor: Editor, literal: PsiLiteralExpression): Boolean {
        val document = PsiDocumentManager.getInstance(literal.project).getDocument(literal.containingFile)
        return document != null && document == editor.document
    }

    private fun targetAt(literal: PsiLiteralExpression): SpelRenameDeclaration? {
        if (literal.value !is String) {
            return null
        }
        val annotation = PsiTreeUtil.getParentOfType(literal, PsiAnnotation::class.java, true) ?: return null
        return when (annotation.qualifiedName) {
            SpelImportParser.SPEL_IMPORT -> importVariableTarget(annotation, literal)
            FUNCTION_ALIAS -> aliasTarget(annotation, literal)
            else -> null
        }
    }

    private fun importVariableTarget(
        annotation: PsiAnnotation,
        literal: PsiLiteralExpression
    ): SpelRenameDeclaration? {
        val declaration = importParser.parse(annotation, SpelSymbolOrigin.METHOD_IMPORT)
            .firstOrNull { it.literal == literal && !it.name.isNullOrBlank() } ?: return null
        val nameRange = declaration.nameRange ?: return null
        val owner = modifierOwner(annotation)
        val kindLabel = if (declaration.kind == SpelSymbolKind.ROOT_VARIABLE) {
            "Root 变量 @SpELImport"
        } else {
            "@SpELImport 变量"
        }
        return SpelRenameDeclaration(
            literal = literal,
            nameRange = nameRange,
            currentName = declaration.name!!.trim(),
            kindLabel = kindLabel,
            targetElement = literal,
            conflictChecker = { newName -> hasImportConflict(owner, literal, newName) }
        )
    }

    private fun aliasTarget(
        annotation: PsiAnnotation,
        literal: PsiLiteralExpression
    ): SpelRenameDeclaration? {
        if (annotation.findAttributeValue("value") != literal) {
            return null
        }
        val alias = literal.value as? String ?: return null
        val method = PsiTreeUtil.getParentOfType(annotation, PsiMethod::class.java, true) ?: return null
        val containingClass = method.containingClass
        return SpelRenameDeclaration(
            literal = literal,
            nameRange = ElementManipulators.getValueTextRange(literal),
            currentName = alias.trim(),
            kindLabel = "函数",
            targetElement = method,
            conflictChecker = { newName ->
                containingClass?.methods.orEmpty().any { other ->
                    other != method && aliasOf(other) == newName
                }
            }
        )
    }

    private fun hasImportConflict(
        owner: PsiModifierListOwner?,
        literal: PsiLiteralExpression,
        newName: String
    ): Boolean {
        if (owner == null) {
            return false
        }
        return importParser.findImportAnnotations(owner)
            .flatMap { importParser.parse(it, SpelSymbolOrigin.METHOD_IMPORT) }
            .any { it.literal != literal && it.name?.trim() == newName }
    }

    private fun modifierOwner(annotation: PsiAnnotation): PsiModifierListOwner? {
        val owner = annotation.owner
        return when (owner) {
            is PsiModifierListOwner -> owner
            is PsiElement -> PsiTreeUtil.getParentOfType(owner, PsiModifierListOwner::class.java, false)
            else -> null
        }
    }

    private fun aliasOf(method: PsiMethod): String? =
        (method.getAnnotation(FUNCTION_ALIAS)?.findAttributeValue("value") as? PsiLiteralExpression)
            ?.value as? String

    companion object {
        private const val FUNCTION_ALIAS = "com.luckyframework.httpclient.proxy.spel.FunctionAlias"
    }
}
