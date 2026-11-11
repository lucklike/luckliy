package com.luckyframework.idea.spel.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.lang.java.JavaLanguage
import com.intellij.patterns.PlatformPatterns

/**
 * SpEL 补全入口。
 *
 * `elementPattern` 只限定 Java 语言：字符串字面量内（含平台补全副本）
 * 父链可能变化，字面量判定与 `#{` / 激活状态由 [SpelCompletionProvider] 负责，
 * 其余位置在 provider 首行直接退出。
 */
class SpelCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(JavaLanguage.INSTANCE),
            SpelCompletionProvider()
        )
    }
}
