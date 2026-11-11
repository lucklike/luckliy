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
        for (name in listOf("UnresolvedSpelSymbol", "SpelImportFormat", "SpelLifecycleMisuse")) {
            assertNotNull(
                "缺少检查描述 $name.html",
                javaClass.getResource("/inspectionDescriptions/$name.html")
            )
        }
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
