package com.luckyframework.idea.spel.lexer

import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelProblemKind
import junit.framework.TestCase

class SpelScannerTest : TestCase() {

    private val scanner = SpelScanner()

    fun testSingleFragmentInLiteral() {
        val fragments = scanner.scanLiteral("\"a#{#x}b\"")

        assertEquals(1, fragments.size)
        val fragment: SpelFragment = fragments[0]
        assertEquals(2, fragment.textRange.startOffset)
        assertEquals(7, fragment.textRange.endOffset)
        assertEquals("#x", fragment.content)
        assertTrue(fragment.closed)
    }

    fun testEscapedQuoteKeepsSourceOffsets() {
        val fragments = scanner.scanLiteral("\"a\\\"b#{#x}\"")

        assertEquals(1, fragments.size)
        assertEquals(5, fragments[0].textRange.startOffset)
        assertEquals(10, fragments[0].textRange.endOffset)
    }

    fun testEscapedBackslashKeepsSourceOffsets() {
        val fragments = scanner.scanLiteral("\"a\\\\b#{#x}\"")

        assertEquals(1, fragments.size)
        assertEquals(5, fragments[0].textRange.startOffset)
    }

    fun testUnclosedFragment() {
        val fragments = scanner.scanLiteral("\"#{foo\"")

        assertEquals(1, fragments.size)
        assertFalse(fragments[0].closed)
        assertEquals(1, fragments[0].textRange.startOffset)
    }

    fun testMultipleFragmentsKeepOffsets() {
        val fragments = scanner.scanLiteral("\"X-TIME: #{#time()}; X-ID: #{#id}\"")

        assertEquals(2, fragments.size)
        assertEquals(9, fragments[0].textRange.startOffset)
        assertEquals(19, fragments[0].textRange.endOffset)
        assertEquals("#time()", fragments[0].content)
        assertEquals(27, fragments[1].textRange.startOffset)
        assertEquals(33, fragments[1].textRange.endOffset)
        assertEquals("#id", fragments[1].content)
    }

    fun testBraceInsideStringDoesNotCloseFragment() {
        val fragments = scanner.scanLiteral("\"#{ {'a': '}'} }\"")

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].closed)
        assertEquals(1, fragments[0].textRange.startOffset)
        assertEquals(16, fragments[0].textRange.endOffset)
    }

    fun testUnclosedExpressionProducesProblem() {
        val fragment = scanner.scanLiteral("\"#{foo\"").single()

        assertFalse(fragment.closed)
        assertTrue(fragment.problems.any { it.kind == SpelProblemKind.UNCLOSED_EXPRESSION })
    }

    fun testUnclosedStringProducesProblem() {
        val fragment = scanner.scanLiteral("\"#{'abc}\"").single()

        assertFalse(fragment.closed)
        assertTrue(fragment.problems.any { it.kind == SpelProblemKind.UNCLOSED_STRING })
        assertTrue(fragment.problems.any { it.kind == SpelProblemKind.UNCLOSED_EXPRESSION })
    }

    fun testUnbalancedBracketsProduceProblem() {
        val fragment = scanner.scanLiteral("\"#{#fn(1, 2}\"").single()

        assertTrue(fragment.closed)
        val problem = fragment.problems.single { it.kind == SpelProblemKind.UNBALANCED_BRACKETS }
        assertEquals(6, problem.range.startOffset)
    }

    fun testExtraClosingBracketProducesProblem() {
        val fragment = scanner.scanLiteral("\"#{#fn(1))}\"").single()

        assertTrue(fragment.closed)
        val problem = fragment.problems.single {
            it.kind == SpelProblemKind.UNEXPECTED_CLOSING_BRACKET
        }
        assertEquals(9, problem.range.startOffset)
        assertTrue("应说明多余的右括号: ${problem.message}", problem.message.contains("多余的 ')'"))
    }

    fun testMismatchedBracketTypeProducesProblem() {
        val fragment = scanner.scanLiteral("\"#{#fn([1)}\"").single()

        val problem = fragment.problems.single {
            it.kind == SpelProblemKind.UNEXPECTED_CLOSING_BRACKET
        }
        assertTrue("应说明括号类型不配对: ${problem.message}", problem.message.contains("不配对"))
    }

    fun testIfPrefixIsRecognizedButNotInContent() {
        val fragment = scanner.scanLiteral("\"@if(#{#enabled}): X\"").single()

        assertTrue(fragment.conditional)
        assertEquals("#enabled", fragment.content)
    }

    fun testMaxPrefixAndBackticks() {
        val fragment = scanner.scanLiteral("\"``@max(3):#{#key}``\"").single()

        assertEquals(3, fragment.maxDepth)
        assertTrue(fragment.nested)
    }

    fun testBacktickNestingProducesInnerFragment() {
        val outer = scanner.scanLiteral("\"``#{#{#key}}``\"").single()

        assertTrue(outer.nested)
        val inner = outer.children.single()
        assertEquals("#key", inner.content)
    }
}
