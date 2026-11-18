package com.luckyframework.idea.spel.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 跳转偏移的回归测试。
 *
 * Java 方法/类/字段的 `textRange.startOffset` 指向声明前的 Javadoc 注释，
 * 平台导航用的是 `textOffset`（名称标识符位置）。本类逐一覆盖各类跳转目标，
 * 确保偏移落在名称上而不是注释上。
 */
class SpelNavigationOffsetTest : SymbolFixtureTestCase() {

    /** 类型引用 `T(...)` 跳到类名，不能落在类注释上。 */
    fun testTypeReferenceTargetsClassName() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(p.Item)}")
    void call() {
    }
}

/**
 * 条目。
 */
class Item {
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("T(p.Item)"), "p.Item")

        assertEquals("类型", declaration.kindLabel)
        val item = declaration.target as PsiClass
        assertEquals(
            "跳转偏移必须落在类名上",
            item.nameIdentifier!!.textRange.startOffset,
            declaration.textOffset
        )
    }

    /** 字段引用跳到字段名，不能落在字段注释上。 */
    fun testFieldReferenceTargetsFieldName() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{item.title}")
    void call(Item item) {
    }
}

class Item {
    /**
     * 标题。
     */
    public String title;
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("item.title"), "title")

        assertEquals("字段", declaration.kindLabel)
        val field = declaration.target as PsiField
        assertEquals(
            "跳转偏移必须落在字段名上",
            field.nameIdentifier!!.textRange.startOffset,
            declaration.textOffset
        )
    }

    /** `$this$` 的声明是所在类：跳到类名，不能落在类注释上。 */
    fun testThisVariableTargetsClassName() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

/**
 * 接口声明。
 */
class Api {
    @HttpRequest(url = "#{${'$'}this${'$'}}")
    void call() {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("${'$'}this${'$'}"), "${'$'}this${'$'}")

        assertEquals("内置变量", declaration.kindLabel)
        val api = declaration.target as PsiClass
        assertEquals("Api", api.name)
        assertEquals(
            "跳转偏移必须落在类名上",
            api.nameIdentifier!!.textRange.startOffset,
            declaration.textOffset
        )
    }

    /**
     * `@Query(常量)` 的名称来自常量引用：跳到引用表达式本身。
     *
     * 这类目标没有可定位的名称内容，`sourceRange` 必须为 null，由目标自身的
     * `textOffset` 定位；传空范围会被当成「声明起点」。
     */
    fun testConstantParamNameTargetsReferenceExpression() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {
    private static final String NAME = "req";

    @HttpRequest(url = "#{req}")
    void call(@Query(NAME) String request) {
    }
}
"""
        )
        val declaration = resolveDeclaration(literalContaining("#{req}"), "req")

        assertEquals("方法参数", declaration.kindLabel)
        val target = declaration.target as PsiReferenceExpression
        assertEquals("NAME", target.text)
        assertEquals(
            "跳转偏移必须落在常量引用处",
            target.textRange.startOffset,
            declaration.textOffset
        )
    }

    /** 空范围不携带内容，等价于「声明本身」：仍使用名称标识符偏移。 */
    fun testEmptySourceRangeFallsBackToDeclarationName() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    /**
     * 获取URL
     */
    @HttpRequest(url = "#{url()}")
    public static String url() {
        return "";
    }
}
"""
        )
        val method = methodNamed("url")
        val declaration = SpelDeclaration(
            project = project,
            name = "url",
            kindLabel = "方法",
            target = method,
            sourceRange = TextRange(0, 0),
            anchor = method
        )

        assertEquals(
            "空范围不能退回声明起点（Javadoc），必须落在方法名上",
            method.nameIdentifier!!.textRange.startOffset,
            declaration.textOffset
        )
    }

    /** 字面量内的显式偏移仍跳过起始引号，不被空范围兜底改写。 */
    fun testLiteralSourceRangeStillSkipsOpeningQuote() {
        configure(
            """
package p;

import com.luckyframework.reflect.Param;

class Api {
    void call(@Param("keyword") String q) {
    }
}
"""
        )
        val parameter = methodNamed("call").parameterList.parameters[0]
        val literal = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiLiteralExpression::class.java)
            .first { it.value == "keyword" }
        val declaration = SpelDeclaration(
            project = project,
            name = "keyword",
            kindLabel = "方法参数",
            target = literal,
            sourceRange = TextRange(1, 1),
            anchor = parameter
        )

        assertEquals(
            "字面量内偏移应跳过起始引号",
            literal.textRange.startOffset + 1,
            declaration.textOffset
        )
    }

    private fun literalContaining(marker: String): PsiLiteralExpression =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiLiteralExpression::class.java)
            .first { it.text.contains(marker) }

    private fun resolveDeclaration(literal: PsiLiteralExpression, text: String): SpelDeclaration {
        val reference = literal.references.first { reference ->
            literal.text.substring(reference.rangeInElement.startOffset, reference.rangeInElement.endOffset) == text
        }
        val resolved = reference.resolve()
        assertNotNull("引用未解析：$text", resolved)
        return resolved as SpelDeclaration
    }
}
