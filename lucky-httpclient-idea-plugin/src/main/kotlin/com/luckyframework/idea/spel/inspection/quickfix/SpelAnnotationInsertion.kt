package com.luckyframework.idea.spel.inspection.quickfix

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

/**
 * 快速修复共用的注解插入逻辑。
 *
 * 注解先按全限定名插入，再由 [JavaCodeStyleManager.shortenClassReferences]
 * 缩短并补 import，避免用户源码缺少 `@SpELImport` 导入。
 */
internal object SpelAnnotationInsertion {

    /**
     * 注解插入目标：默认取字面量最近的修饰符列表持有者（方法/字段/类），
     * [preferClass] 为 true 时强制落到包含类上。
     */
    fun owner(element: PsiElement, preferClass: Boolean): PsiModifierListOwner? {
        if (preferClass) {
            PsiTreeUtil.getParentOfType(element, PsiClass::class.java, true)?.let { return it }
        }
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiModifierListOwner && current.modifierList != null) {
                return current
            }
            current = current.parent
        }
        return null
    }

    fun add(owner: PsiModifierListOwner, annotationText: String): PsiAnnotation? {
        val modifierList = owner.modifierList ?: return null
        val project = owner.project
        val annotation = JavaPsiFacade.getElementFactory(project).createAnnotationFromText(annotationText, owner)
        val added = modifierList.addBefore(annotation, modifierList.firstChild) as? PsiAnnotation ?: return null
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(added)
        CodeStyleManager.getInstance(project).reformat(owner)
        return added
    }
}
