package com.luckyframework.idea.spel.yaml

import com.intellij.codeInsight.lookup.LookupElement
import com.luckyframework.idea.spel.LuckyFixtureTestCase

class SpelYamlCompletionTest : LuckyFixtureTestCase() {

    fun testImportVariableCompletes() {
        val elements = complete(
            """
            springElImport:
              varLit:
                token: "abc"
            url: "http://#{#to<caret>}/api"
            """.trimIndent()
        )

        assertTrue(
            "补全缺失 #token: ${elements.map { it.lookupString }}",
            elements.any { it.lookupString == "#token" }
        )
    }

    fun testBuiltinFunctionCompletes() {
        val elements = complete(
            """
            url: "http://#{#ti<caret>}/api"
            """.trimIndent()
        )

        assertTrue(elements.any { it.lookupString == "#time" })
    }

    fun testKebabCaseRootLitCompletes() {
        val elements = complete(
            """
            spring-el-import:
              root-lit:
                user: "admin"
            url: "#{us<caret>"
            """.trimIndent()
        )

        assertRootCompleted(elements, "user")
    }

    fun testCamelCaseRootLitCompletes() {
        val elements = complete(
            """
            springElImport:
              rootLit:
                user: "admin"
            url: "#{us<caret>"
            """.trimIndent()
        )

        assertRootCompleted(elements, "user")
    }

    /** 唯一候选时平台会自动插入，lookup 为空但文档已被改写为 `#{user`。 */
    private fun assertRootCompleted(elements: List<LookupElement>, name: String) {
        assertTrue(
            "Root 变量补全缺失: ${elements.map { it.lookupString }}",
            elements.any { it.lookupString == name } || myFixture.file.text.contains("#{$name")
        )
    }

    fun testOtherFileDoesNotLeakVariables() {
        myFixture.addFileToProject(
            "other.yml",
            """
            springElImport:
              varLit:
                token: "abc"
            """.trimIndent()
        )
        val elements = complete(
            """
            url: "http://#{#to<caret>}/api"
            """.trimIndent()
        )

        assertFalse(
            "不应出现其他文件的变量: ${elements.map { it.lookupString }}",
            elements.any { it.lookupString == "#token" }
        )
    }

    private fun complete(text: String): List<LookupElement> {
        myFixture.configureByText("config.yml", text)
        myFixture.completeBasic()
        return myFixture.lookupElements?.toList() ?: emptyList()
    }
}
