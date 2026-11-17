package com.luckyframework.idea.spel.reference

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
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
    @HttpRequest(url = "#{userId}")
    void call(String userId) {
    }
}
"""
        )
        val literal = literalContaining("#{userId}")
        val reference = referenceOf(literal, "userId")

        assertEquals(3, reference.rangeInElement.startOffset)
        assertEquals("userId", reference.rangeInElement.substring(literal.text))
        assertTrue(reference.isSoft)
    }

    fun testParameterReferenceResolvesToParameter() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{userId}")
    void call(String userId) {
    }
}
"""
        )
        val literal = literalContaining("#{userId}")
        val declaration = resolveDeclaration(literal, "userId")

        assertEquals("方法参数", declaration.kindLabel)
        assertEquals("userId", declaration.name)
        val parameter = methodNamed("call").parameterList.parameters[0]
        assertSame(parameter, declaration.target)
        assertEquals(parameter.nameIdentifier!!.textRange.startOffset, declaration.textOffset)
    }

    /** `@Param` 覆盖参数名后，跳转定位到注解中书写的名称前。 */
    fun testParamNameNavigatesToAnnotationValue() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.reflect.Param;

class Api {
    @HttpRequest(url = "#{keyword}")
    void call(@Param("keyword") String q) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("keyword"), "keyword")

        assertEquals("方法参数", declaration.kindLabel)
        assertEquals("keyword", declaration.name)
        val annotationValue = annotationStringLiteral("keyword")
        assertSame(annotationValue, declaration.target)
        // 光标落在字符串内容的首字符前（跳过起始引号）
        assertEquals(annotationValue.textRange.startOffset + 1, declaration.textOffset)
    }

    /** `@Query` 组合注解的别名值同样定位到注解中书写名称处。 */
    fun testQueryAliasNameNavigatesToAnnotationValue() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {
    @HttpRequest(url = "#{req}")
    void call(@Query("req") String request) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("req"), "req")

        val annotationValue = annotationStringLiteral("req")
        assertSame(annotationValue, declaration.target)
        assertEquals(annotationValue.textRange.startOffset + 1, declaration.textOffset)
    }

    /** `@Param` 未写值时没有可跳转的注解位置，仍定位到参数声明上的名字。 */
    fun testParamWithoutValueNavigatesToParameterName() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.reflect.Param;

class Api {
    @HttpRequest(url = "#{keyword}")
    void call(@Param String keyword) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("keyword"), "keyword")

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

    fun testBuiltinVariableResolvesToDeclaringField() {
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
        val field = declaration.target as com.intellij.psi.PsiField
        assertEquals("${'$'}_RESPONSE_STATUS_${'$'}", field.name)
        assertTrue("内置变量应可跳转到 @SpelSymbol 声明字段", declaration.canNavigateToSource())
        assertEquals("${'$'}status${'$'}", declaration.presentation.presentableText)
        assertTrue(declaration.presentation.locationString!!.contains("内置变量"))
    }

    /** `$result$` 跳转到返回类型中推导类型的书写位置（`Optional<Item>` 拆包后的 `Item`）。 */
    fun testResultVariableNavigatesToResultType() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import java.util.Optional;

class Api {
    @HttpRequest(url = "#{${'$'}result${'$'}.name}")
    Optional<Item> fetch() {
        return Optional.empty();
    }
}

class Item {
    public String name;
}
"""
        )
        val declaration = resolveDeclaration(
            literalContaining("${'$'}result${'$'}"),
            "${'$'}result${'$'}"
        )

        assertEquals("内置变量", declaration.kindLabel)
        assertTrue("`\$result\$` 应可跳转", declaration.canNavigateToSource())
        val target = declaration.target as com.intellij.psi.PsiTypeElement
        assertEquals("Item", target.text)
        assertEquals(target.textRange.startOffset, declaration.textOffset)
    }

    /** 没有 `@ConvertMetaType` 时不前推到类型位置，跳转停留在 `@SpelSymbol` 声明字段上。 */
    fun testBodyVariableWithoutMetaTypeStaysOnDeclaration() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}body${'$'}.name}")
    void fetch() {
    }
}

class Item {
    public String name;
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("${'$'}body${'$'}.name"), "${'$'}body${'$'}")

        assertEquals("内置变量", declaration.kindLabel)
        val field = declaration.target as com.intellij.psi.PsiField
        assertEquals("${'$'}_RESPONSE_BODY_${'$'}", field.name)
    }

    /** `$body$` 带 `@ConvertMetaType` 时跳转到注解中类字面量的书写位置。 */
    fun testBodyVariableNavigatesToConvertedType() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

@ConvertMetaType(Item.class)
class Api {
    @HttpRequest(url = "#{${'$'}body${'$'}.name}")
    void fetch() {
    }
}

class Item {
    public String name;
}
"""
        )
        val declaration = resolveDeclaration(
            literalContaining("${'$'}body${'$'}"),
            "${'$'}body${'$'}"
        )

        assertEquals("内置变量", declaration.kindLabel)
        assertTrue("`\$body\$` 应可跳转", declaration.canNavigateToSource())
        val target = declaration.target as com.intellij.psi.PsiClassObjectAccessExpression
        assertEquals("Item.class", target.text)
    }

    /** `$body$` 经 `@RespConvert#metaType` 别名链推导时，跳转落在注解中的类字面量上。 */
    fun testBodyVariableNavigatesToMetaTypeAnnotationValue() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.RespConvert;

class Api {
    @RespConvert(value = "#{${'$'}body${'$'}.name}", metaType = Item.class)
    void fetch() {
    }
}

class Item {
    public String name;
}
"""
        )
        val declaration = resolveDeclaration(
            literalContaining("${'$'}body${'$'}.name"),
            "${'$'}body${'$'}"
        )

        assertEquals("内置变量", declaration.kindLabel)
        val target = declaration.target as com.intellij.psi.PsiClassObjectAccessExpression
        assertEquals("Item.class", target.text)
        assertNotNull(
            "跳转目标应位于注解中而不是类型声明上",
            PsiTreeUtil.getParentOfType(target, PsiAnnotation::class.java, false)
        )
    }

    /** void 方法 + `@ResultHandlerMeta` 时，`$result$` 跳转到处理器类字面量。 */
    fun testResultVariableNavigatesToHandlerClassLiteral() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;

class Api {
    @HttpRequest(url = "#{${'$'}result${'$'}}")
    @ResultHandlerMeta(handlerClass = ItemHandler.class)
    void fetch() {
    }
}

class Item {
}

class ItemHandler implements ResultHandler<Item> {
    public void handleResult(Object result) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("${'$'}result${'$'}"), "${'$'}result${'$'}")

        assertEquals("内置变量", declaration.kindLabel)
        val target = declaration.target as com.intellij.psi.PsiClassObjectAccessExpression
        assertEquals("ItemHandler.class", target.text)
    }

    /** `@ResultHandlerMeta(handler = @ObjectGenerate(...))` 时跳转到该类字面量。 */
    fun testResultVariableNavigatesToObjectGenerateLiteral() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;

class Api {
    @HttpRequest(url = "#{${'$'}result${'$'}}")
    @ResultHandlerMeta(handler = @ObjectGenerate(ItemHandler.class))
    void fetch() {
    }
}

class Item {
}

class ItemHandler implements ResultHandler<Item> {
    public void handleResult(Object result) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("${'$'}result${'$'}"), "${'$'}result${'$'}")

        val target = declaration.target as com.intellij.psi.PsiClassObjectAccessExpression
        assertEquals("ItemHandler.class", target.text)
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

    fun testBuiltinFunctionResolvesToRegisteredMethod() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{time()}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("time("), "time")

        assertEquals("函数", declaration.kindLabel)
        val method = declaration.target as com.intellij.psi.PsiMethod
        assertEquals("time", method.name)
        assertEquals("CommonFunctions", method.containingClass?.name)
        assertTrue(declaration.presentation.locationString!!.contains("time()"))
    }

    /** 别名函数（str_ne → strNotEquals）按 methodName 定位真实方法。 */
    fun testBuiltinAliasFunctionNavigatesToRealMethod() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{str_ne(a, b)}")
    void call(String a, String b) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("str_ne("), "str_ne")

        val method = declaration.target as com.intellij.psi.PsiMethod
        assertEquals("strNotEquals", method.name)
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

    /** `T(短名)` 优先解析到框架类，不被同名依赖类抢走。 */
    fun testTypeReferenceShortNamePrefersFrameworkClass() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/core/meta/Request.java",
            "package com.luckyframework.httpclient.core.meta;\npublic interface Request {}\n"
        )
        myFixture.addFileToProject(
            "okhttp3/Request.java",
            "package okhttp3;\npublic class Request {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(Request)}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("T(Request)"), "Request")

        assertEquals("类型", declaration.kindLabel)
        assertEquals(
            "com.luckyframework.httpclient.core.meta.Request",
            (declaration.target as com.intellij.psi.PsiClass).qualifiedName
        )
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

    /** Spring 常见 `<Name>Impl` 命名的 Bean 同样能解析到声明类。 */
    fun testBeanReferenceResolvesToImplClass() {
        myFixture.addFileToProject(
            "com/example/UserMapperImpl.java",
            "package com.example;\npublic class UserMapperImpl {}\n"
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
        assertEquals("UserMapperImpl", (declaration.target as com.intellij.psi.PsiClass).name)
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

    /** 函数结果作为接收者时，成员方法解析到声明类中的方法。 */
    fun testFunctionResultMemberResolvesToMethod() {
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
    @HttpRequest(url = "#{add(1, 3).get(0)}")
    void call() {
    }

    public static com.example.Bag<Integer> add(Integer a, Integer b) {
        return null;
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("get(0)"), "get")

        assertEquals("方法", declaration.kindLabel)
        assertEquals("get", declaration.name)
        assertEquals(
            "com.example.Bag",
            (declaration.target as com.intellij.psi.PsiMethod).containingClass?.qualifiedName
        )
        assertTrue(
            "悬停文档应包含方法注释",
            declaration.documentation!!.description!!.contains("按下标取元素")
        )
    }

    /** 重载按调用实参选择：`pick('a')` 跳转到 `pick(String)`。 */
    fun testMethodOverloadResolvesByArguments() {
        myFixture.addFileToProject(
            "com/example/Picker.java",
            """
package com.example;

public class Picker {
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

import com.example.Picker;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{picker.pick('a')}")
    void call(Picker picker) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("pick('a')"), "pick")

        val method = declaration.target as com.intellij.psi.PsiMethod
        assertEquals("pick", method.name)
        assertEquals("String", method.parameterList.parameters[0].type.presentableText)
    }

    /** `@bean` 接收者的成员方法与字段一样可跳转。 */
    fun testBeanMemberResolvesToMethod() {
        myFixture.addFileToProject(
            "com/example/UserMapper.java",
            """
package com.example;

public class UserMapper {
    public String findByName(String name) {
        return name;
    }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{@userMapper.findByName('a')}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("findByName"), "findByName")

        assertEquals("方法", declaration.kindLabel)
        assertEquals("UserMapper", (declaration.target as com.intellij.psi.PsiMethod).containingClass?.name)
    }

    /** 安全导航 `?.` 之后的成员与 `.` 一样解析。 */
    fun testSafeNavigationMemberResolves() {
        myFixture.addFileToProject(
            "com/example/Profile.java",
            """
package com.example;

public class Profile {
    /** 昵称。 */
    public String nickname;
}
"""
        )
        configure(
            """
package p;

import com.example.Profile;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{profile?.nickname}")
    void call(Profile profile) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("nickname"), "nickname")

        assertEquals("字段", declaration.kindLabel)
        assertEquals("nickname", declaration.name)
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

    /** `new` 后的限定类型名整体作为一个类型引用，可跳转到类声明。 */
    fun testNewTypeReferenceResolvesToClass() {
        myFixture.addFileToProject(
            "com/example/SpelPair.java",
            """
package com.example;

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
    @HttpRequest(url = "#{new com.example.SpelPair(1, 2)}")
    void call() {
    }
}
"""
        )
        val literal = literalContaining("new com.example.SpelPair")
        val declaration = resolveDeclaration(literal, "com.example.SpelPair")

        assertEquals("类型", declaration.kindLabel)
        assertEquals("SpelPair", (declaration.target as com.intellij.psi.PsiClass).name)
    }

    /** `new` 带泛型实参时，外层限定类名仍是一个可跳转的类型引用。 */
    fun testNewGenericTypeReferenceResolvesToClass() {
        myFixture.addFileToProject(
            "java/util/ArrayList.java",
            "package java.util;\npublic class ArrayList<E> {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{new java.util.ArrayList<String>()}")
    void call() {
    }
}
"""
        )
        val literal = literalContaining("new java.util.ArrayList")
        val declaration = resolveDeclaration(literal, "java.util.ArrayList")

        assertEquals("类型", declaration.kindLabel)
        assertEquals("ArrayList", (declaration.target as com.intellij.psi.PsiClass).name)
    }

    /** `instanceof T(...)` 中的类型名同样是类型引用。 */
    fun testInstanceofTypeReferenceResolvesToClass() {
        myFixture.addFileToProject(
            "com/example/SpelUser.java",
            "package com.example;\npublic class SpelUser {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}body${'$'} instanceof T(com.example.SpelUser)}")
    void call() {
    }
}
"""
        )
        val literal = literalContaining("instanceof T(com.example.SpelUser)")
        val declaration = resolveDeclaration(literal, "com.example.SpelUser")

        assertEquals("类型", declaration.kindLabel)
        assertEquals("SpelUser", (declaration.target as com.intellij.psi.PsiClass).name)
    }

    /** `java.lang` 短名 `Object` 也属于明确的类型引用，可跳转。 */
    fun testInstanceofJavaLangShortTypeResolvesToClass() {
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
    @HttpRequest(url = "#{${'$'}body${'$'} instanceof T(Object)}")
    void call() {
    }
}
"""
        )
        val literal = literalContaining("instanceof T(Object)")
        val declaration = resolveDeclaration(literal, "Object")

        assertEquals("类型", declaration.kindLabel)
        assertEquals(
            "java.lang.Object",
            (declaration.target as com.intellij.psi.PsiClass).qualifiedName
        )
    }

    /** 选择方括号内的裸标识符解析到元素类型的字段。 */
    fun testElementBracketPropertyResolvesToField() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[name != null]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("name != null"), "name")

        assertEquals("字段", declaration.kindLabel)
        assertEquals("name", declaration.name)
        assertEquals(
            "com.example.ElementUser",
            (declaration.target as com.intellij.psi.PsiField).containingClass?.qualifiedName
        )
    }

    /** 元素内的 `#this.age` 解析到当前元素类型的字段。 */
    fun testThisMemberInsideElementResolvesToField() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[#this.age > 1]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("#this.age"), "age")

        assertEquals("字段", declaration.kindLabel)
        assertEquals("age", declaration.name)
        assertTrue(declaration.canNavigateToSource())
    }

    /** 投影方括号内的裸方法解析到元素类型的方法。 */
    fun testElementBracketBareMethodResolves() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.![getName()]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("getName"), "getName")

        assertEquals("方法", declaration.kindLabel)
        assertEquals(
            "com.example.ElementUser",
            (declaration.target as com.intellij.psi.PsiMethod).containingClass?.qualifiedName
        )
    }

    /** 元素类型的属性写法回退到 getter 方法。 */
    fun testElementBracketGetterPropertyResolves() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.![nickName]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("nickName"), "nickName")

        assertEquals("方法", declaration.kindLabel)
        assertEquals("getNickName", declaration.name)
    }

    /** 元素成员的链式方法按实际接收者类型解析（`name` 是 String）。 */
    fun testElementMemberChainResolves() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[name.length() > 1]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("length()"), "length")

        assertEquals("方法", declaration.kindLabel)
        assertEquals("length", declaration.name)
        assertEquals(
            "java.lang.String",
            (declaration.target as com.intellij.psi.PsiMethod).containingClass?.qualifiedName
        )
    }

    /** 元素类型上不存在的成员保持软引用、没有跳转目标。 */
    fun testElementBracketUnknownMemberDoesNotResolve() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[nationality != null]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        val reference = referenceOf(literalContaining("nationality"), "nationality")

        assertTrue(reference.isSoft)
        assertNull(reference.resolve())
    }

    /** 嵌套投影内层的裸标识符按内层元素类型解析。 */
    fun testNestedProjectionInnerMemberResolves() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementGroup;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{groups.![members.![name]]}")
    void call(java.util.List<ElementGroup> groups) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("members.![name]"), "name")

        assertEquals("字段", declaration.kindLabel)
        assertEquals("name", declaration.name)
        assertEquals(
            "com.example.ElementUser",
            (declaration.target as com.intellij.psi.PsiField).containingClass?.qualifiedName
        )
    }

    /** Map 选择方括号内的元素是 `Map.Entry`，`key` 属性回退到 getter。 */
    fun testMapEntryKeyResolves() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[key != null]}")
    void call(java.util.Map<String, ElementUser> users) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("key != null"), "key")

        assertEquals("方法", declaration.kindLabel)
        assertEquals("getKey", declaration.name)
    }

    /** 元素类型未知时引用保持软引用、没有跳转目标（降级不误报）。 */
    fun testUnknownElementTypeMemberReferenceIsSoft() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{items.?[nationality]}")
    void call(Object items) {
    }
}
"""
        )
        val reference = referenceOf(literalContaining("nationality"), "nationality")

        assertTrue(reference.isSoft)
        assertNull(reference.resolve())
    }

    /** `. ?[` 标记前的空白不影响成员引用。 */
    fun testSpaceBeforeSelectionMarkerMemberResolves() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users. ?[name]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("name]"), "name")

        assertEquals("字段", declaration.kindLabel)
        assertEquals("name", declaration.name)
    }

    /** `#user.name` 的 `#user` 是普通变量，解析到参数而不是命名空间。 */
    fun testVariableBeforeDotResolvesToParameter() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{user.name}")
    void call(ElementUser user) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("user.name"), "user")

        assertEquals("方法参数", declaration.kindLabel)
        assertEquals("user", declaration.name)
    }

    /** 选择/投影引用共用的 `List<ElementUser>` 类型桩（MockJdk 不含 java.util）。 */
    private fun addElementStubs() {
        myFixture.addFileToProject(
            "com/example/ElementUser.java",
            """
package com.example;

public class ElementUser {

    private String name;

    private int age;

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public String getNickName() {
        return name;
    }

    public boolean isActive() {
        return true;
    }
}
"""
        )
        myFixture.addFileToProject(
            "java/lang/String.java",
            """
package java.lang;

public final class String {

    public int length() {
        return 0;
    }
}
"""
        )
        myFixture.addFileToProject(
            "java/lang/Integer.java",
            """
package java.lang;

public final class Integer {
}
"""
        )
        myFixture.addFileToProject(
            "java/lang/Iterable.java",
            """
package java.lang;

public interface Iterable<T> {
}
"""
        )
        myFixture.addFileToProject(
            "java/util/Collection.java",
            """
package java.util;

public interface Collection<E> extends Iterable<E> {
}
"""
        )
        myFixture.addFileToProject(
            "java/util/List.java",
            """
package java.util;

public interface List<E> extends Collection<E> {

    E get(int index);

    int size();
}
"""
        )
        myFixture.addFileToProject(
            "java/util/Map.java",
            """
package java.util;

public interface Map<K, V> {

    V get(Object key);

    int size();

    interface Entry<K, V> {

        K getKey();

        V getValue();
    }
}
"""
        )
        myFixture.addFileToProject(
            "com/example/ElementGroup.java",
            """
package com.example;

import java.util.List;

public class ElementGroup {

    private String code;

    private List<ElementUser> members;

    public String getCode() {
        return code;
    }

    public List<ElementUser> getMembers() {
        return members;
    }
}
"""
        )
    }

    private fun literalContaining(marker: String): PsiLiteralExpression =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiLiteralExpression::class.java)
            .first { it.text.contains(marker) }

    /** 注解属性中的字符串字面量（内容与 [value] 相等，如 `@Param("keyword")`）。 */
    private fun annotationStringLiteral(value: String): PsiLiteralExpression =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiLiteralExpression::class.java)
            .first { it.value == value }

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
