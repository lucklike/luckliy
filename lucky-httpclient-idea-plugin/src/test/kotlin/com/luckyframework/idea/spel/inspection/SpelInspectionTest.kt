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

        val memberAccess = extensions.firstOrNull {
            it.implementationClass == SpelMemberAccessInspection::class.java.name
        }
        assertNotNull("plugin.xml 未注册成员访问检查", memberAccess)
        assertEquals("ERROR", memberAccess!!.level)
        assertTrue(memberAccess.enabledByDefault)

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

        val unresolvedType = extensions.firstOrNull {
            it.implementationClass == SpelUnresolvedTypeInspection::class.java.name
        }
        assertNotNull("plugin.xml 未注册类型引用检查", unresolvedType)
        assertEquals("ERROR", unresolvedType!!.level)
        assertTrue(unresolvedType.enabledByDefault)

        val selectionCondition = extensions.firstOrNull {
            it.implementationClass == SpelSelectionConditionInspection::class.java.name
        }
        assertNotNull("plugin.xml 未注册过滤条件检查", selectionCondition)
        assertEquals("ERROR", selectionCondition!!.level)
        assertTrue(selectionCondition.enabledByDefault)
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

import com.luckyframework.httpclient.proxy.annotations.PrintLog;

class Api {
    @PrintLog(reqCondition = "#{${'$'}body${'$'}<caret>}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelLifecycleMisuseInspection())

        val infos = highlightsContaining("仅在响应初始化阶段可用")

        assertFalse("应报告生命周期误用", infos.isEmpty())
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
        assertTrue(infos.first().description!!.contains("\$body\$"))
    }

    /** `@If(condition = "#{false}")` 的类确定未导入，引用其函数必须报错。 */
    fun testFalseConditionImportedFunctionIsError() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{false}", classes = MyUtils.class))
    @HttpRequest(url = "#{#sha<caret>('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("condition 求值为 false")
        assertFalse("条件为 false 时应报类未导入", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应说明类未被导入而不是泛化的未解析",
            highlightsContaining("未解析的 SpEL 函数").isEmpty()
        )
    }

    /** condition 静态为 false 时，命名空间的引用同样报错。 */
    fun testFalseConditionImportedNamespaceIsError() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{false}", classes = MyUtils.class))
    @HttpRequest(url = "#{#crypto<caret>.md5('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("命名空间 '#crypto' 来自条件导入")
        assertFalse("条件为 false 时应报命名空间不可用", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "不应再报泛化的未解析命名空间",
            highlightsContaining("未解析的 SpEL 命名空间").isEmpty()
        )
    }

    /** `@If(condition = "#{true}")` 的类正常导入，函数可解析到方法。 */
    fun testTrueConditionImportedFunctionResolves() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{true}", classes = MyUtils.class))
    @HttpRequest(url = "#{#sha<caret>('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        assertTrue(
            "条件为 true 时不应报未解析",
            highlightsContaining("未解析的 SpEL 函数").isEmpty()
        )
    }

    /** 常量比较条件可静态求值为 true，类正常导入。 */
    fun testComparisonTrueConditionResolves() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{1==1}", classes = MyUtils.class))
    @HttpRequest(url = "#{#sha<caret>('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        assertTrue(
            "#{1==1} 求值为 true，不应报未解析或条件警告",
            highlightsContaining("未解析的 SpEL 函数").isEmpty()
        )
        assertTrue(
            "条件已确定，不应提示运行期可能不存在",
            highlightsContaining("condition 无法静态确定").isEmpty()
        )
    }

    /** 常量比较条件可静态求值为 false，引用其函数报错。 */
    fun testComparisonFalseConditionReportsError() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{1!=1}", classes = MyUtils.class))
    @HttpRequest(url = "#{#sha<caret>('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("condition 求值为 false")
        assertFalse("#{1!=1} 求值为 false，应报类未导入", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "不应退化为泛化的未解析",
            highlightsContaining("未解析的 SpEL 函数").isEmpty()
        )
    }

    /** condition 无法静态确定时不能报未解析，只能提示运行期可能不存在。 */
    fun testUnknownConditionImportedFunctionWarns() {
        // 无命名空间的函数类：`#fn(...)` 才是它的可用写法
        myFixture.addFileToProject(
            "com/example/CondUtils.java",
            """
package com.example;

public class CondUtils {
    public static String condHash(String text) {
        return text;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.CondUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{#flag}", classes = CondUtils.class))
    @HttpRequest(url = "#{#condHash<caret>('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("condition 无法静态确定")
        assertFalse("应提示条件导入可能未生效", infos.isEmpty())
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
        assertTrue(
            "条件未知时不能报未解析",
            highlightsContaining("未解析的 SpEL 函数").isEmpty()
        )
    }

    /** 条件导入的命名空间同样只能提示警告，不影响成员解析。 */
    fun testUnknownConditionImportedNamespaceWarns() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{#flag}", classes = MyUtils.class))
    @HttpRequest(url = "#{#crypto<caret>.md5('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("命名空间 '#crypto' 来自条件导入")
        assertFalse("应提示条件导入的命名空间", infos.isEmpty())
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
        assertTrue(
            "命名空间成员仍应可解析",
            highlightsContaining("未解析的 SpEL 命名空间").isEmpty()
        )
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

    /**
     * 方法参数是 Root 变量，只能裸名称引用：`#userId` 提示正确形式，
     * 快速修复去掉 `#`。
     */
    fun testRootVariableWithHashReportsFormErrorAndFixes() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId<caret>}")
    void call(String userId) {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
        myFixture.doHighlighting()

        val infos = highlightsContaining("Root 变量 'userId' 只能用裸名称引用")
        assertFalse("应提示参数的正确引用形式", infos.isEmpty())
        // 书写形式错误直接报错，不是警告
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)

        launchFix("改为裸名称引用")

        val text = myFixture.file.text
        assertTrue("应去掉 #: $text", text.contains("#{userId}"))
        assertTrue(highlightsContaining("Root 变量 'userId'").isEmpty())
    }

    /** 普通变量（`@SpELImport(var = ...)`）必须用 `#name` 引用，裸名称提示正确形式。 */
    fun testNormalVariableWithoutHashReportsFormErrorAndFixes() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "token=1")
class Api {
    @HttpRequest(url = "#{token<caret>}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
        myFixture.doHighlighting()

        val infos = highlightsContaining("变量 'token' 必须使用 # 引用")
        assertFalse("应提示普通变量的正确引用形式", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)

        launchFix("改为 #name 引用")

        val text = myFixture.file.text
        assertTrue("应补上 #: $text", text.contains("#{#token}"))
        assertTrue(highlightsContaining("变量 'token'").isEmpty())
    }

    /** 默认函数类已注册，`#time()` 与 `time()` 两种写法都可用。 */
    fun testRegisteredBuiltinFunctionWithHashIsValid() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#time<caret>()}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
        myFixture.doHighlighting()

        assertTrue(
            "已注册的内置函数不应提示只能裸名称调用",
            highlightsContaining("只能以 time").isEmpty()
        )
        assertTrue(
            "#time() 应能解析",
            highlightsContaining("未解析的 SpEL 函数").isEmpty()
        )
    }

    /**
     * 命名空间函数写成 `#sha(...)`：提示补命名空间，修复后为
     * `#crypto.sha(...)`，而不是误导去 `#` 调用内置/不存在的平铺函数。
     */
    fun testNamespacedFunctionWithHashSuggestsQualifiedForm() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{#sha<caret>('a')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
        myFixture.doHighlighting()

        val infos = highlightsContaining("属于命名空间 '#crypto'")
        assertFalse("应提示命名空间函数的正确调用形式", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)

        launchFix("改为 #crypto.sha 引用")

        val text = myFixture.file.text
        assertTrue("应补上命名空间: $text", text.contains("#{#crypto.sha('a')}"))
        assertTrue(highlightsContaining("属于命名空间").isEmpty())
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

    /** 函数名属性整段就是函数名，引用不存在的函数必须报错而不是警告。 */
    fun testUnresolvedFunctionNameIsError() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(func = "notExists")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("未解析的 SpEL 函数")

        assertFalse("应报告未解析函数", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue("应标出函数名", infos.first().description!!.contains("notExists"))
    }

    /** 同名方法只登记了别名：提示改用别名，且不提供按方法名导入类的修复。 */
    fun testFunctionNameWithAliasSuggestsAliasFix() {
        myFixture.addFileToProject(
            "com/example/FuncDemoApi.java",
            """
package com.example;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

public class FuncDemoApi {
    @FunctionAlias("get_url")
    public static String getUrl() {
        return "";
    }
}
"""
        )
        configure(
            """
package p;

import com.example.FuncDemoApi;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(FuncDemoApi.class)
class Api {
    @HttpRequest(func = "getUrl")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("只登记了别名")
        assertFalse("应提示同名方法只登记了别名", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示别名: ${infos.first().description}",
            infos.first().description!!.contains("get_url")
        )

        // 按方法名导入类不会解析到只登记别名的方法，不应再提供该修复
        val fixTexts = myFixture.getAllQuickFixes().map { it.text + "/" + it.familyName }
        assertTrue("应提供别名修复: $fixTexts", fixTexts.any { it.contains("改用") })
        assertTrue("不应提供导入函数类修复: $fixTexts", fixTexts.none { it.contains("导入函数类") })

        launchFix("改用别名")

        assertTrue(
            "函数名属性应改为别名: ${myFixture.file.text}",
            myFixture.file.text.contains("func = \"get_url\"")
        )
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
        for (name in listOf(
            "UnresolvedSpelSymbol", "SpelFunctionCall", "SpelImportFormat",
            "SpelLifecycleMisuse", "SpelMemberAccess", "SpelUnresolvedType",
            "SpelSelectionCondition"
        )) {
            assertNotNull(
                "缺少检查描述 $name.html",
                javaClass.getResource("/inspectionDescriptions/$name.html")
            )
        }
    }

    /** `.` 访问不存在的属性：接收者类型明确时按错误报告。 */
    fun testMissingPropertyOnTypedReceiverIsError() {
        addUserStub()
        configure(
            """
package p;

import com.example.User;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{user.d}")
    void call(User user) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在属性 'd'")

        assertFalse("应报告不存在的属性", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示声明类: ${infos.first().description}",
            infos.first().description!!.contains("User")
        )
    }

    /** `.` 调用不存在的方法：接收者类型明确时按错误报告。 */
    fun testMissingMethodOnTypedReceiverIsError() {
        addUserStub()
        configure(
            """
package p;

import com.example.User;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{user.m4()}")
    void call(User user) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在方法 'm4'")

        assertFalse("应报告不存在的方法", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示声明类: ${infos.first().description}",
            infos.first().description!!.contains("User")
        )
    }

    /** 安全导航 `?.` 后的成员同样参与检查。 */
    fun testMissingPropertyAfterSafeNavigationIsError() {
        addUserStub()
        configure(
            """
package p;

import com.example.User;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{user?.d}")
    void call(User user) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在属性 'd'")

        assertFalse("安全导航后不存在的属性应报告", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    /** 已声明的字段、getter 属性与方法不产生成员问题。 */
    fun testExistingMembersPass() {
        addUserStub()
        configure(
            """
package p;

import com.example.User;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{user.a} #{user.nick} #{user.m1()}")
    void call(User user) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "已声明的成员不应提示: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
        assertTrue(highlightsContaining("无法校验成员").isEmpty())
    }

    /** `$this$` 的类型是当前类，访问不存在的成员按错误报告。 */
    fun testThisVariableMissingMethodIsError() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(value = "#{${'$'}this${'$'}.bag3()}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在方法 'bag3'")

        assertFalse("应报告当前类上不存在的方法", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示当前类: ${infos.first().description}",
            infos.first().description!!.contains("Api")
        )
    }

    /** 类型不明确的 Object 接收者只给警告，不断言一定失败。 */
    fun testObjectReceiverIsWarning() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{data.d}")
    void call(Object data) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("无法校验成员 'd'")

        assertFalse("Object 接收者应给出警告", infos.isEmpty())
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
    }

    /** Object 上确定存在的成员（toString 等）不产生警告。 */
    fun testObjectKnownMemberPasses() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{data.toString()}")
    void call(Object data) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "Object 自身成员不应提示: ${highlightsContaining("无法校验成员")}",
            highlightsContaining("无法校验成员").isEmpty()
        )
    }

    /** Map 的成员名是动态 key，不做成员检查。 */
    fun testMapReceiverPasses() {
        myFixture.addFileToProject(
            "java/util/Map.java",
            """
package java.util;

public interface Map<K, V> {
    V get(Object key);
}
"""
        )
        configure(
            """
package p;

import java.util.Map;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{query.foo}")
    void call(Map<String, Object> query) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "Map 的成员不应提示: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
    }

    /** 集合的 size/length 属性写法保守放行。 */
    fun testCollectionSizePropertyPasses() {
        myFixture.addFileToProject(
            "java/util/Collection.java",
            """
package java.util;

public interface Collection<E> {
    int size();
}
"""
        )
        configure(
            """
package p;

import java.util.Collection;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{names.size}")
    void call(Collection<String> names) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "集合 size 属性不应提示: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
    }

    /** 泛型类型变量接收者的成员取决于运行期实参，不做成员检查。 */
    fun testTypeVariableReceiverPasses() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api<T> {
    @HttpRequest(url = "#{item.foo}")
    void call(T item) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "类型变量接收者不应提示: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
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

    /**
     * 数组实参不能传给标量形参：`take(bytesOf(name))` 中 `bytesOf` 返回
     * `byte[]`，与 `take(String)` 不兼容。根因与参数注入检查一致：
     * 不能把剥掉 `[]` 的短名当标量比较。
     */
    fun testArrayArgumentIntoStringParameterIsReported() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{take(bytesOf(name))}")
    void call(String name) {
    }

    public static byte[] bytesOf(Object value) {
        return new byte[0];
    }

    public static String take(String value) {
        return value;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")
        val messages = infos.mapNotNull { it.description }

        assertFalse("byte[] 传给 String 形参应报错: $messages", infos.isEmpty())
        assertTrue("应提示实际类型 byte[]: $messages", messages.first().contains("byte[]"))
    }

    /**
     * 数组实参的检查不绑定 String：数组传数值/布尔形参、标量传数组形参、
     * 组件类型不兼容的数组之间，都必须报告。
     */
    fun testArrayArgumentShapeIsCheckedForAllParameterKinds() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{mix(bytesOf(name), bytesOf(name), wordsOf(name), wordsOf(name), name)}")
    void call(String name) {
    }

    public static byte[] bytesOf(Object value) {
        return new byte[0];
    }

    public static String[] wordsOf(Object value) {
        return new String[0];
    }

    public static void mix(Integer number, Boolean flag, int[] numbers, Integer scalar, String[] names) {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")
        val messages = infos.mapNotNull { it.description }

        assertEquals("五个实参全部不兼容，应全部报告: $messages", 5, infos.size)
        assertTrue("byte[] 传 Integer 应报告: $messages", messages.any { it.contains("Integer") && it.contains("byte[]") })
        assertTrue("byte[] 传 Boolean 应报告: $messages", messages.any { it.contains("Boolean") })
        assertTrue("String[] 传 int[] 应报告: $messages", messages.any { it.contains("int[]") && it.contains("String[]") })
        assertTrue("标量传数组形参应报告: $messages", messages.any { it.contains("String[]") && it.contains("String") })
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

    /** 函数结果作为接收者：成员方法的参数数量按实际类型检查（`Bag.get` 只接受 1 个参数）。 */
    fun testFunctionResultMethodArgumentCountMismatch() {
        addBagStub()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(1, 3).get('3', 3)}")
    void call() {
    }

    public static com.example.Bag<Integer> add(Integer a, Integer b) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数数量不匹配")

        assertFalse("应报告成员方法的参数数量不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue("应提示方法名: ${infos.first().description}", infos.first().description!!.contains("get"))
    }

    /** 成员方法的参数类型同样参与检查。 */
    fun testFunctionResultMethodArgumentTypeMismatch() {
        addBagStub()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(1, 3).get('a')}")
    void call() {
    }

    public static com.example.Bag<Integer> add(Integer a, Integer b) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应报告成员方法的参数类型不匹配", infos.isEmpty())
        assertTrue("应提示形参类型 int: ${infos.first().description}", infos.first().description!!.contains("int"))
        assertTrue("应提示实际类型 String: ${infos.first().description}", infos.first().description!!.contains("String"))
    }

    /** 成员方法参数匹配时不再产生函数调用问题。 */
    fun testFunctionResultMethodArgumentsPass() {
        addBagStub()
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
        myFixture.enableInspections(SpelFunctionCallInspection())

        assertTrue(
            "参数匹配时不应提示: ${highlightsContaining("参数数量不匹配")}",
            highlightsContaining("参数数量不匹配").isEmpty()
        )
        assertTrue(highlightsContaining("参数类型不匹配").isEmpty())
    }

    /** 泛型返回值回填：`Bag<Item>.get(0)` 的成员继续按 Item 检查。 */
    fun testGenericElementTypeFlowsThroughMethodChain() {
        addBagStub()
        myFixture.addFileToProject(
            "com/example/Item.java",
            "package com.example;\n\npublic class Item {\n" +
                "    public String getName() {\n        return \"\";\n    }\n}\n"
        )
        configure(
            """
package p;

import com.example.Item;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(1, 3).get(0).getName('a')}")
    void call() {
    }

    public static com.example.Bag<Item> add(Integer a, Integer b) {
        return null;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数数量不匹配")

        assertFalse("应报告 getName 的参数数量不匹配", infos.isEmpty())
        assertTrue(infos.first().description!!.contains("getName"))
    }

    /** 构造器调用检查：参数个数与所有构造器都不匹配时报错。 */
    fun testConstructorArgumentCountMismatch() {
        myFixture.addFileToProject(
            "com/example/Pair.java",
            """
package com.example;

public class Pair {
    public Pair(int first, int second) {
    }

    public Pair(String text) {
    }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{new com.example.Pair(1, 2, 3)}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("构造器 'Pair' 的参数数量不匹配")

        assertFalse("应报告构造器参数数量不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    /** Bean 接收者的成员方法按声明类检查参数。 */
    fun testBeanMethodArgumentCountMismatch() {
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
    @HttpRequest(url = "#{@userMapper.findByName('a', 'b')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数数量不匹配")

        assertFalse("应报告 Bean 方法的参数数量不匹配", infos.isEmpty())
        assertTrue(infos.first().description!!.contains("findByName"))
    }

    /** `@bean` 方法的实参无法推导时同样要提示，不能静默跳过检查。 */
    fun testBeanMethodUnknownArgumentTypeIsWarning() {
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
    @HttpRequest(value = "#{@userMapper.findByName(#unknown)}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("无法推导参数表达式")

        assertFalse("Bean 方法实参无法推导时应给出警告", infos.isEmpty())
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
        assertTrue("应提示形参类型: ${infos.first().description}", infos.first().description!!.contains("String"))
    }

    /** String 形参传入数字字面量时应报类型不匹配，不能因运行期转换而静默。 */
    fun testBeanMethodNumericArgumentTypeMismatch() {
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
    @HttpRequest(value = "#{@userMapper.findByName(5)}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("String 形参传数字应报错", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue("应提示形参类型: ${infos.first().description}", infos.first().description!!.contains("String"))
        assertTrue("应提示实际类型: ${infos.first().description}", infos.first().description!!.contains("int"))
    }

    /** 构造器的 String 形参同样不接收数字字面量。 */
    fun testConstructorNumericArgumentToStringParameter() {
        myFixture.addFileToProject(
            "com/example/Pair.java",
            """
package com.example;

public class Pair {
    public Pair(int first, int second) {
    }

    public Pair(String text) {
    }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{new com.example.Pair(5)}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("String 构造器形参传数字应报错", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue("应提示形参类型: ${infos.first().description}", infos.first().description!!.contains("String"))
    }

    /** Spring 常见 `<Name>Impl` 命名的 Bean 也要能解析并检查参数。 */
    fun testBeanImplMethodArgumentCountMismatch() {
        myFixture.addFileToProject(
            "com/example/UserMapperImpl.java",
            """
package com.example;

public class UserMapperImpl {
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
    @HttpRequest(url = "#{@userMapper.findByName('a', 'b')}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数数量不匹配")

        assertFalse("<Name>Impl Bean 也应参与参数检查", infos.isEmpty())
        assertTrue(infos.first().description!!.contains("findByName"))
    }

    /** `instanceof T(...)` 中的类型名不能被当成未解析变量。 */
    fun testInstanceofTypeIsNotUnresolvedVariable() {
        myFixture.addFileToProject(
            "com/example/SpelUser.java",
            "package com.example;\npublic class SpelUser {}\n"
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{body instanceof T(com.example.SpelUser)}")
    void call(Object body) {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("未解析的 SpEL 变量")

        assertTrue("instanceof 类型名不应报未解析变量: ${infos.map { it.description }}", infos.isEmpty())
    }

    /** `new` 的泛型实参不能被当成未解析变量。 */
    fun testNewTypeArgumentIsNotUnresolvedVariable() {
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
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        val infos = highlightsContaining("未解析的 SpEL 变量")

        assertTrue("泛型实参不应报未解析变量: ${infos.map { it.description }}", infos.isEmpty())
    }

    /** `T(...)` 括号内的类型不存在时按错误报告。 */
    fun testUnresolvedTypeInTypeReferenceIsError() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(com.example.NotExist)}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelUnresolvedTypeInspection())

        val infos = highlightsContaining("未解析的 SpEL 类型")

        assertFalse("T() 中不存在的类型应报错", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示不存在的类型名: ${infos.first().description}",
            infos.first().description!!.contains("com.example.NotExist")
        )
    }

    /** `new` 之后构造的类型不存在时按错误报告，点链只报一次。 */
    fun testUnresolvedTypeAfterNewIsError() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{new com.example.NotExist()}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelUnresolvedTypeInspection())

        val infos = highlightsContaining("未解析的 SpEL 类型")

        assertEquals("点链只应报告一次: ${infos.map { it.description }}", 1, infos.size)
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(infos.first().description!!.contains("com.example.NotExist"))
    }

    /** `instanceof T(...)` 中的类型不存在时按错误报告。 */
    fun testUnresolvedTypeAfterInstanceofIsError() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{body instanceof T(com.example.NotExist)}")
    void call(Object body) {
    }
}
"""
        )
        myFixture.enableInspections(SpelUnresolvedTypeInspection())

        val infos = highlightsContaining("未解析的 SpEL 类型")

        assertFalse("instanceof 后不存在的类型应报错", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(infos.first().description!!.contains("com.example.NotExist"))
    }

    /** 已存在的类型与 java.lang 短名不产生类型问题。 */
    fun testResolvedTypeReferencesPass() {
        addUserStub()
        myFixture.addFileToProject(
            "java/util/ArrayList.java",
            "package java.util;\npublic class ArrayList<E> {}\n"
        )
        // 测试工程不含 JDK 的 java.lang 类，补 Object 桩模拟真实环境
        myFixture.addFileToProject(
            "java/lang/Object.java",
            "package java.lang;\npublic class Object {}\n"
        )
        configure(
            """
package p;

import com.example.User;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(com.example.User)} #{new java.util.ArrayList()} #{body instanceof T(Object)}")
    void call(User user, Object body) {
    }
}
"""
        )
        myFixture.enableInspections(SpelUnresolvedTypeInspection())

        assertTrue(
            "已解析的类型不应报告: ${highlightsContaining("未解析的 SpEL 类型").map { it.description }}",
            highlightsContaining("未解析的 SpEL 类型").isEmpty()
        )
    }

    /** 类型引用检查可用 @SuppressWarnings 抑制。 */
    fun testSuppressUnresolvedType() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{T(com.example.NotExist)}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelUnresolvedTypeInspection())
        assertFalse(highlightsContaining("未解析的 SpEL 类型").isEmpty())

        launchFix("抑制此提示")

        val text = myFixture.file.text
        assertTrue(
            "源码应包含可识别的抑制标记: $text",
            text.contains("SuppressWarnings(\"SpelUnresolvedType\")")
        )
        assertTrue("抑制后不应再报告", highlightsContaining("未解析的 SpEL 类型").isEmpty())
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

    /** 内置变量 `$api$` 的类型是 ApiDescribe，传给 String 形参应报类型不匹配。 */
    fun testBeanMethodReferenceArgumentTypeMismatch() {
        addUserMapperStub()
        addApiDescribeStub()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(value = "#{@userMapper.findByName(${'$'}api${'$'})}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("引用类型实参与 String 形参不兼容时应报错", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue("应提示形参类型: ${infos.first().description}", infos.first().description!!.contains("String"))
        assertTrue("应提示实际类型: ${infos.first().description}", infos.first().description!!.contains("ApiDescribe"))
    }

    /** 引用类型实参可赋值给形参时保持静默（子类 → 父类）。 */
    fun testBeanMethodSubtypeArgumentPasses() {
        addNamesStub()
        myFixture.addFileToProject(
            "com/example/ItemNames.java",
            """
package com.example;

public class ItemNames extends Names {
}
"""
        )
        myFixture.addFileToProject(
            "com/example/UserMapper.java",
            """
package com.example;

public class UserMapper {
    public void acceptNames(Names names) {
    }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(value = "#{@userMapper.acceptNames(itemNames)}")
    void call(com.example.ItemNames itemNames) {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        assertTrue(
            "子类型实参不应报错: ${highlightsContaining("参数类型不匹配").map { it.description }}",
            highlightsContaining("参数类型不匹配").isEmpty()
        )
    }

    /** 不相关引用类型传给父类型形参同样要报错（ApiDescribe → Names）。 */
    fun testBeanMethodOtherReferenceArgumentTypeMismatch() {
        addNamesStub()
        addApiDescribeStub()
        myFixture.addFileToProject(
            "com/example/UserMapper.java",
            """
package com.example;

public class UserMapper {
    public void acceptNames(Names names) {
    }
}
"""
        )
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(value = "#{@userMapper.acceptNames(${'$'}api${'$'})}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("不相关引用类型应报错", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue("应提示形参类型 Names: ${infos.first().description}", infos.first().description!!.contains("Names"))
        assertTrue("应提示实际类型 ApiDescribe: ${infos.first().description}", infos.first().description!!.contains("ApiDescribe"))
    }

    /** 方法链返回的引用类型传给 String 形参同样要报错。 */
    fun testMethodChainReferenceArgumentTypeMismatch() {
        addRequestStub()
        addUserMapperStub()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(value = "#{@userMapper.findByName(${'$'}req${'$'}.getBaseUrl())}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("方法链结果的引用类型与 String 形参不兼容时应报错", infos.isEmpty())
        assertTrue("应提示实际类型 Text: ${infos.first().description}", infos.first().description!!.contains("Text"))
    }

    /** String 变量传给 String 形参不应报错。 */
    fun testBeanMethodStringVariablePasses() {
        addUserMapperStub()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(value = "#{@userMapper.findByName(card)}")
    void call(String card) {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        assertTrue(
            "String 实参不应报错: ${highlightsContaining("参数类型不匹配").map { it.description }}",
            highlightsContaining("参数类型不匹配").isEmpty()
        )
    }

    private fun addUserMapperStub() {
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
    }

    private fun addNamesStub() {
        myFixture.addFileToProject(
            "com/example/Names.java",
            "package com.example;\n\npublic class Names {\n}\n"
        )
    }

    private fun addApiDescribeStub() {
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/generalapi/describe/ApiDescribe.java",
            "package com.luckyframework.httpclient.generalapi.describe;\npublic class ApiDescribe {}\n"
        )
    }

    private fun addUserStub() {
        myFixture.addFileToProject(
            "com/example/User.java",
            """
package com.example;

public class User {
    public String a;
    public String b;
    public String c;

    public String getNick() {
        return "";
    }

    public void m1() {
    }

    public void m2() {
    }

    public void m3() {
    }
}
"""
        )
    }

    private fun addBagStub() {
        myFixture.addFileToProject(
            "com/example/Bag.java",
            """
package com.example;

public class Bag<E> {
    public E get(int index) {
        return null;
    }
}
"""
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

    /** `$body$` 的类型来自 `@ConvertMetaType`，参与函数参数类型检查。 */
    fun testBodyVariableTypeParticipatesInArgumentCheck() {
        myFixture.addFileToProject(
            "com/example/BodyItem.java",
            """
package com.example;

public class BodyItem {
    public String title;
}
"""
        )
        configure(
            """
package p;

import com.example.BodyItem;
import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;

class Api {
    @ConvertMetaType(value = BodyItem.class, respContentTypeFunc = "#{add(${'$'}body${'$'})}")
    void call() {
    }

    public static Integer add(Integer a) {
        return a;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应按 @ConvertMetaType 报告参数类型不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示实际类型: ${infos.first().description}",
            infos.first().description!!.contains("BodyItem")
        )
    }

    /** `$result$` 拆包后的类型参与函数参数类型检查（`Optional<ResultItem>` → `ResultItem`）。 */
    fun testResultVariableTypeUnwrapsPackType() {
        myFixture.addFileToProject(
            "com/example/ResultItem.java",
            """
package com.example;

public class ResultItem {
    public String value;
}
"""
        )
        configure(
            """
package p;

import com.example.ResultItem;
import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;

import java.util.Optional;

class Api {
    @ConvertMetaType(respContentTypeFunc = "#{add(${'$'}result${'$'})}")
    Optional<ResultItem> call() {
        return Optional.empty();
    }

    public static Integer add(Integer a) {
        return a;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应按拆包后的类型报告参数类型不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示拆包后的实际类型: ${infos.first().description}",
            infos.first().description!!.contains("ResultItem")
        )
    }

    /** `@RespConvert(metaType = ...)` 经多级 `@AliasFor` 决定 `$body$` 的类型，参与参数检查。 */
    fun testBodyVariableTypeFromRespConvertMetaType() {
        myFixture.addFileToProject(
            "com/example/RespItem.java",
            """
package com.example;

public class RespItem {
    public String word;
}
"""
        )
        configure(
            """
package p;

import com.example.RespItem;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;

class Api {
    @RespConvert(value = "#{add(${'$'}body${'$'})}", metaType = RespItem.class)
    void call() {
    }

    public static Integer add(Integer a) {
        return a;
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应按 @RespConvert#metaType 报告参数类型不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示实际类型: ${infos.first().description}",
            infos.first().description!!.contains("RespItem")
        )
    }

    /** 选择方括号内的裸标识符按元素类型校验：存在的属性不报错。 */
    fun testElementBracketExistingMemberPasses() {
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
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "元素成员不应提示: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
    }

    /** 选择方括号内不存在的属性按元素类型报错。 */
    fun testElementBracketMissingPropertyIsError() {
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
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在属性 'nationality'")

        assertFalse("应报告元素类型上不存在的属性", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示元素类型: ${infos.first().description}",
            infos.first().description!!.contains("ElementUser")
        )
    }

    /** 元素内的 `#this` 成员按当前元素类型校验。 */
    fun testElementThisMemberIsChecked() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[#this.nationality != null]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在属性 'nationality'")

        assertFalse("`#this` 不存在的属性应报告", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    /** 投影方括号内的裸方法调用按元素类型校验。 */
    fun testElementBracketMethodIsChecked() {
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
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "元素方法不应提示: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
    }

    fun testElementBracketMissingMethodIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.![missing()]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在方法 'missing'")

        assertFalse("应报告元素类型上不存在的方法", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    /** 元素成员的链式方法按实际接收者类型校验，不当作元素本身的裸方法。 */
    fun testElementMemberChainIsChecked() {
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
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "String 成员方法不应提示: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
    }

    /** 元素上下文不再把裸标识符当作 Root 变量报告。 */
    fun testElementBareIdentifierIsNotUnresolvedVariable() {
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
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        assertTrue(
            "元素成员不应报未解析变量: ${highlightsContaining("未解析的 SpEL 变量")}",
            highlightsContaining("未解析的 SpEL 变量").isEmpty()
        )
        assertTrue(
            "元素方法不应报未解析函数: ${highlightsContaining("未解析的 SpEL 函数")}",
            highlightsContaining("未解析的 SpEL 函数").isEmpty()
        )
    }

    /** `#this` 是 SpEL 内置当前对象，不应报未解析变量或命名空间。 */
    fun testThisInsideElementIsNotUnresolvedVariable() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[#this.name != null]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        assertTrue(
            "`#this` 不应报未解析变量: ${highlightsContaining("未解析的 SpEL 变量")}",
            highlightsContaining("未解析的 SpEL 变量").isEmpty()
        )
        assertTrue(
            "`#this` 不应报未解析命名空间: ${highlightsContaining("未解析的 SpEL 命名空间")}",
            highlightsContaining("未解析的 SpEL 命名空间").isEmpty()
        )
    }

    /** 嵌套选择：内层方括号按上一层的元素类型逐层校验。 */
    fun testNestedSelectionMissingPropertyIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementGroup;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{groups.?[members.?[nationality != null].size() > 0]}")
    void call(java.util.List<ElementGroup> groups) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在属性 'nationality'")

        assertFalse("嵌套选择应按内层元素类型报错", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示内层元素类型: ${infos.first().description}",
            infos.first().description!!.contains("ElementUser")
        )
    }

    /** 元素类型未知时检查保持静默降级，不产生误报。 */
    fun testUnknownElementTypeMemberStaysSilent() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{items.?[nationality != null]}")
    void call(Object items) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "未知元素类型不应报成员错误: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
    }

    /** `. ?[` 标记前的空白仍按元素类型校验成员。 */
    fun testSpaceBeforeSelectionMarkerIsChecked() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users. ?[nationality != null]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在属性 'nationality'")

        assertFalse("`. ?[` 应仍按元素类型校验", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    /** 普通变量 `#user.name` 不被当成命名空间，不报未解析。 */
    fun testVariableMemberIsNotUnresolvedNamespace() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#user.name}")
    void call(ElementUser user) {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())

        assertTrue(
            "`#user.name` 不应报未解析命名空间: ${highlightsContaining("未解析的 SpEL 命名空间")}",
            highlightsContaining("未解析的 SpEL 命名空间").isEmpty()
        )
        assertTrue(
            "`#user.name` 不应报未解析变量: ${highlightsContaining("未解析的 SpEL 变量")}",
            highlightsContaining("未解析的 SpEL 变量").isEmpty()
        )
    }

    /** 裸名称（Root 变量）接收者按变量接收者校验成员，不再整段跳过。 */
    fun testVariableMemberIsChecked() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{user.nationality}")
    void call(ElementUser user) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在属性 'nationality'")

        assertFalse("`user.` 后的成员应继续校验", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    /** 元素内 `#this.fn(arg)` 的接收者按当前元素类型解析。 */
    fun testThisMethodArgumentUsesElementType() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementGroup;
import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[#this.same(group)]}")
    void call(java.util.List<ElementUser> users, ElementGroup group) {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应按元素类型报告实参不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示元素类型形参: ${infos.first().description}",
            infos.first().description!!.contains("ElementUser")
        )
    }

    /** 元素内的裸成员实参按当前元素类型推断（`name` 是 ElementUser 的属性）。 */
    fun testElementMemberArgumentUsesElementType() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[#this.flagged(name)]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelFunctionCallInspection())

        val infos = highlightsContaining("参数类型不匹配")

        assertFalse("应按元素类型报告实参不匹配", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示 boolean 形参: ${infos.first().description}",
            infos.first().description!!.contains("boolean")
        )
    }

    /** 过滤条件结果必须是 boolean：String 属性按错误报告。 */
    fun testSelectionConditionNonBooleanIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[name]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("过滤条件的结果类型必须是 boolean")

        assertFalse("应报告过滤条件类型错误", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示实际类型: ${infos.first().description}",
            infos.first().description!!.contains("String")
        )
    }

    /** 非布尔字面量条件同样按错误报告。 */
    fun testSelectionConditionLiteralIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[1]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("过滤条件的结果类型必须是 boolean")

        assertFalse("非布尔字面量条件应报错", infos.isEmpty())
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** `#this` 是元素类型时同样参与过滤条件类型检查。 */
    fun testSelectionConditionThisIsChecked() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[#this]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("过滤条件的结果类型必须是 boolean")

        assertFalse("`#this` 条件应报告元素类型", infos.isEmpty())
        assertTrue(
            "应提示 ElementUser: ${infos.first().description}",
            infos.first().description!!.contains("ElementUser")
        )
    }

    /** boolean 属性与布尔字面量是合法过滤条件。 */
    fun testSelectionConditionBooleanPasses() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[active]} #{users.?[true]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        assertTrue(
            "boolean 条件不应报错: ${highlightsContaining("过滤条件的结果类型")}",
            highlightsContaining("过滤条件的结果类型").isEmpty()
        )
    }

    /** 未知符号无法推导类型时保守放行。 */
    fun testSelectionConditionUnverifiableStaysSilent() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[age > 1]} #{users.?[unknownFlag]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        assertTrue(
            "无法推导的条件不应报错: ${highlightsContaining("过滤条件的结果类型")}",
            highlightsContaining("过滤条件的结果类型").isEmpty()
        )
    }

    /** `![...]` 是投影，不是过滤条件，不做结果类型约束。 */
    fun testProjectionIsNotSelectionCondition() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.![name]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        assertTrue(
            "投影不应按过滤条件检查: ${highlightsContaining("过滤条件的结果类型")}",
            highlightsContaining("过滤条件的结果类型").isEmpty()
        )
    }

    /** `@If#condition` 的结果类型必须是 boolean/Boolean：String 字面量报错。 */
    fun testIfConditionStringLiteralIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{'abc'}", classes = MyUtils.class))
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("String 条件应报错: $infos", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示 String: ${infos.first().description}",
            infos.first().description!!.contains("String")
        )
    }

    /** `@If#condition` 为数值字面量时同样报错。 */
    fun testIfConditionNumericLiteralIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{1}", classes = MyUtils.class))
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("数值条件应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** 不写 `#{}` 的裸表达式同样参与结果类型检查。 */
    fun testIfConditionBareLiteralIsChecked() {
        addElementStubs()
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "1", classes = MyUtils.class))
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("裸数值条件应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** boolean 结果的条件通过，不报告。 */
    fun testIfConditionBooleanPasses() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = {
        @If(condition = "#{true}", classes = MyUtils.class),
        @If(condition = "false", classes = MyUtils.class)
    })
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        assertTrue(
            "boolean 条件不应报错: ${highlightsContaining("条件表达式的结果类型")}",
            highlightsContaining("条件表达式的结果类型").isEmpty()
        )
    }

    /** 比较表达式的结果一定是 boolean，不报告。 */
    fun testIfConditionComparisonStaysSilent() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{1==1}", classes = MyUtils.class))
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        assertTrue(
            "比较表达式不应报错: ${highlightsContaining("条件表达式的结果类型")}",
            highlightsContaining("条件表达式的结果类型").isEmpty()
        )
    }

    /** 算术表达式的结果不是 boolean：`#{1+2}` 报错。 */
    fun testIfConditionArithmeticIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{1+2}", classes = MyUtils.class))
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("算术条件应报错: $infos", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** 字符串拼接的结果不是 boolean：过滤条件 `#this.age + '12'` 报错。 */
    fun testSelectionConditionStringConcatenationIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[#this.age + '12']}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("过滤条件的结果类型必须是 boolean")

        assertFalse("字符串拼接条件应报错: $infos", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示 String: ${infos.first().description}",
            infos.first().description!!.contains("String")
        )
    }

    /** 比较与逻辑表达式的结果是 boolean，不报告。 */
    fun testSelectionConditionComparisonPasses() {
        addElementStubs()
        configure(
            """
package p;

import com.example.ElementUser;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{users.?[age > 1]} #{users.?[#this.age + 1 > 0]} #{users.?[name != null]}")
    void call(java.util.List<ElementUser> users) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        assertTrue(
            "比较/逻辑条件不应报错: ${highlightsContaining("过滤条件的结果类型")}",
            highlightsContaining("过滤条件的结果类型").isEmpty()
        )
    }

    /** `@Retryable#condition` 也登记为 IF_CONDITION，同样要求 boolean 结果。 */
    fun testRetryableConditionNonBooleanIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Retryable;

class Api {
    @Retryable(condition = "#{${'$'}status${'$'}}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("非布尔重试条件应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** 条件属性由多个片段拼接时结果一定是 String，报错。 */
    fun testIfConditionTemplateIsError() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.spel.If;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(ifs = @If(condition = "#{true} and #{false}", classes = MyUtils.class))
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式是模板拼接")

        assertFalse("模板拼接条件应报错: $infos", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
    }

    /** `@Retryable#enable` 也是 IF_CONDITION，同样要求 boolean 结果。 */
    fun testRetryableEnableNonBooleanIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Retryable;

class Api {
    @Retryable(enable = "#{1}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("非布尔 enable 应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** `@Condition#enable` 是条件开关，非布尔时报错。 */
    fun testConditionAnnotationEnableNonBooleanIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Condition;

class Api {
    @Condition(enable = "#{1}", assertion = "#{true}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("非布尔 condition.enable 应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** `@Condition#assertion` 是断言条件，非布尔时报错。 */
    fun testConditionAnnotationAssertionNonBooleanIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Condition;

class Api {
    @Condition(assertion = "#{'abc'}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("非布尔 assertion 应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 String: ${infos.first().description}",
            infos.first().description!!.contains("String")
        )
    }

    /** `@StaticQuery#condition` 等静态参数注解的条件同样参与校验。 */
    fun testStaticQueryConditionNonBooleanIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.StaticQuery;

class Api {
    void call(@StaticQuery(value = "id", condition = "#{1}") String id) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("非布尔 StaticQuery#condition 应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** boolean 结果的静态参数条件通过。 */
    fun testStaticQueryConditionBooleanPasses() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.StaticQuery;

class Api {
    void call(@StaticQuery(value = "id", condition = "#{true}") String id) {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        assertTrue(
            "boolean 条件不应报错: ${highlightsContaining("条件表达式的结果类型")}",
            highlightsContaining("条件表达式的结果类型").isEmpty()
        )
    }

    /** `@Hook#enable` 是条件开关，非布尔时报错。 */
    fun testHookEnableNonBooleanIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.hook.Hook;

class Api {
    @Hook(enable = "#{'abc'}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("非布尔 Hook#enable 应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 String: ${infos.first().description}",
            infos.first().description!!.contains("String")
        )
    }

    /** `@AutoRedirect#condition` 在响应阶段求值，同样要求 boolean。 */
    fun testAutoRedirectConditionNonBooleanIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.AutoRedirect;

class Api {
    @AutoRedirect(condition = "#{1}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("非布尔 AutoRedirect#condition 应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** `@PrintLog` 的请求/响应日志条件同样要求 boolean。 */
    fun testPrintLogConditionsNonBooleanIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.PrintLog;

class Api {
    @PrintLog(reqCondition = "#{'abc'}", respCondition = "#{1}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("非布尔日志条件应报错: $infos", infos.isEmpty())
    }

    /** 组合注解上的 condition 通过 @AliasFor 解析到元注解，同样参与校验。 */
    fun testAliasedConditionIsChecked() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.handle.ExceptionFallback;

class Api {
    @ExceptionFallback(condition = "#{1}")
    void call() {
    }
}
"""
        )
        myFixture.enableInspections(SpelSelectionConditionInspection())

        val infos = highlightsContaining("条件表达式的结果类型必须是 boolean")

        assertFalse("别名条件应报错: $infos", infos.isEmpty())
        assertTrue(
            "应提示 Integer: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** 数组变量的 `length` 是 SpEL 特判属性，不能报成员不存在。 */
    fun testArrayLengthPropertyPasses() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{names.length}")
    void call(String[] names) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "数组 length 不应报错: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
    }

    /** 内置参数数组符号 `$args$` 的 `length` 同样可访问。 */
    fun testBuiltinArrayLengthPropertyPasses() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{${'$'}args${'$'}.length}")
    void call(String name) {
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        assertTrue(
            "内置参数数组的 length 不应报错: ${highlightsContaining("中不存在")}",
            highlightsContaining("中不存在").isEmpty()
        )
    }

    /** `.` 之后的文本运算符（`ge`）是成员名，不存在时按错误报告。 */
    fun testMissingPropertyNamedKeywordAfterDotIsError() {
        addElementStubs()
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{add(1, 3).ge}")
    void call() {
    }

    public static Integer add(Integer a, Integer b) {
        return a + b;
    }
}
"""
        )
        myFixture.enableInspections(SpelMemberAccessInspection())

        val infos = highlightsContaining("不存在属性 'ge'")

        assertFalse("`.ge` 应报告不存在属性", infos.isEmpty())
        assertEquals(HighlightSeverity.ERROR, infos.first().severity)
        assertTrue(
            "应提示 Integer 类型: ${infos.first().description}",
            infos.first().description!!.contains("Integer")
        )
    }

    /** 选择/投影检查共用的 `List<ElementUser>` 类型桩（MockJdk 不含 java.util）。 */
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

    public boolean same(ElementUser other) {
        return other == this;
    }

    public boolean flagged(boolean active) {
        return active;
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
