package com.luckyframework.idea.spel.yaml

import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

class SpelYamlSupportTest : LuckyFixtureTestCase() {

    fun testQuotedValueIsRecognized() {
        myFixture.configureByText(
            "config.yml",
            """
            url: "http://#{#host}/api"
            """.trimIndent()
        )

        val scalars = SpelYamlSupport.expressionScalars(myFixture.file)
        assertEquals(1, scalars.size)
        assertEquals("\"http://#{#host}/api\"", scalars.single().text)
    }

    fun testPlainValueIsRecognized() {
        myFixture.configureByText(
            "config.yml",
            """
            url: http://#{#host}/api
            """.trimIndent()
        )

        assertEquals(1, SpelYamlSupport.expressionScalars(myFixture.file).size)
    }

    fun testCommentIsIgnored() {
        myFixture.configureByText("config.yml", "# 示例：#{#host}\n")

        assertTrue(SpelYamlSupport.expressionScalars(myFixture.file).isEmpty())
    }

    fun testKeyNameIsIgnored() {
        myFixture.configureByText(
            "config.yml",
            """
            "#{key}": value
            """.trimIndent()
        )

        assertTrue(SpelYamlSupport.expressionScalars(myFixture.file).isEmpty())
    }

    fun testSequenceValueIsRecognized() {
        myFixture.configureByText(
            "config.yml",
            """
            running:
              - "#{#id + 1}"
            """.trimIndent()
        )

        assertEquals(1, SpelYamlSupport.expressionScalars(myFixture.file).size)
    }

    fun testNonStringScalarIsNotString() {
        myFixture.configureByText(
            "config.yml",
            """
            count: 3
            enabled: true
            missing: null
            """.trimIndent()
        )

        val scalars = PsiTreeUtil.findChildrenOfType(myFixture.file, YAMLScalar::class.java)
        assertTrue(scalars.isNotEmpty())
        assertTrue(scalars.none { SpelYamlSupport.isStringValue(it) })
    }

    fun testImportVariablesAreNestedKeys() {
        myFixture.configureByText(
            "config.yml",
            """
            springElImport:
              varLit:
                token: "abc"
              rootLit:
                user: "admin"
              val:
                "#{'dynamic'}": "x"
            """.trimIndent()
        )

        val symbols = SpelYamlImportParser().symbols(myFixture.file).associateBy { it.name }

        assertEquals(setOf("token", "user"), symbols.keys)
        assertTrue(symbols.getValue("token").isRoot.not())
        assertTrue(symbols.getValue("user").isRoot)
        assertEquals("token", (symbols.getValue("token").declaration as YAMLKeyValue).keyText)
    }
}
