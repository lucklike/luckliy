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
    @HttpRequest(url = "#{userId<caret>}")
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
        assertTrue("参数应标注裸名称引用: $text", text.contains("引用形式"))
        assertTrue("参数应按裸名称引用: $text", text.contains("userId（裸名称）"))
    }

    /** 普通变量（`@SpELImport(var = ...)`）文档标注只能 `#name` 引用。 */
    fun testImportVariableDocumentationShowsHashForm() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "token=1")
class Api {
    @HttpRequest(url = "#{#token<caret>}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应标注引用形式: $text", text.contains("引用形式"))
        assertTrue("普通变量应按 #name 引用: $text", text.contains("#token"))
    }

    /** 书写形式错误时文档给出正确写法，而不是「未解析」提示。 */
    fun testFormErrorDocumentationShowsCorrectForm() {
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

        assertTrue("应说明引用形式错误: $text", text.contains("引用形式错误"))
        assertTrue("应给出裸名称写法: $text", text.contains("只能用裸名称引用"))
        assertFalse("不应再提示未解析: $text", text.contains("未解析的 SpEL 符号"))
    }

    /** `@If(condition = "#{false}")` 的类未被导入：文档说明条件未生效，而不是「未解析」。 */
    fun testDisabledConditionDocumentation() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{false}", classes = MyUtils.class))
    @HttpRequest(url = "#{#sha<caret>('a')}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应说明条件导入未生效: $text", text.contains("条件导入未生效"))
        assertTrue("应说明类未被导入: $text", text.contains("类未被导入"))
        assertFalse("不应再提示未解析: $text", text.contains("未解析的 SpEL 符号"))
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
    @HttpRequest(url = "#{time<caret>()}")
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
        assertTrue("内置函数应标注裸调用形式: $text", text.contains("引用形式"))
        assertTrue("内置函数应按 name(...) 调用: $text", text.contains("time(...)"))
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

    /** 选择/投影方括号内的 `#this` 悬停显示元素类型与当前元素说明。 */
    fun testSelectionThisDocumentation() {
        myFixture.addFileToProject(
            "com/example/ElementUser.java",
            """
package com.example;

public class ElementUser {

    private String name;

    public String getName() {
        return name;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[#this<caret>]}")
    void call(ElementUser[] users) {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应展示内置变量: $text", text.contains("内置变量"))
        assertTrue("应展示元素类型: $text", text.contains("ElementUser"))
        assertTrue("应说明当前元素: $text", text.contains("当前元素"))
    }

    /** 投影方括号内的 `#this` 同样按元素类型展示。 */
    fun testProjectionThisDocumentation() {
        myFixture.addFileToProject(
            "com/example/ElementUser.java",
            """
package com.example;

public class ElementUser {

    private String name;

    public String getName() {
        return name;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.![#this<caret>]}")
    void call(ElementUser[] users) {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应展示元素类型: $text", text.contains("ElementUser"))
        assertTrue("应说明当前元素: $text", text.contains("当前元素"))
    }

    /** 顶层 `#this` 不在元素上下文中，按表达式当前对象展示。 */
    fun testTopLevelThisDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#this<caret>}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应展示内置变量: $text", text.contains("内置变量"))
        assertTrue("应说明当前对象: $text", text.contains("当前对象"))
    }

    /** `#root` 悬停同样有内置对象文档，不再落到未解析符号。 */
    fun testRootObjectDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#root<caret>}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应展示内置变量: $text", text.contains("内置变量"))
        assertTrue("应说明根对象: $text", text.contains("根对象"))
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
    @HttpRequest(url = "#{userId<caret>}")
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
    @HttpRequest(url = "#{userId<caret>}")
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
    @HttpRequest(url = "#{userId<caret>}")
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
    @HttpRequest(url = "#{userId<caret>}")
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

    /** `new` 后的类型名与 T() 一样要展示类型文档。 */
    fun testNewTypeDocumentation() {
        myFixture.addFileToProject(
            "com/example/SpelPair.java",
            """
package com.example;

/** 键值对。 */
public class SpelPair {
    public SpelPair(int left, int right) {
    }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{new com.example.SpelPair<caret>(1, 2)}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应展示类型短名: $text", text.contains("SpelPair"))
        assertTrue("应展示全限定名: $text", text.contains("com.example.SpelPair"))
        assertTrue("应展示类型注释: $text", text.contains("键值对"))
    }

    /** `instanceof T(...)` 中的类型名同样展示类型文档。 */
    fun testInstanceofTypeDocumentation() {
        myFixture.addFileToProject(
            "com/example/SpelUser.java",
            "package com.example;\n\n/** 用户。 */\npublic class SpelUser {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{body instanceof T(com.example.SpelUser<caret>)}")
    void call(Object body) {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应展示类型文档: $text", text.contains("SpelUser"))
        assertTrue("应展示全限定名: $text", text.contains("com.example.SpelUser"))
    }

    /** `java.lang` 短名（`T(Object)`）按 java.lang 展示文档，而不是未解析提示。 */
    fun testJavaLangShortTypeDocumentation() {
        // 测试工程不含 JDK 的 java.lang 类，补 Object 桩模拟真实环境
        myFixture.addFileToProject(
            "java/lang/Object.java",
            "package java.lang;\npublic class Object {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{body instanceof T(Object<caret>)}")
    void call(Object body) {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertFalse("不应提示未解析类型: $text", text.contains("未解析的 SpEL 类型"))
        assertTrue("应展示 java.lang.Object: $text", text.contains("java.lang.Object"))
    }

    /** 未解析的 `new` 类型同样给出类型专属提示。 */
    fun testUnresolvedNewTypeDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{new com.unknown.Missing<caret>()}")
    void call() {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应提示未解析的 SpEL 类型: $text", text.contains("未解析的 SpEL 类型"))
        assertTrue("应回显类型名: $text", text.contains("com.unknown.Missing"))
    }

    /** 函数结果作为接收者时，悬停文档展示成员方法的签名与所属类。 */
    fun testFunctionResultMethodDocumentation() {
        myFixture.addFileToProject(
            "com/example/Bag.java",
            """
package com.example;

public class Bag<E> {
    /** 按下标取元素。 */
    public E get(int index) {
        return null;
    }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(1, 3).get<caret>(0)}")
    void call() {
    }

    public static com.example.Bag<Integer> add(Integer a, Integer b) {
        return null;
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应展示方法签名: $text", text.contains("get(int)"))
        assertTrue("应展示方法注释: $text", text.contains("按下标取元素"))
        assertTrue("应展示所属类: $text", text.contains("com.example.Bag"))
    }

    /** 重载方法按调用实参展示文档：字符串实参展示 `pick(String)`。 */
    fun testOverloadDocumentationPicksCalledSignature() {
        myFixture.addFileToProject(
            "com/example/OverloadedPicker.java",
            """
package com.example;

public class OverloadedPicker {
    public String pick(int id) {
        return "";
    }

    public String pick(String name) {
        return name;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.OverloadedPicker;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{picker.pick<caret>('a')}")
    void call(OverloadedPicker picker) {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应展示字符串重载: $text", text.contains("pick(String)"))
        assertFalse("不应展示整数重载: $text", text.contains("pick(int)"))
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
