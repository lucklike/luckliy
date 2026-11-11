package com.luckyframework.idea.spel.highlight

import com.intellij.lang.annotation.HighlightSeverity
import com.luckyframework.idea.spel.LuckyFixtureTestCase

class SpelAnnotatorTest : LuckyFixtureTestCase() {

    fun testHighlightsDelimiters() {
        myFixture.configureByText("Demo.java", "class Demo { String s = \"a#{#x}b\"; }")
        val open = myFixture.file.text.indexOf("#{")
        val infos = myFixture.doHighlighting()

        assertTrue(infos.any { it.startOffset == open && it.endOffset == open + 2 })
        assertTrue(infos.any { it.startOffset == open + 4 && it.endOffset == open + 5 })
    }

    fun testHighlightsTokensByType() {
        myFixture.configureByText(
            "Demo.java",
            "class Demo { String s = \"#{#time() + #offset}\"; }"
        )
        val text = myFixture.file.text
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("#time"), SpelTextAttributes.FUNCTION)
        assertKeyAt(infos, text.indexOf("#offset"), SpelTextAttributes.VARIABLE)
        assertKeyAt(infos, text.indexOf("+"), SpelTextAttributes.OPERATOR)
    }

    /** `.` 之后的属性名单独着色，方法成员按函数色。 */
    fun testPropertyAndMemberFunctionHighlighting() {
        myFixture.configureByText(
            "Demo.java",
            "class Demo { String s = \"#{#offset + #user.name + #crypto.md5('a')}\"; }"
        )
        val text = myFixture.file.text
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("name"), SpelTextAttributes.PROPERTY)
        assertKeyAt(infos, text.indexOf("md5"), SpelTextAttributes.FUNCTION)
        assertKeyAt(infos, text.indexOf("#offset"), SpelTextAttributes.VARIABLE)
    }

    /** 函数调用的括号与数组下标按括号色着色。 */
    fun testParenthesesHighlighting() {
        myFixture.configureByText(
            "Demo.java",
            "class Demo { String s = \"#{#time() + #list[0]}\"; }"
        )
        val text = myFixture.file.text
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("time(") + 4, SpelTextAttributes.PARENTHESES)
        assertKeyAt(infos, text.indexOf(")"), SpelTextAttributes.PARENTHESES)
        assertKeyAt(infos, text.indexOf("["), SpelTextAttributes.PARENTHESES)
        assertKeyAt(infos, text.indexOf("]"), SpelTextAttributes.PARENTHESES)
    }

    /** `@SpELImport` 的 `name=value` 声明名按变量/ Root 变量色着色。 */
    fun testImportDeclarationNameHighlighting() {
        myFixture.configureByText(
            "Demo.java",
            """
class Demo {
    @com.luckyframework.httpclient.proxy.spel.SpELImport(
        var = "token=1", root = "user=admin"
    )
    void call() {
    }
}
"""
        )
        val text = myFixture.file.text
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("token"), SpelTextAttributes.VARIABLE)
        assertKeyAt(infos, text.indexOf("user"), SpelTextAttributes.ROOT_VARIABLE)
    }

    /** 裸方法名按函数色，`$body$` 响应变量按 Root 变量色。 */
    fun testBareFunctionAndResponseVariableHighlighting() {
        myFixture.configureByText(
            "Demo.java",
            "class Demo { String s = \"#{chunk_success(\$body\$)}\"; }"
        )
        val text = myFixture.file.text
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("chunk_success"), SpelTextAttributes.FUNCTION)
        assertKeyAt(infos, text.indexOf("\$body\$"), SpelTextAttributes.ROOT_VARIABLE)
    }

    fun testHighlightsStringAndNumber() {
        myFixture.configureByText("Demo.java", "class Demo { String s = \"#{'abc' + 1}\"; }")
        val text = myFixture.file.text
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("'abc'"), SpelTextAttributes.STRING)
        assertKeyAt(infos, text.indexOf("1}"), SpelTextAttributes.NUMBER)
    }

    fun testUnclosedExpressionIsError() {
        myFixture.configureByText("Demo.java", "class Demo { String s = \"#{foo\"; }")

        assertErrorContains("未闭合")
    }

    fun testUnclosedStringIsError() {
        myFixture.configureByText("Demo.java", "class Demo { String s = \"#{'abc}\"; }")

        assertErrorContains("字符串字面量未闭合")
    }

    fun testUnbalancedBracketsIsError() {
        myFixture.configureByText("Demo.java", "class Demo { String s = \"#{#fn(1, 2}\"; }")

        assertErrorContains("不配平")
    }

    fun testPlainStringsAreNotHighlighted() {
        myFixture.configureByText(
            "Demo.java",
            "class Demo { String a = \"hello world\"; String b = \"a=1&b=2\"; }"
        )

        val infos = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey?.externalName?.startsWith("LUCKY_SPEL_") == true }

        assertTrue(infos.isEmpty())
    }

    fun testFiveHundredFragmentsHighlightWithinOneSecond() {
        val builder = StringBuilder("class Demo { String s = \"")
        repeat(500) { builder.append("#{#v").append(it).append("}") }
        builder.append("\"; }")
        myFixture.configureByText("Demo.java", builder.toString())

        // 预热一次，排除索引构建与 JIT 首次开销，只计量 annotator 稳定态耗时
        myFixture.doHighlighting()
        val start = System.nanoTime()
        myFixture.doHighlighting()
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000

        assertTrue("首次高亮耗时 ${elapsedMillis}ms", elapsedMillis < 1000)
    }

    private fun assertKeyAt(
        infos: List<com.intellij.codeInsight.daemon.impl.HighlightInfo>,
        offset: Int,
        expected: com.intellij.openapi.editor.colors.TextAttributesKey
    ) {
        val keys = infos.filter { it.startOffset == offset }.map { it.forcedTextAttributesKey }
        assertTrue(
            "offset $offset 未找到 $expected 高亮: $keys",
            infos.any { it.startOffset == offset && it.forcedTextAttributesKey == expected }
        )
    }

    private fun assertErrorContains(message: String) {
        val errors = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.ERROR }

        assertTrue(
            "未找到包含「$message」的错误: ${errors.map { it.description }}",
            errors.any { it.description?.contains(message) == true }
        )
    }
}
