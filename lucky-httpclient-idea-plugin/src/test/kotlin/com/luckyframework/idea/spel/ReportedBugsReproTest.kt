package com.luckyframework.idea.spel

import com.intellij.lang.annotation.HighlightSeverity
import com.luckyframework.idea.spel.inspection.SpelMemberAccessInspection
import com.luckyframework.idea.spel.inspection.UnresolvedSpelSymbolInspection
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 复现用户报告的四个问题，先失败后修复。
 */
class ReportedBugsReproTest : SymbolFixtureTestCase() {

    /** 问题1：集合过滤表达式中的内置函数不能被当成元素成员方法。 */
    fun testBuiltinFunctionInsideSelectionIsNotMemberError() {
        myFixture.addFileToProject(
            "com/example/DemoUser.java",
            """
package com.example;

public class DemoUser {
    private String address;

    public String getAddress() {
        return address;
    }
}
"""
        )
        configure(
            """
package p;

import java.util.List;
import com.example.DemoUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[str_eq(#this.address, 'a')]<caret>}")
    void call(List<DemoUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection(), UnresolvedSpelSymbolInspection())

        assertTrue(
            "内置函数被当成元素方法: ${descriptions()}",
            highlightsContaining("不存在方法").isEmpty()
        )
        assertTrue(
            "内置函数被当成未解析符号: ${descriptions()}",
            highlightsContaining("未解析的 SpEL 函数").isEmpty()
        )
    }

    /** 问题4：`$req$` 的成员补全必须命中框架 Request 类型。 */
    fun testRequestVariableMembersCompletion() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/core/meta/Request.java",
            """
package com.luckyframework.httpclient.core.meta;

public interface Request {
    String getUrl();
    String getPath();
}
"""
        )
        configure(
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
        myFixture.completeBasic()
        val names = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertTrue("缺少 getUrl: $names", names.any { it.startsWith("getUrl") })
        assertTrue("缺少 getPath: $names", names.any { it.startsWith("getPath") })
    }

    /** 问题2：`@SpelAttribute(kind = LITERAL)` 的属性不产生任何诊断与高亮。 */
    fun testLiteralAttributeProducesNoDiagnostics() {
        myFixture.addFileToProject(
            "com/example/DemoLiteral.java",
            """
package com.example;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

public @interface DemoLiteral {

    @SpelAttribute(kind = SpelKind.LITERAL)
    String value() default "";
}
"""
        )
        configure(
            """
package p;

import com.example.DemoLiteral;

class Api {
    @DemoLiteral("#{${'$'}notDefined} 以及 { 未闭合<caret>")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection(), UnresolvedSpelSymbolInspection())

        assertTrue(
            "字面量属性不应有任何诊断: ${descriptions()}",
            myFixture.doHighlighting().none {
                it.severity >= com.intellij.lang.annotation.HighlightSeverity.WARNING
            }
        )
    }

    /** 问题3：`$status$` 是框架注入的只读根变量，赋值一定不生效，必须报错。 */
    fun testSingleEqualsIsSyntaxError() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}status${'$'} = 400<caret>}")
    void call() {
    }
}
"""
        )

        assertTrue(
            "单等号未报错: ${descriptions()}",
            myFixture.doHighlighting().any {
                it.severity == com.intellij.lang.annotation.HighlightSeverity.ERROR
            }
        )
    }

    /**
     * 问题2（第二轮）：集合过滤表达式内输入函数前缀时，必须列出内置函数候选。
     * 
     * `users.?[str_e]` 是选择方括号内的裸调用，运行期先查函数表再按元素方法
     * 解析，补全若只列元素成员就找不到 `str_eq`。
     */
    fun testBuiltinFunctionCompletionInsideSelection() {
        myFixture.addFileToProject(
            "com/example/DemoUser.java",
            """
package com.example;

public class DemoUser {
    private String address;

    public String getAddress() {
        return address;
    }
}
"""
        )
        configure(
            """
package p;

import java.util.List;
import com.example.DemoUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[str_e<caret>]}")
    void call(List<DemoUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection(), UnresolvedSpelSymbolInspection())
        myFixture.completeBasic()
        val names = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertTrue("过滤表达式内缺少内置函数 str_eq 候选: $names", names.any { it.startsWith("str_eq") })
        assertTrue("内置函数不应报未解析: ${descriptions()}", highlightsContaining("不存在方法").isEmpty())
    }

    /**
     * 问题4（第二轮）：未标注 `@SpelAttribute` 的属性不参与补全。
     * 
     * 运行期框架不会求值这些属性，补全也不能把 `str_eq` 等函数列出来，
     * 否则用户会被引导写出运行期不生效的表达式。
     */
    fun testUnannotatedAttributeProducesNoCompletion() {
        addUnannotatedAttribute()
        configure(
            """
package p;

import com.example.DemoPlain;

class Api {
    @DemoPlain("#{str_e<caret>}")
    void call() {
    }
}
"""
        )
        myFixture.completeBasic()
        val names = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertTrue(
            "未标注属性的属性不应出现 SpEL 函数候选: $names",
            names.none { it.startsWith("str_eq") || it.startsWith("str_ne") }
        )
    }

    /**
     * 问题4（第二轮）：未标注 `@SpelAttribute` 的属性不产生任何 SpEL 着色。
     * 
     * 着色是 silent annotation，必须与语法错误一起拦截，不能只拦截诊断。
     */
    fun testUnannotatedAttributeProducesNoHighlighting() {
        addUnannotatedAttribute()
        configure(
            """
package p;

import com.example.DemoPlain;

class Api {
    @DemoPlain("#{str_eq(a, 'b')}")
    void call() {
    }
}
"""
        )
        val expressionStart = myFixture.file.text.indexOf("#{")
        val highlights = myFixture.doHighlighting().filter {
            it.forcedTextAttributesKey != null && it.startOffset >= expressionStart
        }

        assertTrue(
            "未标注 '@SpelAttribute' 的属性不应有 SpEL 着色: " +
                highlights.map { "${it.startOffset}-${it.endOffset}" },
            highlights.isEmpty()
        )
    }

    /** 完全未标注 `@SpelAttribute` 的自定义注解。 */
    private fun addUnannotatedAttribute() {
        myFixture.addFileToProject(
            "com/example/DemoPlain.java",
            """
package com.example;

public @interface DemoPlain {
    String value() default "";
}
"""
        )
    }

    private fun highlightsContaining(text: String) =
        myFixture.doHighlighting().filter { it.description?.contains(text) == true }

    private fun descriptions(): String =
        myFixture.doHighlighting().mapNotNull { it.description }.toString()
}
