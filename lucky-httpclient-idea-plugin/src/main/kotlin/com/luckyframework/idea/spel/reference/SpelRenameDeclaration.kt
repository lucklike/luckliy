package com.luckyframework.idea.spel.reference

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import javax.swing.Icon

/**
 * 字符串字面量内的可重命名声明（13.1/13.2）。
 *
 * `@SpELImport(var = "token=...")` 的名字与 `@FunctionAlias("sha")` 的别名
 * 都不是独立的 PSI 元素；重命名时用本类包装它们，让平台的重命名流程
 * 能收集引用、预览并写回名字文本。
 *
 * [declarationToMatch] 返回的 [SpelDeclaration] 与解析阶段构造的目标等价，
 * 供 [SpelReference.isReferenceTo] 匹配引用。
 */
class SpelRenameDeclaration(
    private val literal: PsiLiteralExpression,
    val nameRange: TextRange,
    private val currentName: String,
    private val kindLabel: String,
    private val targetElement: PsiElement,
    private val conflictChecker: (String) -> Boolean = { false }
) : FakePsiElement(), PsiNamedElement, Navigatable {

    private val declarationProject: Project
        get() = literal.project

    override fun getParent(): PsiElement? = literal

    override fun isValid(): Boolean = literal.isValid

    override fun getName(): String = currentName

    override fun getText(): String = currentName

    override fun setName(name: String): PsiElement {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && trimmed != currentName) {
            ElementManipulators.handleContentChange(literal, nameRange, trimmed)
        }
        return this
    }

    /** 新名字是否与同作用域内的其他声明冲突。 */
    fun hasConflict(newName: String): Boolean = conflictChecker(newName.trim())

    /** 与跳转/查找用法阶段构造的声明保持一致的匹配目标。 */
    fun declarationToMatch(): SpelDeclaration = SpelDeclaration(
        project = declarationProject,
        name = currentName,
        kindLabel = kindLabel,
        target = targetElement,
        anchor = targetElement
    )

    override fun getProject(): Project = declarationProject

    override fun getContainingFile(): PsiFile? = literal.containingFile

    override fun getTextOffset(): Int = literal.textRange.startOffset + nameRange.startOffset

    override fun getUseScope(): SearchScope = GlobalSearchScope.projectScope(declarationProject)

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = literal.isValid

    override fun navigate(requestFocus: Boolean) {
        if (!literal.isValid) {
            return
        }
        val virtualFile = literal.containingFile?.virtualFile ?: return
        OpenFileDescriptor(declarationProject, virtualFile, textOffset).navigate(requestFocus)
    }

    override fun getPresentation(): ItemPresentation = this

    override fun getPresentableText(): String = currentName

    override fun getLocationString(): String = kindLabel

    override fun getIcon(unused: Boolean): Icon? = null

    override fun toString(): String = "$kindLabel $currentName"
}
