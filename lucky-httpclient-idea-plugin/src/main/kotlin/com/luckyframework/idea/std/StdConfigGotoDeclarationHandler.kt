package com.luckyframework.idea.std

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.PsiTreeUtil

/**
 * `@StdHttpClient` 上 `configId` 字面量的 Ctrl+Click / Ctrl+B 跳转。
 *
 * 只处理直接标注在接口/类上的注解：注解声明处的默认值共享给所有使用者，
 * 无法对应到唯一接口，不建立跳转；`path`/`beanId` 等其他属性也不处理。
 */
class StdConfigGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        val literal = sourceElement as? PsiLiteralExpression
            ?: PsiTreeUtil.getParentOfType(sourceElement, PsiLiteralExpression::class.java)
            ?: return null
        val pair = PsiTreeUtil.getParentOfType(literal, PsiNameValuePair::class.java) ?: return null
        val annotation = PsiTreeUtil.getParentOfType(pair, PsiAnnotation::class.java) ?: return null
        val modifierList = annotation.owner as? PsiModifierList ?: return null
        val ownerClass = modifierList.parent as? PsiClass ?: return null
        if (ownerClass.isAnnotationType) {
            return null
        }
        if (!StdAnnotationValues.carriesStdHttpClient(annotation)) {
            return null
        }
        if (!StdAnnotationValues.isConfigIdAttribute(annotation, pair.name ?: "value")) {
            return null
        }
        val entry = StdHttpClientIndex.getInstance(ownerClass.project).entryOf(ownerClass) ?: return null
        val targets = StdConfigNavigation.configTargets(entry)
        return targets.takeIf { it.isNotEmpty() }?.toTypedArray()
    }
}
