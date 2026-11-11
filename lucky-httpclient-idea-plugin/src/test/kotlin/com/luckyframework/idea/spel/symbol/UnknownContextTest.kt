package com.luckyframework.idea.spel.symbol

/**
 * 6.6 未知上下文降级：普通业务字符串只有语法高亮，不进入符号解析。
 */
class UnknownContextTest : SymbolFixtureTestCase() {

    fun testLiteralOutsideAnnotationIsUnknown() {
        configure(
            """
package p;

class Api {
    void query(String host) {
        String text = "#{notASpelVariable}";
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{notASpelVariable}"))

        assertFalse(context.known)
        assertTrue(context.symbols.isEmpty())
        assertNull(context.attribute)
    }

    fun testUnknownAnnotationIsNotAKnownContext() {
        myFixture.addFileToProject(
            "com/example/Plain.java",
            """
package com.example;

public @interface Plain {
    String value() default "";
}
"""
        )
        configure(
            """
package p;

import com.example.Plain;

class Api {
    @Plain("#{notASpelVariable}")
    void query() {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{notASpelVariable}"))

        assertFalse(context.known)
    }

    fun testKnownAnnotationKeepsContext() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        assertTrue(newBuilder().build(literalOf("#{x}")).known)
    }
}
