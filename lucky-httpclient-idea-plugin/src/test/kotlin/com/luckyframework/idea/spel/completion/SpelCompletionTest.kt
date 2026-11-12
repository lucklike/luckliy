package com.luckyframework.idea.spel.completion

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupEx
import com.intellij.icons.AllIcons
import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider
import com.intellij.testFramework.DumbModeTestUtils
import com.luckyframework.idea.spel.documentation.SpelCompletionDocumentationTarget
import com.luckyframework.idea.spel.documentation.SpelDocumentationRenderer
import com.luckyframework.idea.spel.reference.SpelDeclaration
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase
import javax.swing.Icon

/**
 * 9.1–9.6、9.8：变量、Root、函数、命名空间、类型与成员补全。
 */
class SpelCompletionTest : SymbolFixtureTestCase() {

    fun testNoCompletionWithoutDelimiter() {
        complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "plain<caret>")
    void call(String userId) {
    }
}
"""
        )
        assertTrue(completionElements().isEmpty())
    }

    fun testParameterVariableCompletion() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#<caret>")
    void query(String userId) {
    }
}
"""
        )
        val element = elements.firstOrNull { it.lookupString == "#userId" }
        assertNotNull(element)
        // 默认样式：主文本为变量名，参数类型靠右
        assertEquals("#userId", element!!.itemText())
        assertEquals("String", element.typeText())
    }

    fun testParameterShorthandCompletion() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#${'$'}<caret>")
    void query(String keyword, int page) {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("${'$'}keyword" in strings)
        assertTrue("${'$'}0" in strings)
        assertTrue("${'$'}${'$'}0" in strings)
        assertTrue("${'$'}0::type" in strings)

        val first = elements.first { it.lookupString == "${'$'}0" }
        assertEquals("String", first.typeText())
    }

    /** 参数上下文对象 `$pc$n`/`$pc$name` 与参数值变量一样参与补全。 */
    fun testParameterContextVariableCompletion() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}pc<caret>")
    void query(String keyword, int page) {
    }
}
"""
        )
        val names = spelNames(elements)
        assertTrue("缺少 ${'$'}pc${'$'}0: $names", "${'$'}pc${'$'}0" in names)
        assertTrue("缺少 ${'$'}pc${'$'}keyword: $names", "${'$'}pc${'$'}keyword" in names)
        assertFalse("不应列出无效的裸 ${'$'}pc${'$'}: $names", "${'$'}pc${'$'}" in names)
        assertTypedOrder(elements, "${'$'}pc")
    }

    /** 输入参数名前缀时，包含它的 `$name`/`name::type` 变体同样要保留并参与排序。 */
    fun testTypingParameterNameKeepsShorthandVariants() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{key<caret>")
    void query(String keyword) {
    }
}
"""
        )
        val names = spelNames(elements)
        assertTrue("缺少 keyword: $names", "keyword" in names)
        assertTrue("缺少 ${'$'}keyword: $names", "${'$'}keyword" in names)
        assertTrue("缺少 keyword::type: $names", "keyword::type" in names)
        assertTypedOrder(elements, "key")
    }

    /** 参数名取合并后的 `@Param#value`：`@Query("req")` 提示 `req` 而不是 `request`。 */
    fun testQueryAnnotationParameterCompletionUsesMergedName() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {
    @HttpRequest(url = "#{req<caret>")
    void query(@Query("req") String request) {
    }
}
"""
        )
        val names = spelNames(elements)
        assertTrue("缺少 req: \$names", "req" in names)
        assertTrue("缺少 ${'$'}req: \$names", "${'$'}req" in names)
        assertFalse("不应出现 request: \$names", "request" in names)
    }

    fun testImportedVariableCompletion() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(var = "token=#{1 + 1}")
    @HttpRequest(url = "#{#to<caret>")
    void call() {
    }
}
"""
        )
        val element = elements.firstOrNull { it.lookupString == "#token" }
        assertNotNull(element)
        assertEquals("#token", element!!.itemText())
    }

    /** Root 位置输入 `$` 时列出内置响应变量（`$body$`、`$status$` 等）。 */
    fun testDollarPrefixListsBuiltinResponseVariables() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Retryable;

class Api {
    @Retryable(condition = "#{${'$'}<caret>}")
    void call() {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("未列出 ${'$'}body${'$'}: $strings", "${'$'}body${'$'}" in strings)
        assertTrue("未列出 ${'$'}status${'$'}: $strings", "${'$'}status${'$'}" in strings)
    }

    /** Root 位置输入 `$` 时同样列出位置参数与类型形式（`$0`、`$1`、`$0::type`）。 */
    fun testDollarPrefixInRootListsPositionalParameters() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}<caret>")
    void query(String keyword, int page) {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("未列出 ${'$'}0: $strings", "${'$'}0" in strings)
        assertTrue("未列出 ${'$'}1: $strings", "${'$'}1" in strings)
        assertTrue("未列出 ${'$'}keyword: $strings", "${'$'}keyword" in strings)
        assertTrue("未列出 ${'$'}0::type: $strings", "${'$'}0::type" in strings)
    }

    /** Root 位置的位置参数候选带运行期语义说明（最终值/原始值/类型）。 */
    fun testRootPositionalParameterCompletionShowsDescription() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}<caret>")
    void query(String keyword) {
    }
}
"""
        )
        val finalValue = elements.firstOrNull { it.lookupString == "${'$'}0" }
        assertNotNull("缺少位置参数候选 ${'$'}0", finalValue)
        assertTrue("位置参数缺少说明: ${finalValue!!.tailText()}", finalValue.tailText().contains("最终值"))
        assertEquals("String", finalValue.typeText())

        val rawValue = elements.firstOrNull { it.lookupString == "${'$'}${'$'}0" }
        assertNotNull("缺少原始参数候选 ${'$'}${'$'}0", rawValue)
        assertTrue("原始参数缺少说明: ${rawValue!!.tailText()}", rawValue.tailText().contains("原始值"))

        val type = elements.firstOrNull { it.lookupString == "${'$'}0::type" }
        assertNotNull("缺少类型候选 ${'$'}0::type", type)
        assertTrue("类型参数缺少说明: ${type!!.tailText()}", type.tailText().contains("类型"))
        assertEquals("ResolvableType", type.typeText())
    }

    /** 候选分层：方法参数排在框架内部变量之前（就近作用域优先）。 */
    fun testCompletionOrderPutsParametersBeforeInternalVariables() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{<caret>")
    void query(String keyword) {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        val parameter = strings.indexOf("keyword")
        val internal = strings.indexOf("_index_")
        assertTrue("未列出参数 keyword: $strings", parameter >= 0)
        assertTrue("未列出内部变量 _index_: $strings", internal >= 0)
        assertTrue("参数应排在内部变量之前: $strings", parameter < internal)
    }

    /** 输入为空（`#{` 刚弹出）时内部变量按 @Value 求值语义排序：索引 → 名称 → 类型 → 值 → 来源 → 容器。 */
    fun testInternalVariablesKeepValueContextOrder() {
        val expected = listOf(
            "_index_",
            "_name_",
            "_type_",
            "_value_",
            "_\$value\$_",
            "__\$Val\$__"
        )
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{<caret>")
    void query(String keyword) {
    }
}
"""
        )
        val actual = elements.map { it.lookupString }.filter { it in expected }
        assertEquals(expected, actual)
    }

    /** 候选分层：`@SpELImport` 导入的变量排在内置变量之前。 */
    fun testCompletionOrderPutsImportedBeforeBuiltin() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "alpha=1")
class Api {
    @HttpRequest(url = "#{<caret>")
    void call() {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        val imported = strings.indexOf("alpha")
        val builtin = strings.indexOf("${'$'}mc${'$'}")
        assertTrue("未列出导入变量 alpha: $strings", imported >= 0)
        assertTrue("未列出内置变量 ${'$'}mc${'$'}: $strings", builtin >= 0)
        assertTrue("导入变量应排在内置变量之前: $strings", imported < builtin)
    }

    /** `$0::type` 的类型是 ResolvableType，成员补全按它解析而非参数声明类型。 */
    fun testParameterTypeReferenceMembersResolveToResolvableType() {
        myFixture.addFileToProject(
            "org/springframework/core/ResolvableType.java",
            """
package org.springframework.core;

public class ResolvableType {
    public Class<?> resolve() { return null; }
}
"""
        )
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}0::type.<caret>")
    void call(String userId) {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("未按 ResolvableType 解析成员: $strings", "resolve" in strings)
    }

    /** `#{` 初始候选不列参数简化名，避免列表被 `$name`/`::type` 变体淹没。 */
    fun testRootInitialCandidatesSkipParameterShorthands() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{<caret>")
    void query(String keyword, int page) {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("初始候选不应包含 ${'$'}0: $strings", "${'$'}0" !in strings)
        assertTrue("初始候选应包含参数原名 keyword: $strings", "keyword" in strings)
    }

    /** Root 位置选中位置参数后不补 `#`。 */
    fun testRootPositionalParameterInsertsWithoutHash() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}<caret>")
    void query(String keyword) {
    }
}
"""
        )
        myFixture.completeBasic()
        if (myFixture.lookup != null) {
            select("${'$'}0")
        }
        assertTrue(myFixture.file.text.contains("#{" + "${'$'}0"))
    }

    /** `#{` 的初始候选包含响应变量，输入 `$` 后由 lookup 前缀过滤出来。 */
    fun testRootCandidatesIncludeResponseVariables() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Retryable;

class Api {
    @Retryable(condition = "#{<caret>}")
    void call() {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue(
            "Root 候选未包含 ${'$'}body${'$'}: $strings",
            "${'$'}body${'$'}" in strings
        )
    }

    fun testRootVariableInsertionWithoutHash() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(rootLit = "user=admin")
class Api {
    @HttpRequest(url = "#{u<caret>")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        if (myFixture.lookup != null) {
            select("user")
        }
        assertTrue(myFixture.file.text.contains("#{user"))
    }

    /** 变量没有右侧文档区，说明直接跟在名称后，类型仍然靠右。 */
    fun testBuiltinVariableCompletionShowsDescriptionAndType() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Retryable;

class Api {
    @Retryable(condition = "#{${'$'}<caret>}")
    void call() {
    }
}
"""
        )
        val body = elements.firstOrNull { it.lookupString == "${'$'}body${'$'}" }
        assertNotNull(body)
        assertEquals("${'$'}body${'$'}", body!!.itemText())
        assertEquals("Object", body.typeText())
        assertTrue("内置变量缺少说明: ${body.tailText()}", body.tailText().contains("响应体"))
        assertEquals(AllIcons.Nodes.Variable, body.icon())
    }

    /** 变量与函数在补全列表中用不同图标区分，与 IDEA 自带提示一致。 */
    fun testVariableAndFunctionUseDifferentIcons() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#<caret>")
    void call(String userId) {
    }
}
"""
        )
        val variable = elements.firstOrNull { it.lookupString == "#userId" }
        val function = elements.firstOrNull { it.lookupString == "#time" }
        assertNotNull("缺少变量候选", variable)
        assertNotNull("缺少函数候选", function)
        assertEquals(AllIcons.Nodes.Parameter, variable!!.icon())
        assertEquals(AllIcons.Nodes.Function, function!!.icon())
        assertNotSame(variable.icon(), function.icon())
    }

    fun testBuiltinFunctionCompletion() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#ti<caret>")
    void call() {
    }
}
"""
        )
        val time = elements.firstOrNull { it.lookupString == "#time" }
        assertNotNull(time)
        // 默认方法提示样式：主文本为签名，返回类型靠右
        assertEquals("time()", time!!.itemText())
        assertEquals("long", time.typeText())
    }

    /** 补全预览需要展示内置函数的描述。 */
    fun testBuiltinFunctionCompletionPreviewShowsDescription() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#ti<caret>")
    void call() {
    }
}
"""
        )
        val time = elements.firstOrNull { it.lookupString == "#time" }
        assertNotNull(time)
        val html = completionPreview(time!!)
        assertTrue("内置函数预览缺少描述: $html", html.contains("获取当前时间毫秒"))
    }

    /** 补全预览按默认方法提示样式展示所属类、签名与返回值。 */
    fun testBuiltinFunctionPreviewUsesDefaultMethodStyle() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#ti<caret>")
    void call() {
    }
}
"""
        )
        val time = elements.firstOrNull { it.lookupString == "#time" }
        assertNotNull(time)
        val html = completionPreview(time!!)
        assertTrue("预览缺少所属类: $html", html.contains("CommonFunctions"))
        assertTrue("预览缺少签名: $html", html.contains("time()"))
        assertTrue("预览缺少返回值: $html", html.contains("long"))
    }

    /** 无框架源码时，参数按签名中的类型降级展示。 */
    fun testFunctionPreviewListsParameterTypes() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#format_<caret>")
    void call() {
    }
}
"""
        )
        val element = elements.firstOrNull { it.lookupString == "#format_date" }
        assertNotNull(element)
        val html = completionPreview(element!!)
        assertTrue("预览缺少参数行: $html", html.contains("参数"))
        assertTrue("缺少参数类型 Date: $html", html.contains("Date"))
        assertTrue("缺少参数类型 String: $html", html.contains("String"))
    }

    /** 补全选中项要能通过平台链路拿到默认样式文档目标（右侧文档区的入口）。 */
    fun testCompletionLookupProducesDocumentationTarget() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#ti<caret>")
    void call() {
    }
}
"""
        )
        val time = elements.firstOrNull { it.lookupString == "#time" }
        assertNotNull(time)
        val targets = IdeDocumentationTargetProvider.getInstance(project)
            .documentationTargets(myFixture.editor, myFixture.file, time!!)
        assertTrue(
            "平台未产出补全文档目标: $targets",
            targets.any { it is SpelCompletionDocumentationTarget }
        )
    }

    /**
     * 补全副本中的虚拟声明必须归属原始文件。
     *
     * 2026.3 取补全元素的文档目标时会先调 `CompletionUtil.getTargetElement`，
     * 它用 `getOriginalElement` 把副本内元素映射回原文件：锚点文件为副本时
     * 会在原文件里按声明类型找同偏移元素，找不到就返回 null，右侧文档区因
     * 此一片空白。归属原文件后该映射原样返回声明自身。
     */
    fun testCompletionDeclarationResolvesToItselfAsOriginalElement() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#ti<caret>")
    void call() {
    }
}
"""
        )
        val time = elements.firstOrNull { it.lookupString == "#time" }
        assertNotNull(time)
        val declaration = time!!.psiElement as? SpelDeclaration
        assertNotNull("函数候选缺少虚拟声明", declaration)
        assertSame("虚拟声明应归属原始文件", myFixture.file, declaration!!.containingFile)
        assertSame(
            "原始元素解析应返回虚拟声明自身",
            declaration,
            CompletionUtil.getTargetElement(time)
        )
        assertTrue(
            "声明偏移 ${declaration.textOffset} 超出原始文件长度 ${myFixture.file.textLength}",
            declaration.textOffset in 0..myFixture.file.textLength
        )
        // 平台文档兜底会用 ensureValid 创建 SmartPointer，父元素为空会抛 PsiInvalidElementAccessException
        assertNotNull("虚拟声明必须提供父元素", declaration.parent)
        assertTrue("虚拟声明必须保持有效状态", declaration.isValid)
    }

    /** 成员方法按 `name(参数类型 参数名)` 展示，返回类型靠右，预览带参数说明。 */
    fun testMemberMethodCompletionUsesDefaultMethodStyle() {
        myFixture.addFileToProject(
            "com/example/Greeter.java",
            """
package com.example;

public class Greeter {
    /**
     * 打招呼
     *
     * @param name  姓名
     * @param times 次数
     * @return 问候语
     */
    public String greet(String name, int times) {
        return "";
    }
}
"""
        )
        val elements = complete(
            """
package p;

import com.example.Greeter;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#greeter.<caret>}")
    void call(Greeter greeter) {
    }
}
"""
        )
        val greet = elements.firstOrNull { it.lookupString == "greet" }
        assertNotNull(greet)
        assertEquals("greet(String name, int times)", greet!!.itemText())
        assertEquals("String", greet.typeText())

        val html = completionPreview(greet)
        assertTrue("预览缺少参数名: $html", html.contains("name"))
        assertTrue("预览缺少参数描述: $html", html.contains("姓名"))
        assertTrue("预览缺少参数描述: $html", html.contains("次数"))
        assertTrue("预览缺少返回值描述: $html", html.contains("问候语"))
    }

    fun testOverloadedFunctionsAreListed() {
        myFixture.addFileToProject(
            "com/example/Overloads.java",
            """
package com.example;

public class Overloads {
    public static String pick(String value) { return value; }
    public static String pick(int value) { return ""; }
}
"""
        )
        val elements = complete(
            """
package p;

import com.example.Overloads;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(Overloads.class)
    @HttpRequest(url = "#{#pic<caret>")
    void call() {
    }
}
"""
        )
        val pick = elements.filter { it.lookupString.startsWith("#pick") }
        assertEquals(2, pick.size)
        assertTrue(pick.any { it.lookupString == "#pick(String)" })
        assertTrue(pick.any { it.lookupString == "#pick(int)" })
    }

    fun testNamespaceFunctionCompletion() {
        val elements = complete(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{#crypto.<caret>")
    void call() {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("md5" in strings)
        assertTrue("sha" in strings)
    }

    fun testNamespaceFunctionAlsoCompletesFlat() {
        val elements = complete(
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
        assertTrue(
            "平铺函数补全缺失: ${elements.map { it.lookupString }}",
            elements.any { it.lookupString == "#md5(String)" }
        )
    }

    fun testTypeCompletionFromImportedPackage() {
        myFixture.addFileToProject(
            "java/util/ArrayList.java",
            "package java.util;\npublic class ArrayList<E> {}\n"
        )
        myFixture.addFileToProject(
            "java/util/ArrayDeque.java",
            "package java.util;\npublic class ArrayDeque<E> {}\n"
        )
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(pack = "java.util")
    @HttpRequest(url = "#{T(Array<caret>")
    void call() {
    }
}
"""
        )
        val strings = elements.map { it.itemText() }
        assertTrue("ArrayList" in strings)
        assertTrue("ArrayDeque" in strings)
        val element = elements.first { it.itemText() == "ArrayList" }
        assertEquals("java.util.ArrayList", element.typeText())
        assertEquals("java.util.ArrayList", element.lookupString)
    }

    /** `T(` 类型补全：同名类全部列出，框架类不能被无关依赖的 Request 挤掉。 */
    fun testTypeCompletionListsAllSameNameClasses() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/core/meta/Request.java",
            "package com.luckyframework.httpclient.core.meta;\npublic interface Request {}\n"
        )
        myFixture.addFileToProject(
            "example/a/Request.java",
            "package example.a;\npublic class Request {}\n"
        )
        myFixture.addFileToProject(
            "example/b/Request.java",
            "package example.b;\npublic class Request {}\n"
        )
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(Requ<caret>")
    void call() {
    }
}
"""
        )
        val types = elements.filter { it.itemText() == "Request" }.map { it.typeText() }
        assertTrue(
            "应列出框架 Request: $types",
            "com.luckyframework.httpclient.core.meta.Request" in types
        )
        assertTrue("应列出 example.a.Request: $types", "example.a.Request" in types)
        assertTrue("应列出 example.b.Request: $types", "example.b.Request" in types)
    }

    /**
     * 短名缓存按哈希序遍历，共用词干的类型很容易占满候选上限；
     * 精确同名的框架类必须优先入列表，不能被 RequestXxx 淹没。
     */
    fun testTypeCompletionKeepsExactNameWhenManySimilarTypes() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/core/meta/Request.java",
            "package com.luckyframework.httpclient.core.meta;\npublic interface Request {}\n"
        )
        val body = StringBuilder("package example.many;\n")
        repeat(205) { body.append("class RequestCandidate").append(it).append(" {}\n") }
        myFixture.addFileToProject("example/many/ManyRequests.java", body.toString())

        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(Request<caret>")
    void call() {
    }
}
"""
        )
        val types = elements.filter { it.itemText() == "Request" }.map { it.typeText() }
        assertTrue(
            "应列出框架 Request: $types",
            "com.luckyframework.httpclient.core.meta.Request" in types
        )
    }

    /** 非 java.lang 的类型插入时必须带包名。 */
    fun testTypeCompletionInsertsQualifiedName() {
        myFixture.addFileToProject(
            "java/util/ArrayList.java",
            "package java.util;\npublic class ArrayList<E> {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(Array<caret>)}")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        // 单候选时平台直接插入，不会打开 lookup
        if (myFixture.lookup != null) {
            select("java.util.ArrayList")
        }

        assertTrue(
            "非 java.lang 类型应插入全限定名: ${myFixture.file.text}",
            myFixture.file.text.contains("T(java.util.ArrayList)")
        )
    }

    /** java.lang 包内的类型按 SpEL 规则仍可用短名。 */
    fun testTypeCompletionKeepsJavaLangShortName() {
        myFixture.addFileToProject(
            "java/lang/String.java",
            "package java.lang;\npublic final class String {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(Str<caret>)}")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        // 单候选时平台直接插入，不会打开 lookup
        if (myFixture.lookup != null) {
            select("String")
        }

        assertTrue(
            "java.lang 类型应保留短名: ${myFixture.file.text}",
            myFixture.file.text.contains("T(String)")
        )
        assertFalse(
            "java.lang 类型不应插入包名: ${myFixture.file.text}",
            myFixture.file.text.contains("T(java.lang.String)")
        )
    }

    fun testKnownTypeMembers() {
        myFixture.addFileToProject(
            "com/example/User.java",
            """
package com.example;

public class User {
    public String name;
    public String getName() { return name; }
}
"""
        )
        val elements = complete(
            """
package p;

import com.example.User;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#user.<caret>")
    void call(User user) {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("name" in strings)
        assertTrue("getName" in strings)
    }

    /** 函数参数内的成员补全按参数类型解析，而不是退化为 Object 方法。 */
    fun testMemberCompletionInsideFunctionArgument() {
        myFixture.addFileToProject(
            "com/example/ImageReq.java",
            """
package com.example;

public class ImageReq {
    public String imageUrl;
}
"""
        )
        val elements = complete(
            """
package p;

import com.example.ImageReq;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(request.<caret>, 4)}")
    void call(ImageReq request) {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("应按参数类型补全字段: $strings", "imageUrl" in strings)
        val unknown = elements.filter { it.typeText() == "类型未知" }
        assertTrue("不应退化为类型未知成员: ${unknown.map { it.lookupString }}", unknown.isEmpty())
    }

    /** 函数返回值作为接收者时按返回类型补全成员，参数中的方法链不影响整体解析。 */
    fun testFunctionReturnTypeMembers() {
        addRequestStub()
        myFixture.addFileToProject(
            "com/example/Article.java",
            """
package com.example;

public class Article {
    public String getTitle() {
        return "";
    }
}
"""
        )
        val elements = complete(
            """
package p;

import com.example.Article;
import com.luckyframework.httpclient.core.meta.Text;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(${'$'}req${'$'}.getBaseUrl(), 4).<caret>}")
    void call() {
    }

    public static Article add(Text url, Integer count) {
        return null;
    }
}
"""
        )
        val names = elements.map { it.lookupString }
        assertTrue("未按函数返回类型补全 Article 成员: $names", "title" in names)
        assertTrue("未按函数返回类型补全 Article 成员: $names", "getTitle" in names)
        val unknown = elements.filter { it.typeText() == "类型未知" }
        assertTrue("不应退化为类型未知成员: ${unknown.map { it.lookupString }}", unknown.isEmpty())
    }

    /** 方法链结果作为接收者时按返回值类型补全成员。 */
    fun testMethodChainReturnTypeMembers() {
        addRequestStub()
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}req${'$'}.getBaseUrl().<caret>}")
    void call() {
    }
}
"""
        )
        val names = elements.map { it.lookupString }
        assertTrue("未按方法返回类型补全 Text 成员: $names", "host" in names)
    }

    private fun addRequestStub() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/core/meta/Text.java",
            """
package com.luckyframework.httpclient.core.meta;

public class Text {
    public String getHost() {
        return "";
    }
}
"""
        )
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/core/meta/Request.java",
            """
package com.luckyframework.httpclient.core.meta;

public interface Request {
    Text getBaseUrl();
}
"""
        )
    }

    /** 类型成员补全：主文本为成员名，类型靠右。 */
    fun testMemberCompletionShowsTypes() {
        myFixture.addFileToProject(
            "com/example/DocUser.java",
            """
package com.example;

public class DocUser {
    /** 用户的唯一标识 */
    public String id;

    /**
     * 获取展示名称
     *
     * @return 名称
     */
    public String getDisplayName() { return ""; }
}
"""
        )
        val elements = complete(
            """
package p;

import com.example.DocUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#user.<caret>}")
    void call(DocUser user) {
    }
}
"""
        )
        val id = elements.firstOrNull { it.lookupString == "id" }
        assertNotNull(id)
        assertEquals("String", id!!.typeText())
        val displayName = elements.firstOrNull { it.lookupString == "displayName" }
        assertNotNull(displayName)
        assertEquals("String", displayName!!.typeText())
    }

    /** 字段说明在列表中展示，过长时截断为省略号。 */
    fun testMemberListShowsTruncatedDescription() {
        val longText = "很长的字段说明" + "a".repeat(100)
        myFixture.addFileToProject(
            "com/example/LongDoc.java",
            "package com.example;\n\npublic class LongDoc {\n    /** $longText */\n    public String value;\n}\n"
        )
        val elements = complete(
            """
package p;

import com.example.LongDoc;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#doc.<caret>}")
    void call(LongDoc doc) {
    }
}
"""
        )
        val value = elements.firstOrNull { it.lookupString == "value" }
        assertNotNull(value)
        val tail = value!!.tailText()
        assertTrue("字段说明未展示: $tail", tail.contains("很长的字段说明"))
        assertTrue("过长说明未以省略号结尾: $tail", tail.contains("..."))
        assertFalse("过长说明未被截断: $tail", tail.contains(longText))
        assertEquals("String", value.typeText())
    }

    /** 跨文件成员的声明偏移必须落在当前文件内：否则补全 ML 排序器取行号会越界报错。 */
    fun testCrossFileMemberDeclarationOffsetStaysInCurrentFile() {
        myFixture.addFileToProject(
            "com/example/Far.java",
            "package com.example;\n\npublic class Far {\n" +
                "    // filler\n".repeat(200) +
                "    public String greet() { return \"\"; }\n}\n"
        )
        val elements = complete(
            """
package p;

import com.example.Far;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#far.<caret>}")
    void call(Far far) {
    }
}
"""
        )
        val greet = elements.firstOrNull { it.lookupString == "greet" }
        assertNotNull(greet)
        val declaration = greet!!.psiElement
        assertNotNull("成员方法候选缺少虚拟声明", declaration)
        val offset = declaration!!.textOffset
        assertTrue(
            "声明偏移 $offset 超出当前文件长度 ${myFixture.file.textLength}",
            offset in 0..myFixture.file.textLength
        )
    }

    fun testUnknownTypeFallsBackToGenericMembers() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(var = "payload=1")
    @HttpRequest(url = "#{#payload.<caret>")
    void call() {
    }
}
"""
        )
        val toString = elements.firstOrNull { it.lookupString == "toString" }
        assertNotNull(toString)
        assertEquals("类型未知", toString!!.typeText())
    }

    fun testDumbModeReturnsNoCompletion() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#<caret>")
    void call(String userId) {
    }
}
"""
        )
        val literal = com.intellij.psi.util.PsiTreeUtil
            .findChildrenOfType(myFixture.file, com.intellij.psi.PsiLiteralExpression::class.java)
            .first { it.text.contains("#{") }
        val result = DumbModeTestUtils.computeInDumbModeSynchronously(project) {
            com.intellij.openapi.project.DumbService.isDumb(project) to
                SpelCompletionProvider().isApplicable(literal)
        }
        assertTrue("未进入 dumb mode", result.first)
        assertFalse("dumb mode 下补全应关闭", result.second)
    }

    /** 补全副本中的元素找不到模块，必须按原始文件判定激活。 */
    fun testCopyActivationUsesOriginalFile() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#<caret>")
    void call(String userId) {
    }
}
"""
        )
        val literal = com.intellij.psi.util.PsiTreeUtil
            .findChildrenOfType(myFixture.file, com.intellij.psi.PsiLiteralExpression::class.java)
            .first { it.text.contains("#{") }

        assertTrue(
            "副本补全应按原始文件判定为已激活",
            SpelCompletionProvider().isApplicable(literal, myFixture.file)
        )
    }

    /** 已闭合的 `"#{meta.}"` 中补全成员；Lombok 风格的私有字段 + getter 也要能列出。 */
    fun testMemberCompletionInsideClosedExpression() {
        myFixture.addFileToProject(
            "com/example/ChunkMeta.java",
            """
package com.example;

public class ChunkMeta {
    private String fileId;

    public String getFileId() {
        return fileId;
    }
}
"""
        )
        val elements = complete(
            """
package p;

import com.example.ChunkMeta;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{meta.<caret>}")
    void call(ChunkMeta meta) {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("成员补全缺少 fileId: $strings", "fileId" in strings)
    }

    /** 类型明确的内置变量（`$req$`）在 `.` 之后按框架类型列出成员，同名依赖类不干扰。 */
    fun testBuiltinVariableMembersResolveToFrameworkType() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/core/meta/Request.java",
            """
package com.luckyframework.httpclient.core.meta;

public interface Request {
    String VERSION = "1.1";

    String getUrl();

    String getBaseUrl();
}
"""
        )
        myFixture.addFileToProject(
            "okhttp3/Request.java",
            """
package okhttp3;

public class Request {
    public String getTags${'$'}okhttp() {
        return "";
    }
}
"""
        )
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}req${'$'}.<caret>}")
    void call() {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("内置变量成员缺少 getUrl: $strings", "getUrl" in strings)
        assertTrue("内置变量成员缺少 getBaseUrl: $strings", "getBaseUrl" in strings)
        assertTrue("内置变量成员缺少属性 baseUrl: $strings", "baseUrl" in strings)
        assertTrue("内置变量成员缺少常量 VERSION: $strings", "VERSION" in strings)
        assertFalse("不应解析到 okhttp3.Request: $strings", "getTags${'$'}okhttp" in strings)
    }

    /** 上下文类内置变量（`$mc$`）同样按框架类型补全成员。 */
    fun testBuiltinContextVariableMembers() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/proxy/context/MethodContext.java",
            """
package com.luckyframework.httpclient.proxy.context;

public class MethodContext {
    public String getSimpleSignature() {
        return "";
    }

    public String getWrapper() {
        return "";
    }
}
"""
        )
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}mc${'$'}.<caret>}")
    void call() {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("内置变量成员缺少 getSimpleSignature: $strings", "getSimpleSignature" in strings)
        assertTrue("内置变量成员缺少 getWrapper: $strings", "getWrapper" in strings)
    }

    /** `#{` 后的 Root 候选要包含方法参数裸名（`#{meta.fileId}` 的用法）。 */
    fun testRootCompletionListsParameters() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{<caret>}")
    void call(String meta, int page) {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("Root 候选缺少参数 meta: $strings", "meta" in strings)
        assertTrue("Root 候选缺少参数 page: $strings", "page" in strings)
        assertFalse("Root 候选不应包含 ${'$'} 简写: $strings", "${'$'}meta" in strings)
    }

    /** `@SpELImport(var = ...)` 的普通变量在 Root 位置也要补全，插入时补 `#`。 */
    fun testRootCompletionIncludesImportedVariable() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "time=123")
class Api {
    @HttpRequest(url = "#{ti<caret>}")
    void call() {
    }
}
"""
        )
        // 唯一候选时平台会自动插入，lookup 为空但文档已补上 #
        assertTrue(
            "Root 候选缺少 time: ${elements.map { it.lookupString }}",
            elements.any { it.lookupString == "time" } ||
                myFixture.file.text.contains("#{#time")
        )
    }

    /** Root 位置（无 `#`）也补全内置函数：`#{str_ne(...)}` 的平铺用法。 */
    fun testRootCompletionListsBuiltinFunctions() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{str_ne<caret>}")
    void call() {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        // 唯一候选时平台自动插入（文档变为 `str_ne()`）
        assertTrue(
            "Root 候选缺少 str_ne: $strings / ${myFixture.file.text}",
            strings.any { it.startsWith("str_ne") } ||
                myFixture.file.text.contains("str_ne()")
        )
    }

    /** `#{str` 这类多候选场景应同时列出 str/str_eq/str_ne 等内置函数。 */
    fun testRootCompletionListsMultipleBuiltinFunctions() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{str<caret>}")
    void call() {
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("Root 候选缺少 str: $strings", strings.any { it.startsWith("str") })
        assertTrue("Root 候选缺少 str_eq: $strings", strings.any { it.startsWith("str_eq") })
        assertTrue("Root 候选缺少 str_ne: $strings", strings.any { it.startsWith("str_ne") })
    }

    /** 未注册的内置函数（BankCardGenerator）需 `@SpELImport`，补全不列。 */
    fun testCompletionSkipsUnregisteredBuiltinFunctions() {
        val rootElements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{getByCard<caret>}")
    void call() {
    }
}
"""
        )
        val rootStrings = rootElements.map { it.lookupString }
        assertTrue(
            "未注册函数不应参与 Root 补全: $rootStrings",
            rootStrings.none { it.startsWith("getByCardNumber") } &&
                !myFixture.file.text.contains("getByCardNumber")
        )

        val hashElements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#getByCard<caret>}")
    void call() {
    }
}
"""
        )
        val hashStrings = hashElements.map { it.lookupString }
        assertTrue(
            "未注册函数不应参与 # 补全: $hashStrings",
            hashStrings.none { it.startsWith("#getByCardNumber") } &&
                !myFixture.file.text.contains("getByCardNumber")
        )
    }

    /** 当前类与父类声明的 public static 方法也是 SpEL 函数，Root 位置平铺补全。 */
    fun testClassStaticMethodsListedInRootCompletion() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api extends Base {
    @HttpRequest(url = "#{localHel<caret>}")
    void call() {
    }

    public static String localHelperA() {
        return "";
    }

    public static String localHelperB() {
        return "";
    }
}

class Base {
    public static String localHelperFromBase() {
        return "";
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("Root 候选缺少当前类静态方法: $strings", "localHelperA" in strings)
        val element = elements.first { it.lookupString == "localHelperA" }
        assertEquals("localHelperA()", element.itemText())
        val declaration = element.psiElement as? SpelDeclaration
        assertNotNull(declaration)
        assertEquals(
            "当前类静态方法的来源信息丢失",
            "当前类及其父类、父接口的静态方法",
            declaration!!.documentation?.scope
        )
        assertTrue(
            "Root 候选缺少父类静态方法: $strings",
            "localHelperFromBase" in strings
        )
    }

    /** `#` 前缀同样列出当前类静态方法，且 `@FunctionFilter` 方法不参与。 */
    fun testClassStaticMethodsListedAfterHash() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;

class Api {
    @HttpRequest(url = "#{#localHel<caret>}")
    void call() {
    }

    public static String localHelperA() {
        return "";
    }

    public static String localHelperB() {
        return "";
    }

    @FunctionFilter
    public static String hiddenHelper() {
        return "";
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        assertTrue("`#` 候选缺少当前类静态方法: $strings", "#localHelperA" in strings)
        assertTrue(
            "@FunctionFilter 方法不应参与补全: $strings",
            strings.none { it.startsWith("#hiddenHelper") }
        )
    }

    /** 当前类静态方法的 `@FunctionAlias` 别名生效。 */
    fun testClassStaticMethodAliasCompletion() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

class Api {
    @HttpRequest(url = "#{chunk<caret>}")
    void call() {
    }

    @FunctionAlias("chunk_success")
    public static boolean success() {
        return true;
    }
}
"""
        )
        val strings = elements.map { it.lookupString }
        // 唯一候选时平台自动插入，文档变为 `chunk_success()`
        assertTrue(
            "缺少别名函数 chunk_success: $strings / ${myFixture.file.text}",
            "chunk_success" in strings || myFixture.file.text.contains("chunk_success()")
        )
        assertTrue("别名函数的原名不应出现: $strings", "success" !in strings)
    }

    /** `@FunctionAlias` 函数以可输入别名展示，过滤与排序都按列表里看到的名字进行。 */
    fun testClassStaticAliasIsListedByCallableName() {
        val elements = complete(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

class Api {
    @HttpRequest(url = "#{_<caret>}")
    void call() {
    }

    @FunctionAlias("__std_exception_handler__")
    public static void exceptionHandler() {
    }
}
"""
        )
        val names = spelNames(elements)
        assertTrue("别名函数应按别名列出: $names", "__std_exception_handler__" in names)
        assertTrue("原方法名不应出现在列表里: $names", names.none { it == "exceptionHandler" })
        // 别名以 `_` 开头，应归入前缀命中组，整体仍符合输入后的排序规则
        assertTypedOrder(elements, "_")
    }

    /** 键入 `#{` 后 typedHandler 应自动弹出补全，不依赖 Ctrl+Space。 */
    fun testTypingHashBraceOpensLookup() {
        myFixture.configureByText(
            "Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#<caret>")
    void call(String meta) {
    }
}
"""
        )
        myFixture.type("{")
        com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        assertNotNull("输入 #{ 后未自动弹出补全", myFixture.lookup)
    }

    /** 输入 `#{` 弹出后再输入 `ti`，候选应收窄到 `time`（用户实际操作的第二种路径）。 */
    fun testTypingPrefixAfterDelimiterFiltersImportedVariable() {
        myFixture.configureByText(
            "Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "time=123")
class Api {
    @HttpRequest(url = "#<caret>")
    void call() {
    }
}
"""
        )
        myFixture.type("{")
        com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        assertNotNull("输入 #{ 后未自动弹出补全", myFixture.lookup)

        myFixture.type("ti")
        val filtered = myFixture.lookupElements?.toList()?.map { it.lookupString } ?: emptyList()
        assertTrue(
            "输入 ti 后未过滤出 time: $filtered / ${myFixture.file.text}",
            "time" in filtered || myFixture.file.text.contains("#time")
        )
    }

    /** 输入 `_` 后按规则排序：`_` 开头的在前，仅包含 `_` 的在后，组内字母序。 */
    fun testTypingUnderscoreFiltersToUnderscoreCandidates() {
        myFixture.configureByText(
            "Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#<caret>")
    void call() {
    }
}
"""
        )
        myFixture.type("{")
        com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        assertNotNull("输入 #{ 后未自动弹出补全", myFixture.lookup)

        myFixture.type("_")
        repeat(5) {
            com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        }
        val elements = myFixture.lookupElements?.toList() ?: emptyList()
        val names = spelNames(elements)
        assertTrue("未列出内部变量 _index_: $names", "_index_" in names)
        assertTrue("未列出内置函数 _url: $names", "_url" in names)
        assertTrue("仅包含下划线的函数应进入包含组: $names", "str_eq" in names)
        assertTypedOrder(elements, "_")
        // 仅包含下划线的内置函数归入第二组，应排在所有前缀命中项之后
        val lastPrefixed = names.indexOfLast { it.startsWith("_") }
        val contained = names.indexOfFirst { it == "str_eq" }
        assertTrue(
            "仅包含下划线的函数应排在下划线开头候选之后: $names",
            contained < 0 || contained > lastPrefixed
        )
    }

    /** 连续输入多个字符后按最新单词重新分组：`_b` 开头组在前，仅包含组在后，组内字母序。 */
    fun testTypingMultipleCharsResortsByLatestWord() {
        myFixture.configureByText(
            "Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#<caret>")
    void call() {
    }
}
"""
        )
        myFixture.type("{")
        com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        assertNotNull("输入 #{ 后未自动弹出补全", myFixture.lookup)

        myFixture.type("_")
        repeat(5) {
            com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        }
        myFixture.type("b")
        repeat(5) {
            com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        }
        val elements = myFixture.lookupElements?.toList() ?: emptyList()
        val names = spelNames(elements)
        assertTrue("未列出 _base64: $names", "_base64" in names)
        assertTypedOrder(elements, "_b")
    }

    /** 输入 `$` 后按规则排序：`$` 开头的在前，仅包含 `$` 的在后，组内字母序。 */
    fun testTypingDollarFiltersToDollarCandidates() {
        myFixture.configureByText(
            "Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#<caret>")
    void query(String keyword) {
    }
}
"""
        )
        myFixture.type("{")
        com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        assertNotNull("输入 #{ 后未自动弹出补全", myFixture.lookup)

        myFixture.type("${'$'}")
        repeat(5) {
            com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        }
        val elements = myFixture.lookupElements?.toList() ?: emptyList()
        val names = spelNames(elements)
        assertTrue("未列出位置参数 ${'$'}0: $names", "${'$'}0" in names)
        assertTrue("未列出参数简写 ${'$'}keyword: $names", "${'$'}keyword" in names)
        assertTrue("不应列出不含美元符的候选: $names", "str_eq" !in names)
        assertTrue(
            "不含美元符的候选不应出现在列表里: $names",
            names.none { it == "random_int" || it == "join_url" }
        )
        assertTypedOrder(elements, "${'$'}")
    }

    /**
     * 列表弹出后输入 `,` 只输入分隔符，不应接受高亮候选。
     *
     * 平台的默认过滤器把 `,` 固定当成「接受选中项」：`#{fn(a[0` 输入 `0`
     * 弹出 `$$0` 等简写候选并高亮，继续输入逗号时高亮候选会被直接插入。
     */
    fun testTypingCommaKeepsTerminatorInsteadOfAcceptingCandidate() {
        myFixture.configureByText(
            "Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{fn(a[<caret>])")
    void call(String keyword) {
    }
}
"""
        )
        val settings = com.intellij.codeInsight.CodeInsightSettings.getInstance()
        val originalSelection = settings.SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS
        // 复现用户设置：「按空格、点等上下文相关键插入选中项」开启时，
        // 平台会把 `,` 当成接受高亮候选的按键
        settings.SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS = true
        try {
            myFixture.type("0")
            val lookup = awaitSpelLookup()
            assertNotNull("输入 0 后未自动弹出 SpEL 补全", lookup)
            assertNotNull(
                "列表中没有高亮候选，无法复现「按键接受候选」: " +
                    lookup!!.items.map { it.lookupString },
                lookup.currentItem
            )
            // 过滤器必须直接否决平台默认过滤器的「接受候选」决定
            assertEquals(
                "逗号不应被当作接受候选",
                com.intellij.codeInsight.lookup.CharFilter.Result.HIDE_LOOKUP,
                com.intellij.codeInsight.lookup.impl.LookupTypedHandler.getLookupAction(
                    ',',
                    lookup as com.intellij.codeInsight.lookup.impl.LookupImpl
                )
            )

            myFixture.type(",")
            repeat(5) {
                com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            }
            // 用文档文本断言：PSI 未提交时 file.text 是旧快照，会误判字符丢失
            val text = myFixture.editor.document.text
            assertFalse("输入逗号不应接受高亮候选: $text", text.contains("\$\$0"))
            assertTrue("逗号应正常插入: $text", text.contains("a[0,"))
            assertNull("输入逗号后补全列表应关闭", myFixture.lookup)
        } finally {
            settings.SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS = originalSelection
        }
    }

    /**
     * 等自动弹出的补全真正算出候选。
     *
     * typedHandler 触发的补全在后台线程计算，只 flush 事件队列时可能还拿不到候选，
     * 需要让出时间片等其结果回投，否则会误判「列表为空」。
     */
    private fun awaitSpelLookup(): LookupEx? {
        val deadline = System.currentTimeMillis() + 3000
        while (System.currentTimeMillis() < deadline) {
            com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            val lookup = myFixture.lookup
            if (lookup != null && lookup.items.any { it is SpelOrderedElement }) {
                return lookup
            }
            Thread.sleep(10)
        }
        return myFixture.lookup
    }

    /**
     * 字符过滤器靠 `order="first"` 抢占平台默认过滤器。
     *
     * `DefaultCharFilter` 会把 `,` 固定当成「接受选中项」，它和 Java 自带的
     * 过滤器都必须排在 `SpelCharFilter` 之后，`getFilterDecision` 才会先拿到
     * 我们的否决结果；`LiveTemplateCharFilter` 可能更靠前，但只在模板列表里
     * 处理按键，不影响 `,`。
     */
    fun testSpelCharFilterRunsBeforePlatformFilters() {
        val filters = com.intellij.codeInsight.lookup.CharFilter.EP_NAME.extensionList
        val spelIndex = filters.indexOfFirst { it is SpelCharFilter }
        assertTrue("未注册 SpelCharFilter: $filters", spelIndex >= 0)
        val defaultIndex = filters.indexOfFirst { it.javaClass.simpleName == "DefaultCharFilter" }
        assertTrue("平台上未注册 DefaultCharFilter: $filters", defaultIndex >= 0)
        assertTrue("SpelCharFilter 应先于 DefaultCharFilter 执行: $filters", spelIndex < defaultIndex)
        val javaIndex = filters.indexOfFirst { it.javaClass.simpleName == "JavaCharFilter" }
        assertTrue(
            "SpelCharFilter 应先于 JavaCharFilter 执行: $filters",
            javaIndex < 0 || spelIndex < javaIndex
        )
    }

    private fun complete(text: String): List<LookupElement> {
        configure(text)
        myFixture.completeBasic()
        return completionElements()
    }

    private fun completionElements(): List<LookupElement> =
        myFixture.lookupElements?.toList() ?: emptyList()

    /** 列表主文本里的名字部分：函数去掉参数列表，变量去掉 `#` 前缀。 */
    private fun LookupElement.itemName(): String =
        itemText().substringBefore('(').removePrefix("#")

    /** 只保留 SpEL 贡献的候选：排除 Java 单词补全等其他 contributor 混入的元素。 */
    private fun spelElements(elements: List<LookupElement>): List<SpelOrderedElement> =
        elements.filterIsInstance<SpelOrderedElement>()

    private fun spelNames(elements: List<LookupElement>): List<String> =
        spelElements(elements).map { it.itemName() }

    /** 校验用户要求的排序：先排除不含输入字符的候选，再按前缀组/包含组、组内字母序排列。 */
    private fun assertTypedOrder(elements: List<LookupElement>, word: String) {
        val names = spelNames(elements).filter { it.isNotEmpty() }
        assertTrue("缺少 SpEL 候选、无法校验排序", names.isNotEmpty())
        val foreign = names.filterNot { it.contains(word, true) }
        assertTrue("不应出现不含 `$word` 的候选: $foreign", foreign.isEmpty())
        val prefixed = names.filter { it.startsWith(word, true) }
        val contained = names.filter { !it.startsWith(word, true) }
        assertEquals("前缀命中组应按字母序: $names", prefixed.sortedBy { it.lowercase() }, prefixed)
        assertEquals("包含组应按字母序: $names", contained.sortedBy { it.lowercase() }, contained)
        val lastPrefixed = names.indexOfLast { it.startsWith(word, true) }
        val firstContained = names.indexOfFirst { !it.startsWith(word, true) }
        assertTrue(
            "前缀命中组应全部排在包含组之前: $names",
            lastPrefixed < 0 || firstContained < 0 || lastPrefixed < firstContained
        )
    }

    private fun LookupElement.tailText(): String {
        val presentation = LookupElementPresentation()
        renderElement(presentation)
        return presentation.tailText ?: ""
    }

    private fun LookupElement.itemText(): String {
        val presentation = LookupElementPresentation()
        renderElement(presentation)
        return presentation.itemText ?: ""
    }

    private fun LookupElement.typeText(): String {
        val presentation = LookupElementPresentation()
        renderElement(presentation)
        return presentation.typeText ?: ""
    }

    private fun LookupElement.icon(): Icon? {
        val presentation = LookupElementPresentation()
        renderElement(presentation)
        return presentation.icon
    }

    /** 按默认样式渲染候选的文档预览。 */
    private fun completionPreview(element: LookupElement): String {
        val declaration = element.psiElement as? SpelDeclaration
        assertNotNull("候选缺少虚拟声明: ${element.lookupString}", declaration)
        val documentation = declaration!!.documentation
        assertNotNull("虚拟声明缺少文档", documentation)
        return SpelDocumentationRenderer.renderCompletion(documentation!!)
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
