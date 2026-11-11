package com.luckyframework.idea.spel.activation

import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 未依赖 lucky-httpclient 的模块必须零干扰：
 * 不高亮、不补全、不产生引用。
 */
class ActivationIsolationTest : BasePlatformTestCase() {

    fun testNoHighlightWithoutDependency() {
        myFixture.configureByText("Demo.java", "class Demo { String s = \"a#{#x}b\"; }")
        val open = myFixture.file.text.indexOf("#{")

        val infos = myFixture.doHighlighting()

        assertFalse(infos.any { it.startOffset == open && it.endOffset == open + 2 })
    }

    fun testNoCompletionWithoutDependency() {
        myFixture.configureByText("Demo.java", "class Demo { String s = \"#{#<caret>\"; }")

        val elements = myFixture.completeBasic()

        assertTrue(elements == null || elements.isEmpty())
    }

    fun testNoReferencesWithoutDependency() {
        myFixture.configureByText("Demo.java", "class Demo { String s = \"#{#demo}\"; }")
        val literal = PsiTreeUtil.findChildOfType(myFixture.file, PsiLiteralExpression::class.java)
        assertNotNull(literal)

        assertTrue(literal!!.references.isEmpty())
    }
}
