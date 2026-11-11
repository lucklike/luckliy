package com.luckyframework.idea.spel.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 9.8：模块没有 lucky-httpclient 依赖时补全安全退出。
 */
class SpelCompletionIsolationTest : BasePlatformTestCase() {

    fun testNoDependencyProducesNoCompletion() {
        myFixture.configureByText(
            "Demo.java",
            "class Demo { @Deprecated String value = \"#{#<caret>\"; }"
        )
        myFixture.completeBasic()
        assertTrue(myFixture.lookupElements.isNullOrEmpty())
    }
}
