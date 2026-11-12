package com.luckyframework.idea.spel.documentation

import com.intellij.lang.documentation.impl.documentationTargets
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 11.1–11.4：变量、函数、内置符号与未解析符号的悬停文档。
 */
class SpelDocumentationTest : SymbolFixtureTestCase() {

    fun testParameterVariableDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId<caret>}")
    void call(String userId) {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue(text.contains("userId"))
        assertTrue(text.contains("方法参数"))
        assertTrue(text.contains("String"))
        assertTrue(text.contains("当前方法"))
    }

    fun testRootVariableDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(root = "user=#{1}")
class Api {
    @HttpRequest(url = "#{user<caret>.name}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue(text.contains("Root 变量"))
        assertTrue(text.contains("@SpELImport"))
    }

    fun testBuiltinFunctionDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#time<caret>()}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue(text.contains("time()"))
        assertTrue(text.contains("long"))
        assertTrue(text.contains("CommonFunctions"))
        assertTrue(text.contains("获取当前时间毫秒"))
    }

    fun testAliasFunctionDocumentation() {
        myFixture.addFileToProject(
            "com/example/HashUtils.java",
            """
package com.example;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

public class HashUtils {
    @FunctionAlias("md5")
    public static String sha256(String text) {
        return text;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.HashUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(HashUtils.class)
    @HttpRequest(url = "#{#md5<caret>('a')}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue(text.contains("别名"))
        assertTrue(text.contains("md5"))
        assertTrue(text.contains("sha256"))
    }

    /** 当前类静态方法作为函数时，文档展示签名、所属类与作用域。 */
    fun testClassStaticFunctionDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{localHelper<caret>('a')}")
    void call() {
    }

    public static String localHelper(String text) {
        return text;
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue(text.contains("localHelper(String)"))
        assertTrue(text.contains("当前类及其父类、父接口的静态方法"))
        assertTrue(text.contains("p.Api"))
    }

    fun testBuiltinVariableDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}body${'$'}<caret>}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue(text.contains("响应体"))
        assertTrue(text.contains("响应阶段可用"))
    }

    fun testUnresolvedSymbolDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#notDefined<caret>}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue(text.contains("未解析"))
        assertTrue(text.contains("请检查变量名或当前上下文是否可见"))
    }

    /** 虚拟声明必须带父元素，否则平台创建悬停 SmartPointer 时会抛异常。 */
    fun testDocumentationElementHasParent() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId<caret>}")
    void call(String userId) {
    }
}
"""
        )
        myFixture.doHighlighting()
        val element = SpelDocumentationProvider().getCustomDocumentationElement(
            myFixture.editor,
            myFixture.file,
            null,
            myFixture.caretOffset
        )

        assertNotNull("未找到 SpEL 文档元素", element)
        assertNotNull("虚拟声明缺少父元素", element!!.parent)
        assertNotNull("虚拟声明缺少所在文件", element.containingFile)
    }

    /** 2026.3 起悬停与 Quick Doc 优先使用文档目标，虚拟声明必须能产出非空文档。 */
    fun testDocumentationTargetProviderProducesDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId<caret>}")
    void call(String userId) {
    }
}
"""
        )
        myFixture.doHighlighting()
        val targets = SpelDocumentationTargetProvider().documentationTargets(myFixture.file, myFixture.caretOffset)

        assertEquals("应命中一个 SpEL 文档目标", 1, targets.size)
        assertNotNull("文档目标缺少文档", targets[0].computeDocumentation())
        assertTrue(targets[0].computeDocumentationHint()!!.contains("方法参数"))
    }

    /** 未解析符号同样要产出文档目标，避免新版平台悬停空白。 */
    fun testDocumentationTargetProviderCoversUnresolvedSymbol() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#notDefined<caret>}")
    void call() {
    }
}
"""
        )
        myFixture.doHighlighting()
        val targets = SpelDocumentationTargetProvider().documentationTargets(myFixture.file, myFixture.caretOffset)

        assertEquals(1, targets.size)
        assertNotNull(targets[0].computeDocumentation())
        assertTrue(targets[0].computeDocumentationHint()!!.contains("未解析"))
    }

    /** 非 SpEL 字符串不产出目标，保持普通 Java 文档行为。 */
    fun testDocumentationTargetProviderIgnoresPlainLiteral() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "plain<caret>")
    void call() {
    }
}
"""
        )
        myFixture.doHighlighting()
        val targets = SpelDocumentationTargetProvider().documentationTargets(myFixture.file, myFixture.caretOffset)

        assertTrue("普通字符串不应命中 SpEL 文档", targets.isEmpty())
    }

    /** 扩展必须注册到平台目标提供器 EP，否则 2026.3 不会调用。 */
    fun testDocumentationTargetProviderRegistered() {
        val registered = DocumentationTargetProvider.EP_NAME.extensionList
            .any { it is SpelDocumentationTargetProvider }

        assertTrue("SpelDocumentationTargetProvider 未注册到 documentation.targetProvider", registered)
    }

    /** 补全文档预览的扩展必须注册到 psiTargetProvider EP，否则右侧文档区不会渲染。 */
    fun testPsiDocumentationTargetProviderRegistered() {
        val registered = PsiDocumentationTargetProvider.EP_NAME.extensionList
            .any { it is SpelPsiDocumentationTargetProvider }

        assertTrue("SpelPsiDocumentationTargetProvider 未注册到 documentation.psiTargetProvider", registered)
    }

    /** 平台目标收集函数（2026.3 悬停/Quick Doc 的实际入口）必须能拿到 SpEL 目标。 */
    fun testPlatformDocumentationTargetsContainSpelTarget() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId<caret>}")
    void call(String userId) {
    }
}
"""
        )
        myFixture.doHighlighting()
        val targets = documentationTargets(myFixture.file, myFixture.caretOffset)

        assertTrue("平台未产出 SpEL 文档目标", targets.any { it is SpelDocumentationTarget })
    }

    /** 新版平台从 DocumentationParts 取文档时也要有内容。 */
    fun testDocumentationPartsExposeContent() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId<caret>}")
    void call(String userId) {
    }
}
"""
        )
        myFixture.doHighlighting()
        val provider = SpelDocumentationProvider()
        val element = provider.getCustomDocumentationElement(
            myFixture.editor,
            myFixture.file,
            null,
            myFixture.caretOffset
        )
        assertNotNull(element)

        val parts = provider.getDocumentationParts(element!!, null)

        assertNotNull("DocumentationParts 为空", parts)
        assertTrue(parts!!.doc.contains("userId"))
    }

    /** 悬停在 `T` 上也要展示类型文档，不能因为引用只覆盖类型名而缺席。 */
    fun testTypeReferenceDocumentationFromTypeKeyword() {
        myFixture.addFileToProject(
            "java/util/ArrayList.java",
            "package java.util;\n/** 动态数组 */\npublic class ArrayList<E> {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{<caret>T(java.util.ArrayList)}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应展示类型短名: $text", text.contains("ArrayList"))
        assertTrue("应展示全限定名: $text", text.contains("java.util.ArrayList"))
        assertTrue("应展示类型注释: $text", text.contains("动态数组"))
    }

    /** 嵌套类型（`Outer.Inner`）是合法的 T() 目标，悬停必须能解析。 */
    fun testTypeReferenceDocumentationForNestedClass() {
        myFixture.addFileToProject(
            "org/springframework/data/redis/connection/stream/StreamRecords.java",
            """
package org.springframework.data.redis.connection.stream;

public class StreamRecords {
    public static class RecordBuilder {
    }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(org.springframework.data.redis.connection.stream.StreamRecords.RecordBuilder)<caret>}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应解析嵌套类型: $text", text.contains("RecordBuilder"))
        assertTrue(
            "应展示嵌套类型全限定名: $text",
            text.contains("org.springframework.data.redis.connection.stream.StreamRecords.RecordBuilder")
        )
    }

    /** 未解析的 T() 类型要用类型专属提示，而不是「请检查变量名」。 */
    fun testUnresolvedTypeReferenceDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(com.unknown.Missing)<caret>}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应提示未解析的 SpEL 类型: $text", text.contains("未解析的 SpEL 类型"))
        assertTrue("应给出类型检查建议: $text", text.contains("请检查类型名是否正确"))
    }

    private fun documentationAtCaret(): String {
        // 等待索引完成，模拟真实悬停时索引已就绪的状态
        myFixture.doHighlighting()
        val provider = SpelDocumentationProvider()
        val documentationElement = provider.getCustomDocumentationElement(
            myFixture.editor,
            myFixture.file,
            null,
            myFixture.caretOffset
        )
        assertNotNull("未找到 SpEL 文档元素", documentationElement)
        val documentation = provider.generateDoc(documentationElement, null)
        assertNotNull("文档为空", documentation)
        return documentation!!
    }
}
