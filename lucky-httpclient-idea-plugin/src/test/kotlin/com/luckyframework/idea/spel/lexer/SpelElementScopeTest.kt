package com.luckyframework.idea.spel.lexer

import junit.framework.TestCase

/**
 * 选择/投影作用域扫描：补全、成员检查与引用解析共用的文本规则。
 */
class SpelElementScopeTest : TestCase() {

    fun testSelectionMarkersProduceScopes() {
        assertEquals(listOf("members"), basesAt("members.?[<caret>]"))
        assertEquals(listOf("members"), basesAt("members.^[<caret>]"))
        assertEquals(listOf("members"), basesAt("members.\$[<caret>]"))
        assertEquals(listOf("members"), basesAt("members.![<caret>]"))
    }

    fun testSafeNavigationScopeBase() {
        assertEquals(listOf("members"), basesAt("members?.?[<caret>]"))
        assertEquals(listOf("members"), basesAt("members?.![<caret>]"))
    }

    /** 起始形式 `?[...]` 直接作用于当前元素/根对象，没有接收者。 */
    fun testMarkerWithoutReceiverHasEmptyBase() {
        assertEquals(listOf(""), basesAt("?[<caret>]"))
        assertEquals(listOf(""), basesAt("fn(?[<caret>])"))
    }

    /** 嵌套选择/投影由外到内返回，每层各自带有接收者。 */
    fun testNestedScopesAreOrderedOutsideIn() {
        assertEquals(listOf("groups", "tags"), basesAt("groups.?[tags.?[<caret>]]"))
        assertEquals(
            listOf("groups", "#this.tags"),
            basesAt("groups.?[#this.tags.?[<caret>]]")
        )
    }

    /** 已闭合的作用域不再包裹后续位置。 */
    fun testClosedScopesAreNotReturned() {
        assertTrue(scopesAt("members.?[age].<caret>").isEmpty())
        assertTrue(scopesAt("members.?[age] + <caret>").isEmpty())
        assertTrue(scopesAt("fn(list.?[x], <caret>)").isEmpty())
    }

    /** `$body$[0]` 是响应变量的下标访问，不是 `$[` 选择表达式。 */
    fun testResponseVariableIndexIsNotSelection() {
        assertTrue(scopesAt("\$body\$[0].<caret>").isEmpty())
        assertEquals(listOf("\$body\$[0]"), basesAt("\$body\$[0].?[<caret>]"))
    }

    /** 字符串中的标记字符不产生作用域。 */
    fun testStringContentIsIgnored() {
        assertTrue(scopesAt("fn('?[', <caret>)").isEmpty())
        assertEquals(listOf("members"), basesAt("members.?[name == '?['<caret>]"))
    }

    /** 普通下标不影响外层元素作用域。 */
    fun testOrdinaryIndexInsideSelection() {
        assertEquals(listOf("members"), basesAt("members.?[a[0] > <caret>]"))
        assertEquals(listOf("members"), basesAt("list[members.?[<caret>]]"))
    }

    fun testScopeOffsetsPointInsideBrackets() {
        val text = "members.?[age > 1]"
        val offset = text.indexOf("age")
        val scope = SpelElementScopes.enclosing(text, 0, offset).single()

        assertEquals(text.indexOf("?"), scope.markerOffset)
        assertEquals(text.indexOf('[') + 1, scope.innerStart)
        assertEquals(offset, scope.innerEnd)
    }

    /** 用 `<caret>` 标出查询位置，返回该位置所在的作用域（由外到内）。 */
    private fun scopesAt(marked: String): List<SpelElementScope> {
        val offset = marked.indexOf(MARKER)
        assertTrue("缺少 $MARKER: $marked", offset >= 0)
        return SpelElementScopes.enclosing(marked.replace(MARKER, ""), 0, offset)
    }

    private fun basesAt(marked: String): List<String> = scopesAt(marked).map { it.base }

    private companion object {
        const val MARKER = "<caret>"
    }
}
