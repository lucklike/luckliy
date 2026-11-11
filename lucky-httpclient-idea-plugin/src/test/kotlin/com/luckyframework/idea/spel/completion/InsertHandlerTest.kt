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

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{#md<caret>")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        // 平铺视图下与内置 md5 同名，同名函数按签名区分 lookup
        select("#md5(String)")

        val text = myFixture.file.text
        assertTrue(text.contains("#md5()"))
        val expected = text.indexOf("#md5(") + "#md5(".length
        assertEquals(expected, myFixture.editor.caretModel.offset)
    }

    fun testVariableInsertionDoesNotDuplicateHash() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#us<caret>")
    void call(String userId) {
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
