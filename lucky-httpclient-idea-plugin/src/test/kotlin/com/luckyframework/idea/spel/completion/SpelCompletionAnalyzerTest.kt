package com.luckyframework.idea.spel.completion

import junit.framework.TestCase

/**
 * 补全位置判定：关闭候选列表后继续输入（如 `#{md}` 中补 `5`）仍要识别为 Root 位置，
 * 而表达式闭合之后不再触发。
 */
class SpelCompletionAnalyzerTest : TestCase() {

    private val analyzer = SpelCompletionAnalyzer()

    fun testRootWordGrowsAfterLookupClosed() {
        // 光标在 `5` 之后、`}` 之前：`#{md}` 补成 `#{md5}` 后仍应给出 Root 候选
        val site = analyzer.analyze("\"#{md5}\"", 6)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.ROOT, site!!.mode)
        assertEquals("md5", site.word)
    }

    fun testRootWordBeforeClosingBrace() {
        // 光标在 `d` 之后、`}` 之前
        val site = analyzer.analyze("\"#{md}\"", 5)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.ROOT, site!!.mode)
        assertEquals("md", site.word)
    }

    fun testClosedExpressionHasNoCompletion() {
        // 光标在 `}` 之后：表达式已闭合，不再补全
        assertNull(analyzer.analyze("\"#{md}\"", 6))
    }

    fun testMemberAfterDot() {
        val site = analyzer.analyze("\"#{file.}\"", 8)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.MEMBER, site!!.mode)
        assertEquals("file", site.qualifier)
    }

    fun testVariableAfterHash() {
        val site = analyzer.analyze("\"#{#\"", 4)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.VARIABLE, site!!.mode)
    }

    /** 函数参数内的成员访问只取当前参数作为接收者。 */
    fun testMemberInsideArgumentUsesParameterReceiver() {
        val text = "\"#{add(request., 4)}\""
        val site = analyzer.analyze(text, text.indexOf('.') + 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.MEMBER, site!!.mode)
        assertEquals("request", site.qualifier)
    }

    /** 括号闭合后接收者恢复为整个调用表达式。 */
    fun testMemberAfterClosedCallUsesWholeExpression() {
        val text = "\"#{fn(1).}\""
        val site = analyzer.analyze(text, text.indexOf('.') + 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.MEMBER, site!!.mode)
        assertEquals("fn(1)", site.qualifier)
    }

    /** 调用实参含逗号时，接收者仍是整个调用表达式。 */
    fun testMemberAfterCallWithCommaArguments() {
        val text = "\"#{fn(1, 2).}\""
        val site = analyzer.analyze(text, text.indexOf('.') + 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.MEMBER, site!!.mode)
        assertEquals("fn(1, 2)", site.qualifier)
    }

    /** 安全导航 `?.` 的接收者是问号之前的表达式。 */
    fun testSafeNavigationUsesReceiverBeforeQuestionMark() {
        val text = "\"#{#user?.}\""
        val site = analyzer.analyze(text, text.indexOf("?.") + 2)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.MEMBER, site!!.mode)
        assertEquals("#user", site.qualifier)
    }

    /** `new` 之后是类型位置，即使光标前只有一个空格。 */
    fun testTypeAfterNew() {
        val text = "\"#{new SpelP\""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.TYPE, site!!.mode)
        assertEquals("SpelP", site.word)
        assertEquals("", site.typePrefix)
    }

    /** `new` 后输入包名时，已输入的前缀交给类型候选过滤。 */
    fun testTypeAfterNewWithPackagePrefix() {
        val text = "\"#{new java.util.Arr\""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.TYPE, site!!.mode)
        assertEquals("Arr", site.word)
        assertEquals("java.util.", site.typePrefix)
    }

    /** `instanceof T(` 之后是类型位置，插入时不再重复包 T(...)。 */
    fun testTypeAfterInstanceof() {
        val text = "\"#{#user instanceof T(SpelU\""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.TYPE, site!!.mode)
        assertEquals("SpelU", site.word)
        assertEquals("", site.typePrefix)
        assertFalse(site.wrapInTypeReference)
    }

    /** `instanceof` 后直接补类名时，候选需要自动包一层 `T(...)`。 */
    fun testTypeAfterInstanceofWrapsTypeReference() {
        val text = "\"#{#user instanceof SpelU\""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.TYPE, site!!.mode)
        assertEquals("SpelU", site.word)
        assertEquals("", site.typePrefix)
        assertTrue(site.wrapInTypeReference)
    }

    /** `T(` 内输入部分包名时仍是类型位置，而不是成员访问。 */
    fun testTypeInsideTCallWithPackagePrefix() {
        val text = "\"#{T(java.util.Arr\""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.TYPE, site!!.mode)
        assertEquals("Arr", site.word)
        assertEquals("java.util.", site.typePrefix)
    }

    /** 光标停在包名末尾的 `.` 后：类型前缀已完整，当前单词为空。 */
    fun testTypePrefixEndsWithDot() {
        val text = "\"#{T(java.\""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.TYPE, site!!.mode)
        assertEquals("", site.word)
        assertEquals("java.", site.typePrefix)
    }

    /** `@` 之后是 Bean 引用位置。 */
    fun testBeanAfterAt() {
        val text = "\"#{@use\""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.BEAN, site!!.mode)
        assertEquals("use", site.word)
    }

    /** 内联集合的 `{` 之后是 Root 位置。 */
    fun testRootAfterInlineBrace() {
        val text = "\"#{ {ke\""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.ROOT, site!!.mode)
        assertEquals("ke", site.word)
    }

    /** `between {` 的元素位置同样按 Root 处理。 */
    fun testRootAfterBetweenBrace() {
        val text = "\"#{1 between {ke\""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.ROOT, site!!.mode)
    }

    /** 文本运算符（`and`/`eq` 等）之后是 Root 位置。 */
    fun testRootAfterWordOperator() {
        val text = "\"#{keyword eq \""
        val site = analyzer.analyze(text, text.length - 1)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.ROOT, site!!.mode)
    }

    /** 标识符内部的 `eq` 结尾不能误判为文本运算符。 */
    fun testWordOperatorNeedsBoundary() {
        val text = "\"#{my_eq \""
        val site = analyzer.analyze(text, text.length - 1)
        assertNull(site)
    }

    /** `matches` 右侧的字符串字面量内不补全。 */
    fun testNoCompletionInsideMatchesString() {
        val text = "\"#{a matches 'x'\""
        assertNull(analyzer.analyze(text, text.indexOf('x')))
    }
}
