package com.luckyframework.idea.spel.environment

import com.intellij.openapi.project.DumbService
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.yaml.SpelYamlSupport
import org.jetbrains.yaml.psi.YAMLScalar

/** Java 字符串字面量中 `${key}` 的引用入口。 */
class SpringPlaceholderReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLiteralExpression::class.java),
            SpringPlaceholderReferenceProvider()
        )
    }
}

/** YAML 值中 `${key}` 的引用入口（YAML 插件存在时加载）。 */
class SpringPlaceholderYamlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            SpringPlaceholderYamlReferenceProvider()
        )
    }
}

class SpringPlaceholderYamlReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val scalar = element as? YAMLScalar ?: return PsiReference.EMPTY_ARRAY
        if (!SpelYamlSupport.isPlaceholderValue(scalar)) {
            return PsiReference.EMPTY_ARRAY
        }
        if (DumbService.isDumb(scalar.project)) {
            return PsiReference.EMPTY_ARRAY
        }
        val service = SpringEnvironmentService.getInstance(scalar.project)
        if (!service.isEnabledFor(scalar)) {
            return PsiReference.EMPTY_ARRAY
        }
        val model = service.modelFor(scalar) ?: return PsiReference.EMPTY_ARRAY
        return SpringPlaceholderReferences.referencesOf(scalar, scalar.text, model)
    }
}
