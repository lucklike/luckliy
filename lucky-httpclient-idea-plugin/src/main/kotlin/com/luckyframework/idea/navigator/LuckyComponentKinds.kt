package com.luckyframework.idea.navigator

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import com.luckyframework.idea.std.StdHttpClientIndex

/**
 * Lucky 组件身份的分类规则。
 *
 * 侧边栏分组与编辑器 gutter 调试图标共用同一份判断：只要规则一致，
 * 同一份代码在导航树与编辑器里看到的能力就不会分叉。
 */
internal object LuckyComponentKinds {

    /** `@LuckyComponent#type` → 分组；`AUTO` 按结构推导。 */
    fun of(type: String, psiClass: PsiClass): LuckyGroupKind = when (type) {
        "API" -> LuckyGroupKind.API
        "DATABASE" -> LuckyGroupKind.DATABASE
        "FUNCTION" -> LuckyGroupKind.FUNCTION
        "CONFIG" -> LuckyGroupKind.CONFIG
        "SUPPORT" -> LuckyGroupKind.SUPPORT
        else -> infer(psiClass)
    }

    /**
     * 方法是否可由调试面板调用。
     *
     * 与侧边栏方法行的调试图标同源：`@StdHttpClient` 接口，以及除函数组之外的
     * `@LuckyComponent` 组件，其非静态方法都经代理执行。注解声明本身不是
     * 可调用对象，不参与判断。
     */
    fun isDebugApiMethod(project: Project, method: PsiMethod): Boolean {
        if (method.isConstructor || method.hasModifierProperty(PsiModifier.STATIC)) {
            return false
        }
        val psiClass = method.containingClass ?: return false
        if (psiClass.isAnnotationType) {
            return false
        }
        if (StdHttpClientIndex.getInstance(project).entryOf(psiClass) != null) {
            return true
        }
        val component = SpelRuntimeSymbols.components(project)
            .firstOrNull { it.className == psiClass.qualifiedName }
            ?: return false
        return of(component.type, psiClass) != LuckyGroupKind.FUNCTION
    }

    private fun infer(psiClass: PsiClass): LuckyGroupKind = when {
        psiClass.methods.any { !it.isConstructor && LuckyHttpAnnotations.httpMethodOf(it) != null } ->
            LuckyGroupKind.API

        psiClass.methods.any { it.isPublicStaticFunction() } -> LuckyGroupKind.FUNCTION
        else -> LuckyGroupKind.OTHER
    }

    private fun PsiMethod.isPublicStaticFunction(): Boolean =
        !isConstructor &&
            hasModifierProperty(PsiModifier.PUBLIC) &&
            hasModifierProperty(PsiModifier.STATIC)
}
