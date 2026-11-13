package com.luckyframework.idea.spel.completion

import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 9.7：函数插入补齐 `()` 且光标居中，变量插入不重复 `#`。
 */
class InsertHandlerTest : SymbolFixtureTestCase() {

    fun testFunctionInsertionAddsParenthesesAndMovesCaret() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{md<caret>")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        // 内置函数只支持裸名称调用；唯一候选不带签名后缀
        select("md5")

        val text = myFixture.file.text
        assertTrue(text.contains("md5()"))
        val expected = text.indexOf("md5(") + "md5(".length
        assertEquals(expected, myFixture.editor.caretModel.offset)
    }

    fun testVariableInsertionDoesNotDuplicateHash() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "userId=1")
class Api {
    @HttpRequest(url = "#{#us<caret>")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        select("#userId")

        val text = myFixture.file.text
        assertTrue(text.contains("#{#userId"))
        assertFalse(text.contains("##"))
    }

    private fun select(lookupString: String) {
        val lookup = myFixture.lookup
        assertNotNull(lookup)
        val item = lookup!!.items.firstOrNull { it.lookupString == lookupString }
        assertNotNull(item)
        lookup.currentItem = item
        myFixture.finishLookup('\n')
    }
}
