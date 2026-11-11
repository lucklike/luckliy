package com.luckyframework.idea.spel.yaml

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * 在 YAML 标量上注册 SpEL 引用提供者（YAML 插件存在时加载）。
 */
class SpelYamlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            SpelYamlReferenceProvider()
        )
    }
}
