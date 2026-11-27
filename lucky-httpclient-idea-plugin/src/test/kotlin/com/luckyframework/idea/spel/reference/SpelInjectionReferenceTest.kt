package com.luckyframework.idea.spel.reference

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.inspection.SpelInjectionTypeInspection
import com.luckyframework.idea.spel.inspection.UnresolvedSpelSymbolInspection
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * static 函数参数注入（`@Rar`/`@Var`/`@Env`/`@Bean`/`@Param`）的注入源语义。
 *
 * 运行期 `Context#getMethodParamObject` 在**调用方**方法的上下文中求值注入
 * 表达式：`@Post(func = "...")` 执行函数时复用调用方的变量表，函数自身的
 * 参数不在求值上下文中。因此跳转、悬停与类型检查都必须指向调用方声明，
 * 而不是注入目标参数自身。
 */
class SpelInjectionReferenceTest : SymbolFixtureTestCase() {

    /** 用户实测场景：`@Rar("a")` 跳转到调用方 `index` 的参数，而不是函数自身参数。 */
    fun testRarNameResolvesToCallerParameter() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Rar;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a, String b);

    static String indexFunc(@Rar("a") Integer a) {
        return null;
    }
}
"""
        )
        val declaration = declarationOf(literalOf("a"))

        assertEquals("方法参数", declaration.kindLabel)
        val target = declaration.target as PsiParameter
        assertEquals("a", target.name)
        assertEquals("String", target.type.presentableText)
        assertSame(methodNamed("index"), PsiTreeUtil.getParentOfType(target, PsiMethod::class.java, false))
    }

    /** `@Var("c")` 走普通变量作用域，目标是调用方类上的 `@SpELImport` 变量声明。 */
    fun testVarNameResolvesToCallerImportVariable() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.Var;

@SpELImport(var = "c=2206")
interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Var("c") Integer c) {
        return null;
    }
}
"""
        )
        val declaration = declarationOf(literalOf("c"))

        assertEquals("@SpELImport 变量", declaration.kindLabel)
        assertSame(literalOf("c=2206"), declaration.target)
    }

    /** 多个调用方时保留各自的同名参数声明：类型检查需要按「任一兼容」判断。 */
    fun testMultipleCallersKeepEachParameterDeclaration() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Rar;

class MockApi {
    @HttpRequest(func = "join")
    String first(String value) {
        return null;
    }

    @HttpRequest(func = "join")
    String second(Integer value) {
        return null;
    }

    static String join(@Rar("value") String value) {
        return null;
    }
}
"""
        )
        val targets = declarationsOf(literalOf("value"))
            .mapNotNull { it.target as? PsiParameter }

        assertEquals(2, targets.size)
        assertEquals(setOf("String", "Integer"), targets.map { it.type.presentableText }.toSet())
        val owners = targets.map { PsiTreeUtil.getParentOfType(it, PsiMethod::class.java, false)?.name }
        assertTrue("调用方应为 first/second: $owners", owners.containsAll(listOf("first", "second")))
    }

    /** 注入源类型（调用方 String 参数）与目标参数类型（Integer）不兼容必须报错。 */
    fun testTypeMismatchReportedForInjectedCallerParameter() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Rar;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Rar("a") Integer a) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        val infos = highlightsContaining("参数注入类型不匹配")

        assertFalse("应报告类型不匹配", infos.isEmpty())
        assertTrue("应说明来源类型: ${infos.first().description}", infos.first().description!!.contains("String"))
        assertEquals("类型不匹配应直接报错", HighlightSeverity.ERROR, infos.first().severity)
    }

    /** 调用方参数与目标参数类型一致时不报错。 */
    fun testCompatibleInjectionIsNotReported() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Rar;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Rar("a") String a) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        assertTrue(
            "类型一致不应报错",
            highlightsContaining("参数注入类型不匹配").isEmpty()
        )
    }

    /**
     * `@Bean` 注入按 Bean 的实际类型检查：没有继承关系的接口之间不能互相注入。
     *
     * 回归：Bean 的跳转目标可能是 `@Bean` 方法（PsiMethod），检查直接取其
     * 类型会得到 null 而静默跳过；这里必须按 Bean 模型的实际类型报告。
     */
    fun testBeanInjectionTypeMismatchIsReported() {
        addProfileDBApi()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Bean;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Bean("tdiProfileKeypointDBApi") MockApi api) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        val infos = highlightsContaining("参数注入类型不匹配")

        assertFalse("应报告 Bean 类型不匹配", infos.isEmpty())
        assertTrue(
            "应说明 Bean 的实际类型: ${infos.first().description}",
            infos.first().description!!.contains("TdiProfileKeypointDBApi")
        )
        assertEquals("Bean 类型不匹配应报错", HighlightSeverity.ERROR, infos.first().severity)
    }

    /** Bean 的实际类型与参数类型一致时不报错。 */
    fun testCompatibleBeanInjectionIsNotReported() {
        addProfileDBApi()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Bean;
import org.example.api.TdiProfileKeypointDBApi;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Bean("tdiProfileKeypointDBApi") TdiProfileKeypointDBApi api) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        assertTrue(
            "Bean 类型一致不应报错",
            highlightsContaining("参数注入类型不匹配").isEmpty()
        )
    }

    private fun addProfileDBApi() {
        myFixture.addFileToProject(
            "org/example/api/TdiProfileKeypointDBApi.java",
            """
package org.example.api;

public interface TdiProfileKeypointDBApi {
}
"""
        )
    }

    /**
     * 找不到调用方（函数只在外部被调用）时注入源未知：
     * 函数自身的参数不是注入源，未解析检查也不能据此误报。
     */
    fun testWithoutCallerInjectionSourceIsUnknown() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.Rar;

class MockApi {
    static String helper(@Rar("a") Integer a) {
        return null;
    }
}
"""
        )
        val context = SpelReferenceProvider.contextOf(literalOf("a"))

        assertTrue(context.known)
        assertTrue("应标记注入源未知", context.injectionSourceUnknown)
        assertTrue("函数自身参数不是注入源", context.rootVariables("a").isEmpty())

        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
        assertTrue(
            "注入源未知时不应误报未解析",
            highlightsContaining("未解析的 Root 变量").isEmpty()
        )
    }

    /**
     * `@Param` 是 SpEL 模板：`md5(...)` 的真实返回类型是 `byte[]`
     * （DigestFunctions），数组无法注入 Integer 参数，必须报错。
     */
    fun testParamExpressionResultTypeMismatchIsReported() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.reflect.Param;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a, String b);

    static String indexFunc(@Param("#{md5(b)}") Integer b) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        val infos = highlightsContaining("参数注入类型不匹配")

        assertFalse("应报告表达式结果类型不匹配", infos.isEmpty())
        assertTrue("应说明结果类型 byte[]: ${infos.first().description}", infos.first().description!!.contains("byte[]"))
        assertEquals("表达式结果类型不匹配应报错", HighlightSeverity.ERROR, infos.first().severity)
    }

    /**
     * 根因回归：类型兼容按 `PsiType` 结构判断，不是只针对 `md5` 特判。
     *
     * 任意返回数组的函数（`byte[]`/`int[]`/`String[]`）都不能注入标量参数，
     * 包括 `String`：运行期的 Base64/文本化转换不能掩盖「数组注入标量」；
     * 数组参数与数值互转仍按运行期规则放行。
     */
    fun testArrayResultFollowsStructuralTypeRules() {
        myFixture.addFileToProject(
            "com/example/Encoders.java",
            """
package com.example;

public class Encoders {
    public static byte[] bytesOf(Object value) { return new byte[0]; }
    public static int[] intsOf(Object value) { return new int[0]; }
    public static String[] stringsOf(Object value) { return new String[0]; }
    public static int numberOf(Object value) { return 0; }
    public static long longOf(Object value) { return 0L; }
}
"""
        )
        configure(
            """
package p;

import com.example.Encoders;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.reflect.Param;

@SpELImport(Encoders.class)
interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String b);

    static String indexFunc(
            @Param("#{bytesOf(b)}") Integer a1,
            @Param("#{intsOf(b)}") Integer a2,
            @Param("#{stringsOf(b)}") Integer a3,
            @Param("#{numberOf(b)}") Integer a4,
            @Param("#{longOf(b)}") Integer a5,
            @Param("#{bytesOf(b)}") byte[] a6,
            @Param("#{stringsOf(b)}") String[] a7,
            @Param("#{bytesOf(b)}") String a8,
            @Param("#{bytesOf(b)}") Boolean a9,
            @Param("#{bytesOf(b)}") java.util.Map<String, Object> a10) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        val infos = highlightsContaining("参数注入类型不匹配")
        val mismatched = infos.mapNotNull { it.description }
        assertEquals("只应报告数组注入标量的六个参数: $mismatched", 6, infos.size)
        assertTrue("应报告 a1: $mismatched", mismatched.any { it.contains("'a1'") && it.contains("byte[]") })
        assertTrue("应报告 a2: $mismatched", mismatched.any { it.contains("'a2'") })
        assertTrue("应报告 a3: $mismatched", mismatched.any { it.contains("'a3'") })
        assertTrue("数组注入 String 也应报告 a8: $mismatched", mismatched.any { it.contains("'a8'") && it.contains("byte[]") })
        assertTrue("数组注入 Boolean 也应报告 a9: $mismatched", mismatched.any { it.contains("'a9'") })
        assertTrue("数组注入 Map 也应报告 a10: $mismatched", mismatched.any { it.contains("'a10'") })
        assertTrue("数组注入标量应报错", infos.all { it.severity == HighlightSeverity.ERROR })
    }

    /**
     * 用户场景回归：`md5_hex(b)` 返回 String，`.bytes` 经 `String.getBytes()`
     * 推导为 `byte[]`；注入 String 参数同样是数组注入标量，必须报错。
     */
    fun testParamExpressionArrayResultIntoStringIsReported() {
        myFixture.addFileToProject(
            "java/lang/String.java",
            """
package java.lang;

public final class String {
    public byte[] getBytes() { return null; }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.reflect.Param;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a, String b);

    static String indexFunc(
            @Param("#{md5_hex(b).bytes}") String s,
            @Param("#{md5_hex(b).bytes}") byte[] bs) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        val infos = highlightsContaining("参数注入类型不匹配")
        val mismatched = infos.mapNotNull { it.description }

        assertEquals("只有 String 参数应报错: $mismatched", 1, infos.size)
        assertTrue("应说明结果类型 byte[]: $mismatched", mismatched.first().contains("byte[]"))
        assertTrue("应指向参数 s: $mismatched", mismatched.first().contains("'s'"))
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    /** 表达式结果本身能注入时不报错：`#{'123'}` 可以转成 Integer。 */
    fun testParamExpressionCompatibleIsNotReported() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.reflect.Param;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Param("#{'123'}") Integer number) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        assertTrue(
            "数值字符串可以转换，不应报错",
            highlightsContaining("参数注入类型不匹配").isEmpty()
        )
    }

    /**
     * `@SpELImport(varLit = ...)` 的值是字面量，不做 SpEL 求值：
     * `2--206` 无法转成 Integer，必须报错。
     */
    fun testVarLitValueMismatchIsReported() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.Var;

@SpELImport(varLit = {"c=2--206"})
interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Var("c") Integer c) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        val infos = highlightsContaining("参数注入类型不匹配")

        assertFalse("应报告字面量变量值不匹配", infos.isEmpty())
        assertTrue("应说明变量的值: ${infos.first().description}", infos.first().description!!.contains("2--206"))
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    /** `@SpELImport(var = ...)` 的值按 SpEL 求值：`2206` 是数字，注入 Integer 不报错。 */
    fun testVarSpelValueIsNotReported() {
        addJdkIntegerStub()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.Var;

@SpELImport(var = "c=2206")
interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Var("c") Integer c) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        assertTrue(
            "SpEL 数值可以注入 Integer，不应报错",
            highlightsContaining("参数注入类型不匹配").isEmpty()
        )
    }

    /**
     * `@Env` 的配置值无法转换时只警告：环境值可能被启动参数覆盖，
     * 静态配置不能当成确定的运行期值。
     */
    fun testEnvValueMismatchIsReportedAsWarning() {
        myFixture.addFileToProject(
            "io/github/lucklike/httpclient/SpringSpELConvert.java",
            """
package io.github.lucklike.httpclient;

public class SpringSpELConvert {
}
"""
        )
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "server.port=4-5\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Env;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Env("server.port") Integer port) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        val infos = highlightsContaining("参数注入类型不匹配")

        assertFalse("应报告环境值类型不匹配", infos.isEmpty())
        assertTrue("应说明配置值: ${infos.first().description}", infos.first().description!!.contains("4-5"))
        assertEquals("环境值可能被覆盖，只应警告", HighlightSeverity.WARNING, infos.first().severity)
    }

    /** `key:default` 语法：键未配置时按默认值判断，`4-5` 无法转成 Integer。 */
    fun testEnvDefaultValueMismatchIsReportedAsWarning() {
        myFixture.addFileToProject(
            "io/github/lucklike/httpclient/SpringSpELConvert.java",
            """
package io.github.lucklike.httpclient;

public class SpringSpELConvert {
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Env;

interface MockApi {
    @HttpRequest(func = "indexFunc")
    String index(String a);

    static String indexFunc(@Env("unset.port:4-5") Integer port) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelInjectionTypeInspection())

        val infos = highlightsContaining("参数注入类型不匹配")

        assertFalse("键未配置时应按默认值报告", infos.isEmpty())
        assertTrue("应说明默认值: ${infos.first().description}", infos.first().description!!.contains("4-5"))
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
    }

    /** SpEL 数字字面量推导依赖 `java.lang.Integer`；MockJdk 缺失时补充最小桩。 */
    private fun addJdkIntegerStub() {
        myFixture.addFileToProject(
            "java/lang/Integer.java",
            """
package java.lang;

public final class Integer {
}
"""
        )
        myFixture.addFileToProject(
            "java/lang/String.java",
            """
package java.lang;

public final class String {
}
"""
        )
    }

    private fun declarationOf(literal: PsiLiteralExpression): SpelDeclaration =
        declarationsOf(literal).first()

    private fun declarationsOf(literal: PsiLiteralExpression): List<SpelDeclaration> {
        val reference = literal.references.firstOrNull()
        assertNotNull("未建立引用：${literal.text}", reference)
        val targets = if (reference is PsiPolyVariantReference) {
            reference.multiResolve(false).mapNotNull { it.element as? SpelDeclaration }
        } else {
            listOfNotNull(reference!!.resolve() as? SpelDeclaration)
        }
        assertFalse("引用未解析：${literal.text}", targets.isEmpty())
        return targets
    }

    private fun highlightsContaining(text: String): List<HighlightInfo> =
        myFixture.doHighlighting().filter { it.description?.contains(text) == true }
}
