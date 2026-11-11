package com.luckyframework.idea.spel.yaml

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML 侧 SpEL 补全入口（YAML 插件存在时加载）。
 */
class SpelYamlCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withLanguage(YAMLLanguage.INSTANCE)
                .inside(YAMLScalar::class.java),
            SpelYamlCompletionProvider()
        )
    }
}
