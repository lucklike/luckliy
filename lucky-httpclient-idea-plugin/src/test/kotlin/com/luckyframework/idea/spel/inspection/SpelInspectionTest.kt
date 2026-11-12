package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.extensions.Extensions
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 12.1–12.5、12.7、12.8：检查、快速修复与描述资源。
 */
class SpelInspectionTest : SymbolFixtureTestCase() {

    fun testUnresolvedVariableWarning() {
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
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("未解析的 SpEL 变量")

        assertFalse("应产生未解析变量警告", infos.isEmpty())
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
        assertTrue(infos.first().description!!.contains("#notDefined"))
    }

    fun testInspectionRegisteredInPluginXml() {
        val extensions = Extensions.getExtensions(LocalInspectionEP.LOCAL_INSPECTION).toList()

        val unresolved = extensions.firstOrNull {
            it.implementationClass == UnresolvedSpelSymbolInspection::class.java.name
        }
        assertNotNull("plugin.xml 未注册未解析检查", unresolved)
        assertEquals("WARNING", unresolved!!.level)
        assertTrue(unresolved.enabledByDefault)

        val functionCall = extensions.firstOrNull {
            it.implementationClass == SpelFunctionCallInspection::class.java.name
        }
        assertNotNull("plugin.xml 未注册函数调用检查", functionCall)
        assertEquals("ERROR", functionCall!!.level)
        assertTrue(functionCall.enabledByDefault)

        val format = extensions.firstOrNull {
            it.implementationClass == SpelImportFormatInspection::class.java.name
        }
        assertNotNull("plugin.xml 未注册导入格式检查", format)
        assertEquals("ERROR", format!!.level)

        val lifecycle = extensions.firstOrNull {
            it.implementationClass == SpelLifecycleMisuseInspection::class.java.name
        }
        assertNotNull("plugin.xml 未注册生命周期检查", lifecycle)
        assertEquals("WARNING", lifecycle!!.level)
    }

    fun testImportMissingSeparatorIsError() {
        configureImport("""@SpELImport(var = "token<caret>")""")

        val infos = highlightsContaining("缺少分隔符")

        assertFalse("应报告缺少分隔符", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    fun testImportEmptyNameIsError() {
        configureImport("""@SpELImport(var = "<caret>=abc")""")

        val infos = highlightsContaining("变量名不能为空")

        assertFalse("应报告空变量名", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    fun testImportDuplicateDeclarationIsError() {
        configureImport("""@SpELImport(var = {"token=#{1}", "token=#{2}<caret>"})""")

        val infos = highlightsContaining("重复声明变量")

        assertFalse("应报告重复声明", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    fun testImportOverridesBuiltinIsWarning() {
        configureImport("""@SpELImport(root = "${'$'}req${'$'}=#{1}<caret>")""")

        val infos = highlightsContaining("覆盖内置变量")

        assertFalse("应报告覆盖内置变量", infos.isEmpty())
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
    }

    fun testLifecycleMisuseWarning() {
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
        myFixture.enableInspections(SpelLifecycleMisuseInspection())

        val infos = highlightsContaining("仅在响应阶段可用")

        assertFalse("应报告生命周期误用", infos.isEmpty())
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
        assertTrue(infos.first().description!!.contains("\$body\$"))
    }

    fun testCreateVariableQuickFix() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#token<caret>}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
        myFixture.doHighlighting()

        launchFix("创建 SpEL 变量声明（当前方法）")

        val text = myFixture.file.text
        assertTrue("应插入 @SpELImport: $text", text.contains("@SpELImport"))
        assertTrue("应保留变量名 token: $text", text.contains("token="))
        assertTrue("原表达式不应改变: $text", text.contains("#{#token}"))
        assertTrue(highlightsContaining("未解析的 SpEL 变量").isEmpty())
    }

    fun testImportFunctionClassQuickFix() {
        myFixture.addFileToProject(
            "com/example/HashUtils.java",
            """
package com.example;

public class HashUtils {
    public static String myHash(String text) {
        return text;
    }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#myHash<caret>('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
        myFixture.doHighlighting()

        launchFix("导入函数类")

        val text = myFixture.file.text
        assertTrue("应插入 @SpELImport: $text", text.contains("@SpELImport"))
        assertTrue("应导入 HashUtils: $text", text.contains("HashUtils"))
        assertTrue(highlightsContaining("未解析的 SpEL 函数").isEmpty())
    }

    /** 当前类静态方法作为函数时不应报告未解析。 */
    fun testClassStaticFunctionIsResolved() {
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
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        assertTrue(
            "当前类静态方法应被解析: ${highlightsContaining("未解析的 SpEL 函数")}",
            highlightsContaining("未解析的 SpEL 函数").isEmpty()
        )
    }

    fun testSuppressUnresolvedWarning() {
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
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
        assertFalse(highlightsContaining("未解析的 SpEL 变量").isEmpty())

        launchFix("抑制此提示")

        val text = myFixture.file.text
        assertTrue(
            "源码应包含可识别的抑制标记: $text",
            text.contains("SuppressWarnings(\"UnresolvedSpelSymbol\")")
        )
        assertTrue("抑制后不应再报告", highlightsContaining("未解析的 SpEL 变量").isEmpty())
    }

    fun testInspectionDescriptionsLoad() {
        for (name in listOf("UnresolvedSpelSymbol", "SpelFunctionCall", "SpelImportFormat", "SpelLifecycleMisuse")) {
            assertNotNull(
                "缺少检查描述 $name.html",
                javaClass.getResource("/inspectionDescriptions/$name.html")
            )
        }
    }

    /** 字符串字面量传给 Integer 形参：参数类型不匹配。 */
    fun testFunctionArgumentTypeMismatch() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add('a', 3)}")
    void call() {
    }

    public static Integer add(Integer a, Integer b) {
        return a + b;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应报告参数类型不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue("应提示目标类型: ${infos.first().description}", infos.first().description!!.contains("Integer"))
        assertTrue("应提示实际类型: ${infos.first().description}", infos.first().description!!.contains("String"))
    }

    /** 方法参数注入的变量同样参与类型检查。 */
    fun testParameterVariableTypeMismatch() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(card, 1)}")
    void call(String card) {
    }

    public static Integer add(Integer a, Integer b) {
        return a + b;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应报告参数类型不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(infos.first().description!!.contains("String"))
    }

    /** 参数个数与所有重载都不匹配时按错误报告。 */
    fun testFunctionArgumentCountMismatch() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(1)}")
    void call() {
    }

    public static Integer add(Integer a, Integer b) {
        return a + b;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数数量不匹配")

        assertFalse("应报告参数数量不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue("应提示需要的个数: ${infos.first().description}", infos.first().description!!.contains("2 个"))
    }

    /** 类型一致时不产生任何函数调用问题。 */
    fun testMatchingFunctionArgumentsPass() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(1, 2)}")
    void call() {
    }

    public static Integer add(Integer a, Integer b) {
        return a + b;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        assertTrue(
            "类型一致时不应提示: ${highlightsContaining("参数类型不匹配")}",
            highlightsContaining("参数类型不匹配").isEmpty()
        )
        assertTrue(highlightsContaining("参数数量不匹配").isEmpty())
    }

    /** 内置函数的参数个数检查复用符号表签名。 */
    fun testBuiltinFunctionArgumentCountMismatch() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{temp_encode('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数数量不匹配")

        assertFalse("应报告内置函数参数数量不匹配", infos.isEmpty())
        assertTrue(infos.first().description!!.contains("temp_encode"))
    }

    /** 方法调用结果的类型参与检查：`getBaseUrl()` 返回 Text，传给 Integer 形参应报错。 */
    fun testMethodChainResultTypeMismatch() {
        addRequestStub()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(${'$'}req${'$'}.getBaseUrl(), 3)}")
    void call() {
    }

    public static Integer add(Integer a, Integer b) {
        return a + b;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应报告方法链结果的类型不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue("应提示实际类型 Text: ${infos.first().description}", infos.first().description!!.contains("Text"))
    }

    /** 参数表达式结果无法推导时只给警告，不声称运行期一定失败。 */
    fun testUnknownArgumentExpressionIsWarning() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(1 + 2, 3)}")
    void call() {
    }

    public static Integer add(Integer a, Integer b) {
        return a + b;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("无法推导参数表达式")

        assertFalse("无法推导时应给出警告", infos.isEmpty())
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
        assertTrue("应提示形参类型: ${infos.first().description}", infos.first().description!!.contains("Integer"))
    }

    /** `T(X)` 的值类型是 `Class<X>`，提示中不能直接显示 X。 */
    fun testTypeReferenceArgumentReportsClassType() {
        addRequestStub()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(1, T(com.luckyframework.httpclient.core.meta.Request))}")
    void call() {
    }

    public static Integer add(Integer a, Integer b) {
        return a + b;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应报告 T() 结果与 Integer 形参不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "实际类型应显示为 Class<Request>: ${infos.first().description}",
            infos.first().description!!.contains("Class<Request>")
        )
    }

    private fun addRequestStub() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/core/meta/Text.java",
            """
package com.luckyframework.httpclient.core.meta;

public class Text {
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

    private fun configureImport(annotation: String) {
        myFixture.configureByText(
            "Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    $annotation
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelImportFormatInspection())
    }

    private fun highlightsContaining(text: String): List<HighlightInfo> =
        myFixture.doHighlighting().filter { it.description?.contains(text) == true }

    private fun launchFix(text: String) {
        val fix = myFixture.getAllQuickFixes().firstOrNull {
            it.text.contains(text) || it.familyName.contains(text)
        }
        assertNotNull("未找到快速修复「$text」", fix)
        myFixture.launchAction(fix!!)
    }
}
