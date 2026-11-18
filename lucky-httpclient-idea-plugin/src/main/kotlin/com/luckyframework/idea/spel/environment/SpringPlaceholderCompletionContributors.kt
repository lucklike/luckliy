package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.lang.java.JavaLanguage
import com.intellij.patterns.PlatformPatterns
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLScalar

/** Java 字符串字面量中的 `${key}` 补全入口。 */
class SpringPlaceholderCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(JavaLanguage.INSTANCE),
            SpringPlaceholderCompletionProvider()
        )
    }
}

/** YAML 值中的 `${key}` 补全入口（YAML 插件存在时加载）。 */
class SpringPlaceholderYamlCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withLanguage(YAMLLanguage.INSTANCE)
                .inside(YAMLScalar::class.java),
            SpringPlaceholderYamlCompletionProvider()
        )
    }
}
