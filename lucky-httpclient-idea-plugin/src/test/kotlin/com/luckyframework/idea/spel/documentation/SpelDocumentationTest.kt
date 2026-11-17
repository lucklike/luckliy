package com.luckyframework.idea.spel.documentation

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.documentation.impl.documentationTargets
import com.intellij.lang.documentation.psi.PsiElementDocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.luckyframework.idea.spel.reference.SpelDeclaration
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

        // 参数悬停与 IDEA 内置一致：展示参数所属方法的原生文档（参数名 + 类型）
        assertTrue(text.contains("userId"))
        assertTrue(text.contains("String"))
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

        assertTrue(text.contains("time"))
        assertTrue(text.contains("long"))
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

        // 原生方法文档展示真实方法名与参数；别名只是插件的调用名，不在 Java 文档中
        assertTrue("应展示真实方法名 sha256: $text", text.contains("sha256"))
        assertTrue("应展示参数类型 String: $text", text.contains("String"))
        assertTrue("应展示参数名 text: $text", text.contains("text"))
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

        assertTrue(text.contains("localHelper"))
        assertTrue(text.contains("String"))
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
        assertTrue(text.contains("响应初始化阶段可用"))
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
        assertTrue(
            "参数文档必须交给平台原生目标渲染: ${targets[0].javaClass.name}",
            targets[0] is PsiElementDocumentationTarget
        )
        val hint = targets[0].computeDocumentationHint()
        assertTrue("悬停提示缺少参数名: $hint", hint != null && hint.contains("userId"))
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

        assertTrue("平台未产出 SpEL 文档目标", targets.isNotEmpty())
        assertTrue(
            "参数文档应由平台原生目标渲染: ${targets.map { it.javaClass.name }}",
            targets.any { it is PsiElementDocumentationTarget }
        )
    }

    /** 新版平台从 DocumentationParts 取文档时也要有内容（虚拟声明兜底路径）。 */
    fun testDocumentationPartsExposeContent() {
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
        assertTrue(parts!!.doc.contains("未解析"))
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
        assertTrue("应展示包名: $text", text.contains("java.util"))
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
            "应展示嵌套类型所属包: $text",
            text.contains("org.springframework.data.redis.connection.stream")
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
        assertTrue("应展示包名: $text", text.contains("com.example"))
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
        assertTrue("应展示包名: $text", text.contains("com.example"))
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
        assertTrue("应展示所属包 java.lang: $text", text.contains("java.lang"))
        assertTrue("应展示类型名 Object: $text", text.contains("Object"))
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

        assertTrue("应展示方法名: $text", text.contains("get"))
        assertTrue("应展示参数类型: $text", text.contains("int"))
        assertTrue("应展示方法注释: $text", text.contains("按下标取元素"))
        assertTrue("应展示所属类: $text", text.contains("com.example.Bag"))
    }

    /**
     * 用户实测场景：悬停方法时必须是 IDEA 内置 Quick Doc 的页面结构——
     * 原生定义区 + 方法签名 + 返回类型 + 所属类 + Javadoc 正文，
     * 而不是插件自制的「方法/来源/签名/返回类型/所属类」表格。
     *
     * 测试工程未附带 mock JDK，`java.lang.String` 不可见，因此用项目内
     * 带 Javadoc 的等价类验证同一条原生文档链路。
     */
    fun testMethodDocumentationUsesBuiltinQuickDocHtml() {
        myFixture.addFileToProject(
            "com/example/Text.java",
            """
package com.example;

public class Text {
    /**
     * 转为大写。
     *
     * @return 大写文本
     */
    public String toUpperCase() {
        return toString();
    }
}
"""
        )
        configure(
            """
package p;

import com.example.Text;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{text.toUpper<caret>Case()}")
    void call(Text text) {
    }
}
"""
        )
        val text = documentationAtCaret()

        assertTrue("应使用平台原生定义区: $text", text.contains("'definition'"))
        assertTrue("应展示方法签名: $text", text.contains("toUpperCase"))
        assertTrue("应展示返回类型与所属类: $text", text.contains("com.example.Text"))
        assertTrue("应展示 Javadoc 正文: $text", text.contains("转为大写"))
        // Javadoc 的 `@return` 由原生渲染器输出为 Returns 段落，与 IDEA 内置一致
        assertTrue("应展示 Returns 段落: $text", text.contains("Returns:"))
        assertFalse("不应再出现自制表格字段: $text", text.contains("方法参数"))
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

        assertTrue("应展示字符串重载: $text", text.contains("pick"))
        assertTrue("应展示字符串参数类型: $text", text.contains("String"))
        assertFalse("不应展示整数重载: $text", text.contains("pick(int)"))
    }

    /**
     * 补全列表选中函数时，右侧文档必须展示 Javadoc 正文与参数，
     * 与 IDEA 内置方法提示一致（用户反馈所有函数都没有文档）。
     */
    fun testCompletionLookupDocumentationRendersFunctionJavadoc() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{str_n<caret>}")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        val declarations = myFixture.lookupElements
            ?.mapNotNull { it.`object` as? SpelDeclaration }
            ?: emptyList()
        val strNe = declarations.firstOrNull { it.name == "str_ne" }
        assertNotNull("补全列表缺少 str_ne: ${declarations.map { it.name }}", strNe)

        val documentation = strNe!!.documentation
        assertNotNull("函数候选缺少文档模型", documentation)
        val html = SpelDocumentationRenderer.renderCompletion(documentation!!, strNe.target)
        assertTrue("补全文档缺少 Javadoc 正文: $html", html.contains("字符串不等判断"))
        assertTrue("补全文档缺少参数行: $html", html.contains("参数"))
        assertTrue("补全文档缺少参数名 left: $html", html.contains("left"))
        assertTrue("补全文档缺少返回类型 boolean: $html", html.contains("boolean"))

        // 平台补全右侧文档的实际入口：虚拟声明 → psiTargetProvider
        val targets = SpelPsiDocumentationTargetProvider().documentationTargets(strNe, null)
        assertEquals("补全文档目标缺失", 1, targets.size)
        assertTrue(
            "函数文档必须交给平台原生目标渲染: ${targets[0].javaClass.name}",
            targets[0] is PsiElementDocumentationTarget
        )
        val targetElement = (targets[0] as PsiElementDocumentationTarget).targetElement
        val nativeHtml = DocumentationManager.getProviderFromElement(targetElement)
            ?.generateDoc(targetElement, null)
        assertNotNull("原生文档为空", nativeHtml)
        assertTrue("原生文档缺少 Javadoc 正文: $nativeHtml", nativeHtml!!.contains("字符串不等判断"))
    }

    /** 变量候选的右侧文档同样展示名称与类型，不留空白。 */
    fun testCompletionLookupDocumentationRendersVariableDocumentation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{us<caret>}")
    void call(String userId, String userName) {
    }
}
"""
        )
        myFixture.completeBasic()
        val declarations = myFixture.lookupElements
            ?.mapNotNull { it.`object` as? SpelDeclaration }
            ?: emptyList()
        val parameter = declarations.firstOrNull { it.name == "userId" }
        assertNotNull("补全列表缺少参数 userId: ${declarations.map { it.name }}", parameter)

        val documentation = parameter!!.documentation
        assertNotNull("参数候选缺少文档模型", documentation)
        val html = SpelDocumentationRenderer.renderCompletion(documentation!!, parameter.target)
        assertTrue("变量补全文档缺少名称: $html", html.contains("userId"))
        assertTrue("变量补全文档缺少类型: $html", html.contains("String"))

        val targets = SpelPsiDocumentationTargetProvider().documentationTargets(parameter, null)
        assertEquals("变量补全文档目标缺失", 1, targets.size)
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
        // 真实声明与 IDEA 内置 Quick Doc 走同一平台文档链；
        // 虚拟声明仍由插件自己的渲染器兜底。
        val documentation = if (documentationElement is SpelDeclaration) {
            provider.generateDoc(documentationElement, null)
        } else {
            DocumentationManager.getProviderFromElement(documentationElement!!)
                ?.generateDoc(documentationElement, null)
        }
        assertNotNull("文档为空", documentation)
        return documentation!!
    }
}
