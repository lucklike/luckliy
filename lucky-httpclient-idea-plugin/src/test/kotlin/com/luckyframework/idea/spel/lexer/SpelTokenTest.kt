package com.luckyframework.idea.spel.lexer

import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType
import junit.framework.TestCase

class SpelTokenTest : TestCase() {

    private val scanner = SpelScanner()
    private val tokenizer = SpelTokenizer()

    private fun tokensOf(literal: String): List<SpelToken> {
        val fragment = scanner.scanLiteral(literal).single()
        return tokenizer.tokenize(literal, fragment)
    }

    fun testFunctionVariableAndOperator() {
        val tokens = tokensOf("\"#{#time() + #offset}\"")

        assertEquals(SpelTokenType.FUNCTION, tokens.single { it.text == "#time" }.type)
        assertEquals(SpelTokenType.VARIABLE, tokens.single { it.text == "#offset" }.type)
        assertTrue(tokens.any { it.type == SpelTokenType.OPERATOR && it.text == "+" })
    }

    fun testRootVariableAndPropertyAccess() {
        val tokens = tokensOf("\"#{user.name}\"")

        assertEquals(SpelTokenType.ROOT_VARIABLE, tokens[0].type)
        assertEquals("user", tokens[0].text)
        assertTrue(tokens.any { it.type == SpelTokenType.DOT })
        assertEquals(SpelTokenType.IDENTIFIER, tokens.last().type)
        assertEquals("name", tokens.last().text)
    }

    fun testNamespaceFunction() {
        val tokens = tokensOf("\"#{#crypto.md5('a')}\"")

        assertEquals(SpelTokenType.NAMESPACE, tokens.single { it.text == "#crypto" }.type)
        assertTrue(tokens.any { it.type == SpelTokenType.STRING && it.text == "'a'" })
        assertEquals(SpelTokenType.IDENTIFIER, tokens.single { it.text == "md5" }.type)
    }

    /** 裸方法名后跟 `(` 是函数调用；`$body$` 是整体 Root 变量。 */
    fun testBareCallAndResponseVariable() {
        val tokens = tokensOf("\"#{chunk_success(\$body\$)}\"")

        assertEquals(SpelTokenType.FUNCTION, tokens.single { it.text == "chunk_success" }.type)
        assertEquals(SpelTokenType.ROOT_VARIABLE, tokens.single { it.text == "\$body\$" }.type)
    }

    fun testNewTypeIsTypeReference() {
        val tokens = tokensOf("\"#{new java.util.ArrayList()}\"")

        assertEquals(SpelTokenType.TYPE_REFERENCE, tokens.single { it.text == "ArrayList" }.type)
    }

    /** `instanceof` 的右操作数是 `T(类型)`，整个类型引用是一个 token。 */
    fun testInstanceofTypeIsTypeReference() {
        val token = tokensOf("\"#{${'$'}body${'$'} instanceof T(java.util.List)}\"")
            .single { it.type == SpelTokenType.TYPE_REFERENCE }

        assertEquals("T(java.util.List)", token.text)
    }

    /** `new` 的泛型实参也属于类型上下文，不能降级为 Root 变量。 */
    fun testNewTypeArgumentIsTypeReference() {
        val tokens = tokensOf("\"#{new java.util.ArrayList<String>()}\"")

        assertEquals(SpelTokenType.TYPE_REFERENCE, tokens.single { it.text == "String" }.type)
    }

    fun testTypeReference() {
        val token = tokensOf("\"#{T(java.util.ArrayList)}\"").single()

        assertEquals(SpelTokenType.TYPE_REFERENCE, token.type)
        assertEquals("T(java.util.ArrayList)", token.text)
    }

    fun testBeanReference() {
        val token = tokensOf("\"#{@userMapper}\"").single()

        assertEquals(SpelTokenType.BEAN, token.type)
        assertEquals("@userMapper", token.text)
    }

    /** 安全导航 `?.` 之后的标识符与 `.` 之后一样是成员。 */
    fun testSafeNavigationMarksMember() {
        val tokens = tokensOf("\"#{#user?.name}\"")

        assertTrue(tokens.any { it.type == SpelTokenType.OPERATOR && it.text == "?." })
        assertEquals("name", tokens.last().text)
        assertEquals(SpelTokenType.IDENTIFIER, tokens.last().type)
    }

    fun testStringNumberAndKeyword() {
        val tokens = tokensOf("\"#{'abc' + 1 + true}\"")

        assertEquals(SpelTokenType.STRING, tokens.single { it.text == "'abc'" }.type)
        assertEquals(SpelTokenType.NUMBER, tokens.single { it.text == "1" }.type)
        assertEquals(SpelTokenType.KEYWORD, tokens.single { it.text == "true" }.type)
    }

    /** `?[`/`^[`/`$[` 与 `![` 是选择/投影标记，与 `[` 一起构成一个 token。 */
    fun testSelectionAndProjectionMarkers() {
        assertEquals(
            SpelTokenType.SELECTION,
            tokensOf("\"#{#users.?[age > 1]}\"").single { it.text == "?[" }.type
        )
        assertEquals(
            SpelTokenType.SELECTION,
            tokensOf("\"#{#users.^[age > 1]}\"").single { it.text == "^[" }.type
        )
        assertEquals(
            SpelTokenType.SELECTION,
            tokensOf("\"#{#users.\$[age > 1]}\"").single { it.text == "\$[" }.type
        )
        assertEquals(
            SpelTokenType.PROJECTION,
            tokensOf("\"#{#users.![name]}\"").single { it.text == "![" }.type
        )
    }

    /** 安全导航 `?.` 后紧跟选择/投影标记：`?.` 是运算符，`?[`/`![` 是标记。 */
    fun testSafeNavigationSelectionAndProjection() {
        val selection = tokensOf("\"#{#users?.?[age > 1]}\"")
        assertTrue(selection.any { it.type == SpelTokenType.OPERATOR && it.text == "?." })
        assertTrue(selection.any { it.type == SpelTokenType.SELECTION && it.text == "?[" })

        val projection = tokensOf("\"#{#users?.![name]}\"")
        assertTrue(projection.any { it.type == SpelTokenType.OPERATOR && it.text == "?." })
        assertTrue(projection.any { it.type == SpelTokenType.PROJECTION && it.text == "![" })
    }

    /** `$body$[0]` 是下标访问，`$` 已属于前一个 Root 变量，不能拆成 `$[` 标记。 */
    fun testResponseVariableIndexKeepsBracket() {
        val tokens = tokensOf("\"#{${'$'}body${'$'}[0]}\"")

        assertEquals(SpelTokenType.ROOT_VARIABLE, tokens.single { it.text == "\$body\$" }.type)
        assertTrue(tokens.any { it.type == SpelTokenType.LBRACKET })
        assertTrue(tokens.none { it.type == SpelTokenType.SELECTION })
    }

    /** 标记与 `[` 之间有空白不成立，仍按两种运算符/标点处理。 */
    fun testMarkerWithSpaceIsNotSelection() {
        val tokens = tokensOf("\"#{#users.? [age > 1]}\"")

        assertTrue(tokens.none { it.type == SpelTokenType.SELECTION })
    }
}
