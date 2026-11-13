package com.luckyframework.idea.spel.symbol

import junit.framework.TestCase

/**
 * `@If#condition` 静态求值：只覆盖可脱离运行期上下文确定的布尔表达式，
 * 其余形式必须返回 `null`，由调用方保持「按已导入处理」。
 */
class SpelStaticConditionTest : TestCase() {

    fun testBooleanLiterals() {
        assertEquals(false, SpelStaticCondition.evaluate("#{false}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{true}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{ true }"))
        assertEquals(false, SpelStaticCondition.evaluate("false"))
        assertEquals(true, SpelStaticCondition.evaluate("true"))
    }

    fun testBooleanOperators() {
        assertEquals(true, SpelStaticCondition.evaluate("#{!false}"))
        assertEquals(false, SpelStaticCondition.evaluate("#{not true}"))
        assertEquals(false, SpelStaticCondition.evaluate("#{false && true}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{false || true}"))
        assertEquals(false, SpelStaticCondition.evaluate("#{true and false}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{true or false}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{!(false && (true || false))}"))
    }

    fun testNumericComparisons() {
        assertEquals(true, SpelStaticCondition.evaluate("#{1==1}"))
        assertEquals(false, SpelStaticCondition.evaluate("#{1!=1}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{1 == 1.0}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{2 > 1}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{1 < 2}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{1 <= 1}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{2 >= 2}"))
        assertEquals(false, SpelStaticCondition.evaluate("#{2 <= 1}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{1 gt 0}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{-1 < 0}"))
    }

    fun testStringAndBooleanComparisons() {
        assertEquals(true, SpelStaticCondition.evaluate("#{'a' == 'a'}"))
        assertEquals(false, SpelStaticCondition.evaluate("#{'a' == 'b'}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{'a' < 'b'}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{'abc' ne 'abd'}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{true == true}"))
        assertEquals(false, SpelStaticCondition.evaluate("#{true == false}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{true ne false}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{null == null}"))
        assertEquals(false, SpelStaticCondition.evaluate("#{1 == '1'}"))
    }

    fun testComparisonCombinedWithLogic() {
        assertEquals(true, SpelStaticCondition.evaluate("#{1 == 1 and 2 > 1}"))
        assertEquals(false, SpelStaticCondition.evaluate("#{1 == 1 and 2 < 1}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{1 == 2 or 2 > 1}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{!(1 == 2)}"))
        assertEquals(true, SpelStaticCondition.evaluate("#{!(false && (1 > 2))}"))
    }

    /** 引用变量、配置占位符、算术运算与多片段模板都无法静态确定。 */
    fun testUnknownConditions() {
        assertNull(SpelStaticCondition.evaluate("#{#flag}"))
        assertNull(SpelStaticCondition.evaluate("#{#flag == null}"))
        assertNull(SpelStaticCondition.evaluate("#{1 + 1 == 2}"))
        assertNull(SpelStaticCondition.evaluate("#{'a' matches 'a'}"))
        assertNull(SpelStaticCondition.evaluate("#{true and false} and #{true}"))
        assertNull(SpelStaticCondition.evaluate("#{${'$'}{feature.enabled}}"))
        assertNull(SpelStaticCondition.evaluate("#{true} extra"))
        assertNull(SpelStaticCondition.evaluate(""))
        assertNull(SpelStaticCondition.evaluate("#{true"))
    }
}
