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
