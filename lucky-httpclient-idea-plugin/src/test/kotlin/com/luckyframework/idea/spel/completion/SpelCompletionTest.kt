package com.luckyframework.idea.spel.completion

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
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
        val strings = elements.map { it.lookupString }
        assertTrue("ArrayList" in strings)
        assertTrue("ArrayDeque" in strings)
        val element = elements.first { it.lookupString == "ArrayList" }
        assertEquals("java.util.ArrayList", element.typeText())
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

    private fun complete(text: String): List<LookupElement> {
        configure(text)
        myFixture.completeBasic()
        return completionElements()
    }

    private fun completionElements(): List<LookupElement> =
        myFixture.lookupElements?.toList() ?: emptyList()

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
