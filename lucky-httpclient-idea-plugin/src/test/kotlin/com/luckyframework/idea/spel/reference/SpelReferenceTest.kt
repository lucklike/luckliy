package com.luckyframework.idea.spel.reference

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.metadata.SpelLifecycle
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import com.luckyframework.idea.spel.symbol.SpelSymbolOrigin
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 10.1–10.6：引用范围、变量/函数/命名空间/类型/Bean 跳转与未解析降级。
 */
class SpelReferenceTest : SymbolFixtureTestCase() {

    fun testReferenceRangeAndSoft() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId}")
    void call(String userId) {
    }
}
"""
        )
        val literal = literalContaining("#{#userId}")
        val reference = referenceOf(literal, "#userId")

        assertEquals(3, reference.rangeInElement.startOffset)
        assertEquals("#userId", reference.rangeInElement.substring(literal.text))
        assertTrue(reference.isSoft)
    }

    fun testParameterReferenceResolvesToParameter() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId}")
    void call(String userId) {
    }
}
"""
        )
        val literal = literalContaining("#{#userId}")
        val declaration = resolveDeclaration(literal, "#userId")

        assertEquals("方法参数", declaration.kindLabel)
        assertEquals("userId", declaration.name)
        val parameter = methodNamed("call").parameterList.parameters[0]
        assertSame(parameter, declaration.target)
        assertEquals(parameter.nameIdentifier!!.textRange.startOffset, declaration.textOffset)
    }

    fun testShorthandReferenceResolvesToParameter() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}0}")
    void call(String keyword, int page) {
    }
}
"""
        )
        val literal = literalContaining("#{${'$'}0}")
        val declaration = resolveDeclaration(literal, "${'$'}0")

        assertEquals("方法参数", declaration.kindLabel)
        assertSame(methodNamed("call").parameterList.parameters[0], declaration.target)
    }

    fun testTypedParameterReferenceResolves() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{userId::type}")
    void call(String userId) {
    }
}
"""
        )
        val literal = literalContaining("#{userId::type}")
        val declaration = resolveDeclaration(literal, "userId::type")

        assertEquals("方法参数", declaration.kindLabel)
        assertSame(methodNamed("call").parameterList.parameters[0], declaration.target)
    }

    fun testImportVariableReferenceResolvesToDeclaration() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "token=#{1 + 1}")
class Api {
    @HttpRequest(url = "#{#token}")
    void call() {
    }
}
"""
        )
        val importLiteral = literalContaining("token=")
        val declaration = resolveDeclaration(literalContaining("#{#token}"), "#token")

        assertEquals("@SpELImport 变量", declaration.kindLabel)
        assertSame(importLiteral, declaration.target)
        val expected = importLiteral.textRange.startOffset + importLiteral.text.indexOf("token")
        assertEquals(expected, declaration.textOffset)
    }

    fun testBuiltinVariableResolvesToVirtualDeclaration() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}status${'$'}}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("status"), "${'$'}status${'$'}")

        assertEquals("内置变量", declaration.kindLabel)
        assertNull(declaration.target)
        assertFalse(declaration.canNavigateToSource())
        assertEquals("${'$'}status${'$'}", declaration.presentation.presentableText)
        assertTrue(declaration.presentation.locationString!!.contains("内置变量"))
    }

    fun testGlobalVariableResolvesThroughNavigation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{x}")
    void call() {
    }
}
"""
        )
        val method = methodNamed("call")
        val global = SpelSymbol(
            name = "globalToken",
            kind = SpelSymbolKind.VARIABLE,
            type = "String",
            origin = SpelSymbolOrigin.GLOBAL,
            declaration = method,
            lifecycle = SpelLifecycle.REQUEST_INIT,
            sourceRange = TextRange(0, 0)
        )
        val context = SpelContext(true, SpelLifecycle.REQUEST_INIT, null, listOf(global))

        val declarations = SpelNavigation(project, context).variables("globalToken")
        assertEquals(1, declarations.size)
        assertEquals("全局变量", declarations[0].kindLabel)
        assertSame(method, declarations[0].target)
    }

    fun testBuiltinFunctionResolvesToVirtualDeclaration() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#time()}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("#time"), "#time")

        assertEquals("函数", declaration.kindLabel)
        // 测试工程没有框架依赖，找不到 owner 类时降级为虚拟声明
        assertNull(declaration.target)
        assertFalse(declaration.canNavigateToSource())
        assertTrue(declaration.presentation.locationString!!.contains("time()"))
    }

    /** 内置函数按 owner/methodName 跳到框架源码中的真实方法。 */
    fun testBuiltinFunctionNavigatesToOwnerMethod() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/proxy/function/CommonFunctions.java",
            """
package com.luckyframework.httpclient.proxy.function;

public class CommonFunctions {
    public static long time() { return 0L; }
    public static boolean strNe(Object o1, Object o2) { return false; }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#time()}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("#time"), "#time")

        assertEquals("函数", declaration.kindLabel)
        assertTrue("内置函数应可跳转", declaration.canNavigateToSource())
        val method = declaration.target as com.intellij.psi.PsiMethod
        assertEquals("time", method.name)
        assertEquals("CommonFunctions", method.containingClass?.name)
    }

    /** 别名函数（str_ne → strNe）按 methodName 定位真实方法。 */
    fun testBuiltinAliasFunctionNavigatesToRealMethod() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/proxy/function/CommonFunctions.java",
            """
package com.luckyframework.httpclient.proxy.function;

public class CommonFunctions {
    public static boolean strNe(Object o1, Object o2) { return false; }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#str_ne(a, b)}")
    void call(String a, String b) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("#str_ne"), "#str_ne")

        val method = declaration.target as com.intellij.psi.PsiMethod
        assertEquals("strNe", method.name)
        assertEquals("CommonFunctions", method.containingClass?.name)
    }

    /** 当前类声明的静态方法注册为 SpEL 函数后可直接跳转到方法声明。 */
    fun testClassStaticFunctionResolvesToDeclaringMethod() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{localHelper('a')}")
    void call() {
    }

    public static String localHelper(String text) {
        return text;
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("localHelper"), "localHelper")

        assertEquals("函数", declaration.kindLabel)
        assertTrue("当前类静态方法应可跳转", declaration.canNavigateToSource())
        val method = declaration.target as com.intellij.psi.PsiMethod
        assertEquals("localHelper", method.name)
        assertEquals("Api", method.containingClass?.name)
    }

    /** 父类声明的静态方法同样可用，跳转定位到父类中的方法。 */
    fun testInheritedStaticFunctionResolvesToSuperMethod() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api extends Base {
    @HttpRequest(url = "#{#inheritedHelper()}")
    void call() {
    }
}

class Base {
    public static String inheritedHelper() {
        return "";
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("#inheritedHelper"), "#inheritedHelper")

        assertEquals("函数", declaration.kindLabel)
        val method = declaration.target as com.intellij.psi.PsiMethod
        assertEquals("inheritedHelper", method.name)
        assertEquals("Base", method.containingClass?.name)
    }

    /** 父接口声明的静态方法同样可用（运行期 `ClassUtils.getAllMethod` 会递归父接口）。 */
    fun testSuperInterfaceStaticFunctionResolves() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

interface Api extends Base {
    @HttpRequest(url = "#{#ifaceHelper()}")
    void call();
}

interface Base {
    static String ifaceHelper() {
        return "";
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("#ifaceHelper"), "#ifaceHelper")

        assertEquals("函数", declaration.kindLabel)
        val method = declaration.target as com.intellij.psi.PsiMethod
        assertEquals("ifaceHelper", method.name)
        assertEquals("Base", method.containingClass?.name)
    }

    fun testAliasFunctionResolvesToOriginalMethod() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{#crypto.sha('a')}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("#crypto.sha"), "sha")

        assertEquals("函数", declaration.kindLabel)
        assertEquals("sha", declaration.name)
        assertEquals("sha256", (declaration.target as com.intellij.psi.PsiMethod).name)
    }

    fun testOverloadedFunctionOffersMultipleTargets() {
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
        configure(
            """
package p;

import com.example.Overloads;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(Overloads.class)
    @HttpRequest(url = "#{#pick('a')}")
    void call() {
    }
}
"""
        )
        val literal = literalContaining("#pick")
        val reference = referenceOf(literal, "#pick") as PsiPolyVariantReference

        val targets = reference.multiResolve(false)
        assertEquals(2, targets.size)
        val labels = targets.map { (it.element as SpelDeclaration).presentation.locationString!! }
        assertTrue(labels.any { it.contains("pick(String)") })
        assertTrue(labels.any { it.contains("pick(int)") })
    }

    fun testNamespaceFunctionResolvesFlat() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{#md5('a')}")
    void call() {
    }
}
"""
        )
        val literal = literalContaining("#md5")
        val reference = referenceOf(literal, "#md5") as PsiPolyVariantReference

        // 平铺调用同时能看到内置函数与命名空间函数，用 multiResolve 避免多目标歧义
        val targets = reference.multiResolve(false).map { it.element as SpelDeclaration }
        assertTrue(
            "未解析到 MyUtils.md5: ${targets.map { it.presentation.locationString }}",
            targets.any { target ->
                val method = target.target as? com.intellij.psi.PsiMethod
                method?.name == "md5" && method.containingClass?.qualifiedName == "com.example.MyUtils"
            }
        )
    }

    fun testNamespaceMemberResolvesToStaticMethod() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{#crypto.md5('a')}")
    void call() {
    }
}
"""
        )
        val literal = literalContaining("#crypto.md5")
        val declaration = resolveDeclaration(literal, "md5")

        assertEquals("函数", declaration.kindLabel)
        assertEquals("md5", (declaration.target as com.intellij.psi.PsiMethod).name)
    }

    fun testNamespaceReferenceResolvesToClass() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{#crypto.md5('a')}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("#crypto.md5"), "#crypto")

        assertEquals("命名空间", declaration.kindLabel)
        assertEquals("MyUtils", (declaration.target as com.intellij.psi.PsiClass).name)
    }

    fun testTypeReferenceResolvesToClass() {
        myFixture.addFileToProject(
            "java/util/ArrayList.java",
            "package java.util;\npublic class ArrayList<E> {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(java.util.ArrayList)}")
    void call() {
    }
}
"""
        )
        val literal = literalContaining("T(java.util.ArrayList)")
        val declaration = resolveDeclaration(literal, "java.util.ArrayList")

        assertEquals("类型", declaration.kindLabel)
        assertEquals("ArrayList", (declaration.target as com.intellij.psi.PsiClass).name)
    }

    fun testBeanReferenceResolvesToClass() {
        myFixture.addFileToProject(
            "com/example/UserMapper.java",
            "package com.example;\npublic class UserMapper {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{@userMapper}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("@userMapper"), "@userMapper")

        assertEquals("Bean", declaration.kindLabel)
        assertEquals("UserMapper", (declaration.target as com.intellij.psi.PsiClass).name)
    }

    /** 成员属性解析到字段（含 Javadoc）——Lombok 实体的 private 字段也要能跳转。 */
    fun testPropertyResolvesToPrivateFieldWithDoc() {
        myFixture.addFileToProject(
            "com/example/ChunkUploadMeta.java",
            """
package com.example;

public class ChunkUploadMeta {
    /** 文件唯一标识。 */
    private String fileId;

    public String getFileId() {
        return fileId;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.ChunkUploadMeta;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{meta.fileId}")
    void call(ChunkUploadMeta meta) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("meta.fileId"), "fileId")

        assertEquals("字段", declaration.kindLabel)
        assertEquals("fileId", declaration.name)
        assertTrue(declaration.canNavigateToSource())
        assertTrue(
            "悬停文档应包含字段注释",
            declaration.documentation!!.description!!.contains("文件唯一标识")
        )
    }

    /** 只有 getter 没有字段时，成员解析到访问器方法。 */
    fun testGetterOnlyPropertyResolvesToMethod() {
        myFixture.addFileToProject(
            "com/example/ChunkUploadResult.java",
            """
package com.example;

public class ChunkUploadResult {
    /** 分片总数。 */
    public int getTotalChunks() {
        return 0;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.ChunkUploadResult;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{result.totalChunks}")
    void call(ChunkUploadResult result) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("result.totalChunks"), "totalChunks")

        assertEquals("方法", declaration.kindLabel)
        assertEquals("getTotalChunks", declaration.name)
        assertTrue(
            "悬停文档应包含方法注释",
            declaration.documentation!!.description!!.contains("分片总数")
        )
    }

    /** 类型明确的内置变量的成员方法，跳转到框架类中的声明（同名依赖类不干扰）。 */
    fun testBuiltinVariableMemberResolvesToFrameworkMethod() {
        addRequestStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}req${'$'}.getUrl()}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("req"), "getUrl")

        assertEquals("方法", declaration.kindLabel)
        assertTrue("内置变量的成员方法应可跳转", declaration.canNavigateToSource())
        val method = declaration.target as com.intellij.psi.PsiMethod
        assertEquals("getUrl", method.name)
        assertEquals(
            "com.luckyframework.httpclient.core.meta.Request",
            method.containingClass?.qualifiedName
        )
    }

    /** 内置变量的 JavaBean 属性（`baseUrl`）解析到框架类中的 getter。 */
    fun testBuiltinVariablePropertyResolvesToFrameworkGetter() {
        addRequestStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}req${'$'}.baseUrl}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("req"), "baseUrl")

        assertEquals("方法", declaration.kindLabel)
        assertEquals("getBaseUrl", declaration.name)
        assertEquals(
            "com.luckyframework.httpclient.core.meta.Request",
            (declaration.target as com.intellij.psi.PsiMethod).containingClass?.qualifiedName
        )
    }

    /** 内置变量的成员字段（常量）也支持跳转。 */
    fun testBuiltinVariableFieldResolvesToFrameworkField() {
        addRequestStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}req${'$'}.VERSION}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("req"), "VERSION")

        assertEquals("字段", declaration.kindLabel)
        assertEquals("VERSION", declaration.name)
        assertTrue(declaration.canNavigateToSource())
    }

    /** 框架 `Request` 与同名依赖类同时存在，验证类型不允许被后者抢走。 */
    private fun addRequestStubs() {
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
    public String getUrl() {
        return "";
    }

    public String getBaseUrl() {
        return "";
    }
}
"""
        )
    }

    fun testUnresolvableBeanHasNoTarget() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{@unknownBean}")
    void call() {
    }
}
"""
        )
        val literal = literalContaining("@unknownBean")
        val reference = referenceOf(literal, "@unknownBean")
        assertNull(reference.resolve())
    }

    fun testUnresolvedReferenceDoesNotBlockEditing() {
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
        val literal = literalContaining("#notDefined")
        val reference = referenceOf(literal, "#notDefined")
        assertNull(reference.resolve())

        // 未解析引用是软引用，输入新字符不会被引用解析阻塞
        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.insertString(myFixture.caretOffset, "x")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        assertTrue(myFixture.file.text.contains("#notDefinedx"))
        assertTrue(literalContaining("#notDefinedx").references.all { it.isSoft })
    }

    private fun literalContaining(marker: String): PsiLiteralExpression =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiLiteralExpression::class.java)
            .first { it.text.contains(marker) }

    private fun referenceOf(literal: PsiLiteralExpression, text: String): PsiReference =
        literal.references.first { reference ->
            literal.text.substring(reference.rangeInElement.startOffset, reference.rangeInElement.endOffset) == text
        }

    private fun resolveDeclaration(literal: PsiLiteralExpression, text: String): SpelDeclaration {
        val resolved = referenceOf(literal, text).resolve()
        assertNotNull("引用未解析：$text", resolved)
        return resolved as SpelDeclaration
    }
}
