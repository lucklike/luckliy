package com.luckyframework.idea.spel.lexer

import junit.framework.TestCase

/**
 * 接收者回溯与实参切分：补全、引用收集与调用检查共用的文本规则。
 */
class SpelExpressionTextTest : TestCase() {

    fun testSimpleReceiver() {
        assertEquals("#user", receiverOf("#user"))
        assertEquals("user.name", receiverOf("user.name"))
    }

    /** 闭合的调用表达式整体作为接收者，实参中的逗号不影响回溯。 */
    fun testClosedCallWithCommaArguments() {
        assertEquals("fn(1, 2)", receiverOf("fn(1, 2)"))
        assertEquals("fn(a, fn(b, c))", receiverOf("fn(a, fn(b, c))"))
    }

    /** 括号未闭合时只看当前参数，避免把半截表达式当作接收者。 */
    fun testUnclosedCallUsesCurrentArgument() {
        assertEquals("request", receiverOf("add(request"))
    }

    fun testOperatorBoundaries() {
        assertEquals("#user", receiverOf("1 + #user"))
        assertEquals("c", receiverOf("a ? b : c"))
    }

    /** 文本运算符与字符运算符一样切分接收者，右侧子表达式才是接收者。 */
    fun testTextOperatorBoundaries() {
        assertEquals("{10, 20, 50}", receiverOf("page > 0 and {10, 20, 50}"))
        assertEquals("list", receiverOf("a eq 1 and list"))
        assertEquals("name", receiverOf("name matches 'x' or name"))
        assertEquals("x", receiverOf("not x"))
        assertEquals("y", receiverOf("x instanceOf Integer and y"))
        // 文本运算符与 Spring 解析器一致大小写不敏感
        assertEquals("Integer", receiverOf("x instanceOf Integer"))
        // 变量/成员名中的关键字不当作运算符
        assertEquals("and.name", receiverOf("and.name"))
        assertEquals("#and.name", receiverOf("#and.name"))
        assertEquals("obj.and.name", receiverOf("obj.and.name"))
    }

    /** `?.` 与 `::type` 属于接收者，不作为顶层边界。 */
    fun testSafeNavigationAndTypeSuffixStayInReceiver() {
        assertEquals("#user?.address", receiverOf("#user?.address"))
        assertEquals("\${'$'}0::type", receiverOf("\${'$'}0::type"))
    }

    /** 选择/投影标记（含安全导航）整体属于接收者，不把 `[...]` 单独当接收者。 */
    fun testSelectionAndProjectionStayInReceiver() {
        assertEquals("members.?[age > 1]", receiverOf("members.?[age > 1]"))
        assertEquals("members.^[age > 1]", receiverOf("members.^[age > 1]"))
        assertEquals("members.\$[age > 1]", receiverOf("members.\$[age > 1]"))
        assertEquals("members.![name]", receiverOf("members.![name]"))
        assertEquals("members?.?[age > 1]", receiverOf("members?.?[age > 1]"))
        assertEquals("members?.![name]", receiverOf("members?.![name]"))
    }

    /** 方括号内的运算符与嵌套下标不影响接收者回溯。 */
    fun testSelectionConditionIsNotReceiverBoundary() {
        assertEquals("members.?[a[0] > 1]", receiverOf("1 + members.?[a[0] > 1]"))
        assertEquals("members.?[name == 'x']", receiverOf("1 + members.?[name == 'x']"))
        assertEquals("#list.?[fn(1, 2)]", receiverOf("1 + #list.?[fn(1, 2)]"))
    }

    /** 响应变量的下标访问是普通下标，整个表达式仍作为接收者。 */
    fun testResponseVariableIndexIsOrdinarySubscript() {
        assertEquals("\${'$'}body\${'$'}[0]", receiverOf("\${'$'}body\${'$'}[0]"))
    }
    fun testSplitArgumentsRespectsNesting() {
        assertEquals(
            listOf("'a,b'", "fn(1, 2)", "{1, 2}"),
            SpelExpressionText.splitArguments("'a,b', fn(1, 2), {1, 2}")
        )
        assertEquals(
            listOf("Map<String,Integer>"),
            SpelExpressionText.splitArguments("Map<String,Integer>", generics = true)
        )
    }

    private fun receiverOf(text: String): String =
        text.substring(SpelExpressionText.receiverStart(text)).trim()
}
