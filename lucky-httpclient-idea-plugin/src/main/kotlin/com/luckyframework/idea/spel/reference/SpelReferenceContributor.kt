package com.luckyframework.idea.spel.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

/**
 * 注册 SpEL 引用提供者。
 */
class SpelReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLiteralExpression::class.java),
            SpelReferenceProvider()
        )
    }
}
