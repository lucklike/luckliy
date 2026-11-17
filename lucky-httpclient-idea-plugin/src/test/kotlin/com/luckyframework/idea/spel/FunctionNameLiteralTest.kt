package com.luckyframework.idea.spel

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.PlatformTestUtil
import com.luckyframework.idea.spel.completion.SpelOrderedElement
import com.luckyframework.idea.spel.documentation.SpelDocumentationProvider
import com.luckyframework.idea.spel.highlight.SpelTextAttributes
import com.luckyframework.idea.spel.inspection.UnresolvedSpelSymbolInspection
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.reference.SpelDeclaration
import com.luckyframework.idea.spel.reference.SpelReference
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * `@SpelAttribute(kind = SpelKind.FUNCTION)` 属性值只写函数名时的解析。
 *
 * 运行期属性值不含 `#{}` 时原样作为函数名，按平铺名从变量表取 `Method`
 * （`Context#autoInjectParamExecuteFunction` → `getVar(name, Method.class)`），
 * 因此高亮、引用/跳转、悬停文档、补全与未解析诊断都必须覆盖这种写法。
 *
 * 只有平铺注册的函数可用：带 `@Namespace` 的类注册成
 * `var[namespace] = Map<name, Method>`，平铺名不存在；
 * 非标识符内容（如 URL）不是函数名，不参与任何解析。
 */
class FunctionNameLiteralTest : SymbolFixtureTestCase() {

    /** 元数据层：FUNCTION 属性的裸函数名被识别，`url` 属性不被误判。 */
    fun testFunctionNameLiteralDetection() {
        configureApi("func = \"getUrl\"")
        val literal = literalOf("getUrl")!!
        val resolver = SpelAnnotationResolver()

        assertTrue(resolver.isFunctionNameAttribute(literal))
        assertTrue(resolver.isFunctionNameLiteral(literal))
        assertEquals("getUrl", resolver.functionNameOf(literal))
    }

    /** 非标识符内容不是函数名：不建引用、不着色，也不进入未解析诊断。 */
    fun testNonIdentifierValueIsIgnored() {
        configureApi("func = \"http://example.com\"")
        val literal = literalOf("http://example.com")!!
        val resolver = SpelAnnotationResolver()

        assertTrue(resolver.isFunctionNameAttribute(literal))
        assertNull(resolver.functionNameOf(literal))
        assertFalse(resolver.isFunctionNameLiteral(literal))
        assertTrue("非法标识符不应建 SpEL 引用", spelReferences(literal).isEmpty())
    }

    /** 普通属性（如 `url`）的值不是函数名：不着色、不建引用。 */
    fun testNonFunctionAttributeIsNotTreatedAsFunctionName() {
        configureApi("url = \"getUrl\"")
        val literal = literalOf("getUrl")!!
        val highlights = myFixture.doHighlighting().filter {
            it.forcedTextAttributesKey?.externalName?.startsWith("LUCKY_SPEL_") == true
        }

        assertFalse(SpelAnnotationResolver().isFunctionNameAttribute(literal))
        assertTrue("普通属性不应有 SpEL 着色: $highlights", highlights.isEmpty())
        assertTrue("普通属性不应建 SpEL 引用", spelReferences(literal).isEmpty())
    }

    /** 函数名整段按函数色着色。 */
    fun testFunctionNameIsHighlighted() {
        configureApi("func = \"getUrl\"")
        val start = myFixture.file.text.indexOf("\"getUrl\"") + 1
        val highlights = myFixture.doHighlighting()

        assertTrue(
            "未找到函数名着色: ${
                highlights.map { "${it.startOffset}-${it.endOffset}:${it.forcedTextAttributesKey?.externalName}" }
            }",
            highlights.any {
                it.forcedTextAttributesKey == SpelTextAttributes.FUNCTION &&
                    it.startOffset == start &&
                    it.endOffset == start + "getUrl".length
            }
        )
    }

    /** 整段内容是一个引用，解析目标指向真实静态方法。 */
    fun testFunctionNameResolvesToStaticMethod() {
        configureApi("func = \"getUrl\"")
        val literal = literalOf("getUrl")!!
        val reference = literal.references.single()

        assertEquals(TextRange(1, 1 + "getUrl".length), reference.rangeInElement)
        val declaration = reference.resolve() as? SpelDeclaration
        assertNotNull("引用未解析", declaration)
        assertEquals("函数", declaration!!.kindLabel)
        assertSame(methodNamed("getUrl"), declaration.target)
    }

    /** 悬停/Quick Doc 命中函数声明，走平台原生文档链。 */
    fun testFunctionNameHoverResolvesToMethod() {
        configureApi("func = \"getUrl\"")
        val offset = myFixture.file.text.indexOf("\"getUrl\"") + 1 + 3
        val element = SpelDocumentationProvider().getCustomDocumentationElement(
            myFixture.editor,
            myFixture.file,
            null,
            offset
        )

        assertSame(methodNamed("getUrl"), element)
    }

    /** 补全只列出平铺注册的函数，带 `@Namespace` 的函数只能 `#ns.name` 调用。 */
    fun testFunctionNameCompletionListsFlatFunctionsOnly() {
        addNamespacedFunctions()
        configure(
            """
package p;

import com.example.NamespacedFunctions;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport({NamespacedFunctions.class})
class Api {
    public static String getUrl() {
        return "";
    }

    @HttpRequest(func = "<caret>")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        val names = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertTrue("平铺函数缺失 getUrl: $names", "getUrl" in names)
        assertTrue("命名空间函数不能按平铺名补全: $names", "nsOnly" !in names)
    }

    /** 重载候选用签名区分列表，插入时只保留裸函数名（不补 `()`）。 */
    fun testFunctionNameCompletionInsertsBareName() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    public static String getUrl() {
        return "";
    }

    public static String getUrl(String base) {
        return base;
    }

    @HttpRequest(func = "getU<caret>")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        val lookup = myFixture.lookup
        if (lookup != null) {
            val item = lookup.items.firstOrNull { it.lookupString == "getUrl(String)" }
            assertNotNull("缺少重载候选: ${lookup.items.map { it.lookupString }}", item)
            lookup.currentItem = item
            myFixture.finishLookup('\n')
        }
        val text = myFixture.file.text

        assertTrue("插入结果必须是裸函数名: $text", text.contains("func = \"getUrl\""))
        assertFalse("函数名属性不能补括号: $text", text.contains("func = \"getUrl("))
    }

    /** 函数名属性内输入标识符字符时自动弹出补全，候选包含平铺函数。 */
    fun testFunctionNameAutoPopupListsFlatFunctions() {
        configureApi("func = \"<caret>\"")
        myFixture.type("g")
        val lookup = awaitSpelLookup()

        assertNotNull("函数名属性内输入标识符后未自动弹出补全", lookup)
        val names = lookup!!.items.map { it.lookupString }
        assertTrue("候选应包含平铺函数 getUrl: $names", "getUrl" in names)
    }

    /** `.` 不是函数名里的有效字符：输入时不自动弹出。 */
    fun testFunctionNameAutoPopupIgnoresDot() {
        configureApi("func = \"<caret>\"")
        myFixture.type(".")
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        val lookup = myFixture.lookup

        assertTrue(
            "函数名属性内输入点号不应弹出 SpEL 补全: ${lookup?.items?.map { it.lookupString }}",
            lookup == null || lookup.items.none { it is SpelOrderedElement }
        )
    }

    /** 非 FUNCTION 属性（如 `url`）的值不是函数名：输入字符不自动弹出。 */
    fun testNonFunctionAttributeDoesNotAutoPopup() {
        configureApi("url = \"<caret>\"")
        myFixture.type("g")
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        val lookup = myFixture.lookup

        assertTrue(
            "普通属性不应弹出 SpEL 补全: ${lookup?.items?.map { it.lookupString }}",
            lookup == null || lookup.items.none { it is SpelOrderedElement }
        )
    }

    /** 不存在的函数名必须报「未解析的 SpEL 函数」。 */
    fun testUnresolvedFunctionNameIsReported() {
        configureApi("func = \"missingFn\"")
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val messages = myFixture.doHighlighting().mapNotNull { it.description }
        assertTrue(
            "未报告未解析函数: $messages",
            messages.any { it.contains("未解析的 SpEL 函数 'missingFn'") }
        )
    }

    /** 已解析的函数名不产生诊断。 */
    fun testResolvedFunctionNameIsNotReported() {
        configureApi("func = \"getUrl\"")
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val messages = myFixture.doHighlighting().mapNotNull { it.description }
        assertTrue(
            "已解析函数不应报告: $messages",
            messages.none { it.contains("未解析的 SpEL 函数") }
        )
    }

    /** 带 `@Namespace` 的函数按平铺名使用时运行期找不到，必须报未解析。 */
    fun testNamespacedFunctionFlatNameIsUnresolved() {
        addNamespacedFunctions()
        configure(
            """
package p;

import com.example.NamespacedFunctions;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport({NamespacedFunctions.class})
class Api {
    @HttpRequest(func = "nsOnly")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val messages = myFixture.doHighlighting().mapNotNull { it.description }
        assertTrue(
            "命名空间函数按平铺名使用时必须报未解析: $messages",
            messages.any { it.contains("未解析的 SpEL 函数 'nsOnly'") }
        )
    }

    /** 属性未填写时不建引用、不报诊断。 */
    fun testEmptyFunctionNameProducesNoReferenceOrDiagnostic() {
        configureApi("func = \"\"")
        val literal = literalOf("")
        assertTrue("空值不应建 SpEL 引用", spelReferences(literal).isEmpty())

        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
        val messages = myFixture.doHighlighting().mapNotNull { it.description }
        assertTrue("空值不应报告: $messages", messages.none { it.contains("未解析的 SpEL 函数") })
    }

    /** 用户报告的最小复现：当前类内的静态函数可被 `func = "..."` 引用。 */
    private fun configureApi(attribute: String) {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    public static String getUrl() {
        return "https://example.com";
    }

    @HttpRequest($attribute)
    void call() {
    }
}
"""
        )
    }

    /**
     * 等自动弹出的补全在后台算出候选。
     *
     * typedHandler 触发的补全不是同步的：只 flush 事件队列时可能还拿不到候选，
     * 需要让出时间片等结果回投，否则会误判「未弹出」。
     */
    private fun awaitSpelLookup(): Lookup? {
        val deadline = System.currentTimeMillis() + 3000
        while (System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            val lookup = myFixture.lookup
            if (lookup != null && lookup.items.any { it is SpelOrderedElement }) {
                return lookup
            }
            Thread.sleep(10)
        }
        return myFixture.lookup
    }

    /** 只关注插件自身创建的引用：平台插件也会为 URL 文本建立自己的引用。 */
    private fun spelReferences(literal: com.intellij.psi.PsiLiteralExpression) =
        literal.references.filterIsInstance<SpelReference>()

    private fun addNamespacedFunctions() {
        myFixture.addFileToProject(
            "com/example/NamespacedFunctions.java",
            """
package com.example;

import com.luckyframework.httpclient.proxy.spel.Namespace;

@Namespace("nsx")
public class NamespacedFunctions {

    public static String nsOnly() {
        return "";
    }
}
"""
        )
    }
}
