package com.luckyframework.idea.spel.symbol

/**
 * 6.3 `@SpELImport` 声明解析：默认分隔符、自定义分隔符、缺分隔符，
 * 以及 `varLit` 按普通变量处理的公开语义（不复制运行期偏差）。
 */
class SpelImportParserTest : SymbolFixtureTestCase() {

    private val parser = SpelImportParser()

    fun testDefaultSeparatorAndSpelValue() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(var = "token=#{#time()}")
    void a() {
    }
}
"""
        )
        val annotation = methodAnnotation("a", SpelImportParser.SPEL_IMPORT)
        val declaration = parser.parse(annotation, SpelSymbolOrigin.METHOD_IMPORT).single()

        assertEquals("token", declaration.name)
        assertEquals("#{#time()}", declaration.value)
        assertEquals(SpelSymbolKind.VARIABLE, declaration.kind)
        assertTrue(declaration.valueIsSpel)
        assertNull(declaration.problem)
        assertEquals("token", declaration.literal.text.substring(declaration.nameRange!!.startOffset, declaration.nameRange!!.endOffset))
    }

    fun testCustomSeparatorAndLiteralDeclarations() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(rootLit = "user:admin", varLit = "answer:42", separator = ":")
    void a() {
    }
}
"""
        )
        val annotation = methodAnnotation("a", SpelImportParser.SPEL_IMPORT)
        val declarations = parser.parse(annotation, SpelSymbolOrigin.CLASS_IMPORT)

        assertEquals(2, declarations.size)
        val root = declarations.single { it.kind == SpelSymbolKind.ROOT_VARIABLE }
        assertEquals("user", root.name)
        assertEquals("admin", root.value)
        assertFalse(root.valueIsSpel)

        val normal = declarations.single { it.kind == SpelSymbolKind.VARIABLE }
        assertEquals("answer", normal.name)
        assertEquals("42", normal.value)
        assertFalse(normal.valueIsSpel)
    }

    fun testMissingSeparatorIsReported() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(var = "token")
    void a() {
    }
}
"""
        )
        val annotation = methodAnnotation("a", SpelImportParser.SPEL_IMPORT)
        val declaration = parser.parse(annotation, SpelSymbolOrigin.METHOD_IMPORT).single()

        assertNull(declaration.name)
        assertEquals(SpelImportProblem.MISSING_SEPARATOR, declaration.problem)
    }

    fun testEmptyNameIsReported() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(var = " =#{1}")
    void a() {
    }
}
"""
        )
        val annotation = methodAnnotation("a", SpelImportParser.SPEL_IMPORT)
        val declaration = parser.parse(annotation, SpelSymbolOrigin.METHOD_IMPORT).single()

        assertEquals("", declaration.name)
        assertEquals(SpelImportProblem.EMPTY_NAME, declaration.problem)
    }

    fun testRepeatableContainerAndMetaAnnotation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.SpELImports;
import com.luckyframework.httpclient.proxy.spel.MyImport;

class Api {
    @SpELImports({@SpELImport(var = "a=1"), @SpELImport(root = "b=2")})
    void repeated() {
    }

    @MyImport
    void composed() {
    }
}
"""
        )
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/proxy/spel/MyImport.java",
            """
package com.luckyframework.httpclient.proxy.spel;

@SpELImport(var = "z=1")
public @interface MyImport {
}
"""
        )

        val repeated = parser.findImportAnnotations(methodNamed("repeated"))
        assertEquals(2, repeated.size)

        val composed = parser.findImportAnnotations(methodNamed("composed"))
        assertEquals(1, composed.size)
        assertEquals("z", parser.parse(composed.single(), SpelSymbolOrigin.METHOD_IMPORT).single().name)
    }

    fun testPackAndConditionalClassParsing() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(pack = {"java.util", "java.time"}, ifs = @If(condition = "#{true}", classes = MyUtils.class))
    void a() {
    }
}
"""
        )
        val annotation = methodAnnotation("a", SpelImportParser.SPEL_IMPORT)

        assertEquals(listOf("java.util", "java.time"), parser.parsePackages(annotation))
        assertEquals(listOf("com.example.MyUtils"), parser.parseConditionalClasses(annotation).map { it.qualifiedName })
    }
}
