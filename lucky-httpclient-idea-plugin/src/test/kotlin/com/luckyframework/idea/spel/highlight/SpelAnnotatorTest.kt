package com.luckyframework.idea.spel.highlight

import com.intellij.lang.annotation.HighlightSeverity
import com.luckyframework.idea.spel.LuckyFixtureTestCase

/**
 * 着色与语法校验只针对显式标注 `@SpelAttribute` 的注解属性：
 *
 * - SpEL 表达式必须写在 `@HttpRequest(url = ...)` 这类属性中；
 * - 普通字符串字面量、未标注属性不进入词法分析，不产生任何着色或错误。
 */
class SpelAnnotatorTest : LuckyFixtureTestCase() {

    fun testHighlightsDelimiters() {
        val text = configureSpel("a#{#x}b")
        val open = text.indexOf("#{")
        val infos = myFixture.doHighlighting()

        assertTrue(infos.any { it.startOffset == open && it.endOffset == open + 2 })
        assertTrue(infos.any { it.startOffset == open + 4 && it.endOffset == open + 5 })
    }

    fun testHighlightsTokensByType() {
        val text = configureSpel("#{#time() + #offset}")
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("#time"), SpelTextAttributes.FUNCTION)
        assertKeyAt(infos, text.indexOf("#offset"), SpelTextAttributes.VARIABLE)
        assertKeyAt(infos, text.indexOf("+"), SpelTextAttributes.OPERATOR)
    }

    /** `.` 之后的属性名单独着色，方法成员按函数色。 */
    fun testPropertyAndMemberFunctionHighlighting() {
        val text = configureSpel("#{#offset + #user.name + #crypto.md5('a')}")
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("name"), SpelTextAttributes.PROPERTY)
        assertKeyAt(infos, text.indexOf("md5"), SpelTextAttributes.FUNCTION)
        assertKeyAt(infos, text.indexOf("#offset"), SpelTextAttributes.VARIABLE)
    }

    /** 函数调用的括号与数组下标按括号色着色。 */
    fun testParenthesesHighlighting() {
        val text = configureSpel("#{#time() + #list[0]}")
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("time(") + 4, SpelTextAttributes.PARENTHESES)
        assertKeyAt(infos, text.indexOf(")"), SpelTextAttributes.PARENTHESES)
        assertKeyAt(infos, text.indexOf("["), SpelTextAttributes.PARENTHESES)
        assertKeyAt(infos, text.indexOf("]"), SpelTextAttributes.PARENTHESES)
    }

    /** 选择方括号的 `[` 与配对的 `]` 必须同色，且使用独立的过滤标记色。 */
    fun testSelectionBracketsShareSameHighlighting() {
        val text = configureSpel("#{#users.?[age > 1]}")
        val infos = myFixture.doHighlighting()

        // SELECTION token 覆盖 `?[`，RBRACKET token 是单独的 `]`
        assertKeyAt(infos, text.indexOf("?["), SpelTextAttributes.SELECTION)
        assertKeyAt(infos, text.indexOf("]"), SpelTextAttributes.SELECTION)
    }

    /** 投影与嵌套选择各自保持 `[`/`]` 同色，过滤与映射使用不同颜色。 */
    fun testProjectionAndNestedSelectionBracketsShareHighlighting() {
        val text = configureSpel("#{#groups.![members.?[age > 1]]}")
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("!["), SpelTextAttributes.PROJECTION)
        assertKeyAt(infos, text.indexOf("?["), SpelTextAttributes.SELECTION)
        // 内层选择先闭合（SELECTION），外层投影后闭合（PROJECTION）
        assertKeyAt(infos, text.indexOf("]]"), SpelTextAttributes.SELECTION)
        assertKeyAt(infos, text.lastIndexOf("]"), SpelTextAttributes.PROJECTION)
    }

    /** 普通下标访问保持括号色，不受选择/投影着色影响。 */
    fun testIndexerBracketsStayParentheses() {
        val text = configureSpel("#{#list[0] + #users.?[age > 1]}")
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("[0]"), SpelTextAttributes.PARENTHESES)
        assertKeyAt(infos, text.indexOf("]"), SpelTextAttributes.PARENTHESES)
        assertKeyAt(infos, text.indexOf("?["), SpelTextAttributes.SELECTION)
        assertKeyAt(infos, text.lastIndexOf("]"), SpelTextAttributes.SELECTION)
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
        val text = configureSpel("#{chunk_success(\$body\$)}")
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("chunk_success"), SpelTextAttributes.FUNCTION)
        assertKeyAt(infos, text.indexOf("\$body\$"), SpelTextAttributes.ROOT_VARIABLE)
    }

    fun testHighlightsStringAndNumber() {
        val text = configureSpel("#{'abc' + 1}")
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("'abc'"), SpelTextAttributes.STRING)
        assertKeyAt(infos, text.indexOf("1}"), SpelTextAttributes.NUMBER)
    }

    /**
     * 片段内部字符串字面量里的 `#{` 只是普通文本：
     * `#{'#{Hello World}'}` 只求值一次，内层不能当作子片段报语法错误。
     */
    fun testFragmentInsideStringLiteralIsNotError() {
        configureSpel("#{'#{Hello World}'}")

        val errors = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.ERROR }
        assertTrue(
            "不应报语法错误: ${errors.map { it.description }}",
            errors.isEmpty()
        )
    }

    /** 双引号字符串字面量内出现 `#{` 同样不产生子片段。 */
    fun testDoubleQuotedFragmentInsideStringLiteralIsNotError() {
        // Java 字符串源文中双引号必须转义，这里传入的 `\"` 会原样写进注解
        configureSpel("#{T(String).format(\\\"#{x}\\\")}")

        val errors = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.ERROR }
        assertTrue(
            "不应报语法错误: ${errors.map { it.description }}",
            errors.isEmpty()
        )
    }

    fun testUnclosedExpressionIsError() {
        configureSpel("#{foo")

        assertErrorContains("未闭合")
    }

    fun testUnclosedStringIsError() {
        configureSpel("#{'abc}")

        assertErrorContains("字符串字面量未闭合")
    }

    fun testUnbalancedBracketsIsError() {
        configureSpel("#{#fn(1, 2}")

        assertErrorContains("不配平")
    }

    /** 多出来的右括号（如 `add(1))`）必须报错，而不是被静默忽略。 */
    fun testExtraClosingParenthesisIsError() {
        configureSpel("#{#fn(1))}")

        assertErrorContains("多余的 ')'")
    }

    fun testMismatchedBracketTypeIsError() {
        configureSpel("#{#fn([1)}")

        assertErrorContains("不配对")
    }

    fun testEmptyArgumentIsError() {
        configureSpel("#{#fn(1,)}")

        assertErrorContains("最后一个参数不能为空")
    }

    /** `.` 后直接跟逗号（如 `add(request., 4)`）属于不完整的成员访问。 */
    fun testDotWithoutMemberIsError() {
        configureSpel("#{#fn(request., 4)}")

        assertErrorContains("后缺少属性或方法名")
    }

    /** 安全导航 `?.` 后同样必须跟成员名。 */
    fun testSafeNavigationWithoutMemberIsError() {
        configureSpel("#{#user?.}")

        assertErrorContains("后缺少属性或方法名")
    }

    /** 官方语法只允许一个表达式：函数调用后的顶层 `, p` 属于错误。 */
    fun testSecondTopLevelExpressionIsError() {
        configureSpel("#{#fn(\$req\$.getBaseUrl(), 4), p}")

        assertErrorContains("只能包含一个表达式")
    }

    /** 三目运算符缺少 `:` 必须报错。 */
    fun testTernaryWithoutColonIsError() {
        configureSpel("#{#a ? 1}")

        assertErrorContains("三目运算符缺少 ':'")
    }

    /** 运算符右侧缺少操作数必须报错。 */
    fun testMissingOperandIsError() {
        configureSpel("#{#fn(1 +)}")

        assertErrorContains("缺少操作数")
    }

    /** 索引访问缺少下标表达式必须报错。 */
    fun testEmptyIndexerIsError() {
        configureSpel("#{#list[]}")

        assertErrorContains("索引访问缺少下标表达式")
    }

    /** 普通字符串（非注解属性）不是 SpEL 属性，含 `#{` 也不产生任何着色。 */
    fun testPlainStringsAreNotHighlighted() {
        myFixture.configureByText(
            "Demo.java",
            "class Demo { String a = \"hello world\"; String b = \"a=1&b=2\"; String c = \"#{#x}\"; }"
        )

        val infos = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey?.externalName?.startsWith("LUCKY_SPEL_") == true }

        assertTrue(infos.map { "${it.startOffset}-${it.endOffset}" }.toString(), infos.isEmpty())
    }

    fun testFiveHundredFragmentsHighlightWithinOneSecond() {
        val builder =
            StringBuilder("class Demo { @com.luckyframework.httpclient.proxy.annotations.HttpRequest(url = \"")
        repeat(500) { builder.append("#{#v").append(it).append("}") }
        builder.append("\") void call() { } }")
        myFixture.configureByText("Demo.java", builder.toString())

        // 预热一次，排除索引构建与 JIT 首次开销，只计量 annotator 稳定态耗时
        myFixture.doHighlighting()
        val start = System.nanoTime()
        myFixture.doHighlighting()
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000

        assertTrue("首次高亮耗时 ${elapsedMillis}ms", elapsedMillis < 1000)
    }

    /**
     * 把表达式写进 `@HttpRequest(url = ...)` 注解属性。
     *
     * 只有显式标注 `@SpelAttribute` 的属性才参与着色与语法校验，
     * 普通字符串字面量不属于任何 SpEL 属性，因此不能直接写在字段初始化表达式里。
     */
    private fun configureSpel(expression: String): String {
        myFixture.configureByText(
            "Demo.java",
            """
class Demo {
    @com.luckyframework.httpclient.proxy.annotations.HttpRequest(url = "$expression")
    void call() {
    }
}
"""
        )
        return myFixture.file.text
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
