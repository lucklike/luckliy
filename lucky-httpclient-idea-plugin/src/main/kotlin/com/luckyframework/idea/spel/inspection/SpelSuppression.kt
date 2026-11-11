package com.luckyframework.idea.spel.inspection

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.luckyframework.idea.spel.symbol.annotationMemberElements

/**
 * 单条提示的抑制与识别（12.7）。
 *
 * 抑制标记使用 JDK 自带 `@SuppressWarnings("<inspection>")`，无需额外导入，
 * 检查在 `isSuppressedFor` 中识别标记后不再报告。
 */
object SpelSuppression {

    private const val SUPPRESS_WARNINGS = "java.lang.SuppressWarnings"

    /** 最近的可承载 `@SuppressWarnings` 的修饰符列表持有者。 */
    fun owner(element: PsiElement): PsiModifierListOwner? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiModifierListOwner && current.modifierList != null) {
                return current
            }
            current = current.parent
        }
        return null
    }

    /**
     * 元素自身或任一祖先是否被 [inspectionId] 抑制。
     */
    fun isSuppressed(element: PsiElement, inspectionId: String): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiModifierListOwner && suppressionIds(current).contains(inspectionId)) {
                return true
            }
            current = current.parent
        }
        return false
    }

    /**
     * 在 [owner] 上追加抑制标记；已存在时并入既有 `@SuppressWarnings`。
     */
    fun addSuppression(owner: PsiModifierListOwner, inspectionId: String) {
        val modifierList = owner.modifierList ?: return
        val project = owner.project
        val existing = modifierList.annotations.firstOrNull { isSuppressWarnings(it) }
        val factory = JavaPsiFacade.getElementFactory(project)
        val annotation = if (existing == null) {
            val created = factory.createAnnotationFromText("@$SUPPRESS_WARNINGS(\"$inspectionId\")", owner)
            modifierList.addBefore(created, modifierList.firstChild) as? PsiAnnotation ?: return
        } else {
            val values = suppressionIds(existing).toMutableList()
            if (!values.add(inspectionId)) {
                return
            }
            val text = values.joinToString(", ") { "\"$it\"" }
            existing.replace(factory.createAnnotationFromText("@$SUPPRESS_WARNINGS({$text})", owner)) as? PsiAnnotation
                ?: return
        }
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
        CodeStyleManager.getInstance(project).reformat(owner)
    }

    private fun suppressionIds(owner: PsiModifierListOwner): Set<String> {
        val annotation = owner.modifierList?.annotations?.firstOrNull { isSuppressWarnings(it) }
            ?: return emptySet()
        return suppressionIds(annotation)
    }

    private fun suppressionIds(annotation: PsiAnnotation): Set<String> =
        annotationMemberElements(annotation.findAttributeValue("value"))
            .mapNotNull { (it as? PsiLiteralExpression)?.value as? String }
            .toSet()

    private fun isSuppressWarnings(annotation: PsiAnnotation): Boolean =
        annotation.qualifiedName == SUPPRESS_WARNINGS ||
            annotation.nameReferenceElement?.referenceName == "SuppressWarnings"
}
