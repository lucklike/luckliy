package com.luckyframework.idea.spel.yaml

import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.completion.SpelCompletionAnalyzer
import org.jetbrains.yaml.psi.YAMLScalar

class YamlContextDebugTest : LuckyFixtureTestCase() {

    fun testDebugRootSymbols() {
        myFixture.configureByText(
            "config.yml",
            """
springElImport:
  rootLit:
    user: "admin"
url: "#{us<caret>"
""".trimIndent()
        )
        val scalar = PsiTreeUtil.findChildrenOfType(myFixture.file, YAMLScalar::class.java)
            .first { it.text.contains("#{") }
        val context = SpelYamlContextBuilder().build(scalar)
        println("DEBUG_SYMBOLS=" + context.symbols.joinToString { "${it.name}/${it.kind}/${it.origin}/${it.visibility}" })
        println("DEBUG_AVAILABLE=" + context.available().joinToString { it.name })

        println("DEBUG_SCALAR_TEXT=[${scalar.text}]")
        println("DEBUG_SITE=" + SpelCompletionAnalyzer().analyze(scalar.text, scalar.text.length))

        myFixture.completeBasic()
        println("DEBUG_LOOKUP_NULL=" + (myFixture.lookup == null))
        println("DEBUG_FILE_TEXT=" + myFixture.file.text.replace("\n", "|"))
        println("DEBUG_LOOKUP=" + (myFixture.lookupElements?.toList()?.map { it.lookupString } ?: emptyList<String>()))
    }
}
