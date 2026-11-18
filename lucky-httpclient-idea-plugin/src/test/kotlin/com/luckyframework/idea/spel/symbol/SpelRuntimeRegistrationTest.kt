package com.luckyframework.idea.spel.symbol

import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 注册函数类的运行时发现：框架默认类、factory 源码调用与项目内注册调用。
 *
 * 三路来源取并集：jar 场景下 factory 方法体不可读时，默认类与项目调用
 * 仍必须完整；注册函数从 CLASS 阶段起可用。
 *
 * 用户源码在 setUp 中写入，保证词索引在测试方法执行前就绪。
 */
class SpelRuntimeRegistrationTest : SymbolFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        addUserSources()
    }

    /** 默认注册类始终存在，且生命周期对齐工厂构造时机。 */
    fun testDefaultFunctionClassesAlwaysRegistered() {
        val symbols = SpelRuntimeSymbols.functions(project)

        val time = symbols.first { it.name == "time" }
        assertEquals(SpelSymbolKind.FUNCTION, time.kind)
        assertEquals(SpelSymbolOrigin.BUILTIN, time.origin)
        assertTrue(time.registered)
        assertEquals(SpelLifecycle.CLASS, time.lifecycle)
    }

    /** factory 源码中的显式注册与默认清单是并集，不会互相替换。 */
    fun testFactorySourceRegistrationKeepsDefaultClasses() {
        val symbols = SpelRuntimeSymbols.functions(project)

        assertTrue("默认清单中的函数应保留", symbols.any { it.name == "time" })
        assertTrue("factory 源码注册的函数应存在", symbols.any { it.name == "temp_encode" })
    }

    /** 项目源码中的实例调用可识别，命名空间重载产生两组符号。 */
    fun testProjectRegistrationCallIsDiscovered() {
        val symbols = SpelRuntimeSymbols.functions(project)

        val plain = symbols.first { it.name == "hello" && it.namespace == null }
        assertEquals(SpelSymbolOrigin.BUILTIN, plain.origin)
        assertTrue(plain.registered)
        assertEquals(SpelLifecycle.CLASS, plain.lifecycle)

        assertTrue("显式命名空间注册应保留", symbols.any { it.name == "hello" && it.namespace == "util" })
        assertTrue("别名在命名空间下同样生效", symbols.any { it.name == "hi" && it.namespace == "util" })
    }

    /** 子类构造器内继承的注册调用同样可识别。 */
    fun testSubclassRegistrationCallIsDiscovered() {
        assertTrue(
            "子类中的注册调用应被识别",
            SpelRuntimeSymbols.functions(project).any { it.name == "subHello" }
        )
    }

    /** 用户自定义的同名方法不参与注册识别。 */
    fun testSameNameMethodIsNotMistakenForRegistration() {
        assertTrue(
            "同名方法的调用不应被当作工厂注册",
            SpelRuntimeSymbols.functions(project).none { it.name == "fakeHello" }
        )
    }

    /** 字符串与注释中的方法名不参与注册识别。 */
    fun testStringAndCommentOccurrencesAreIgnored() {
        assertTrue(
            "字符串与注释中的方法名不应产生符号",
            SpelRuntimeSymbols.functions(project).none { it.name == "commentHello" }
        )
    }

    /** `ClassStaticElement` 包装调用的实参不是类字面量，直接跳过。 */
    fun testClassStaticElementWrapperIsIgnored() {
        assertTrue(
            "包装调用无法静态解析，应当跳过",
            SpelRuntimeSymbols.functions(project).none { it.name == "wrappedHello" }
        )
    }

    /** 常量命名空间被求值；空白命名空间回退到类上的 `@Namespace`。 */
    fun testConstantAndBlankNamespaceAreResolved() {
        val symbols = SpelRuntimeSymbols.functions(project)

        assertTrue(
            "常量命名空间应覆盖类上的 @Namespace",
            symbols.any { it.name == "annotatedHello" && it.namespace == "util" }
        )
        assertTrue(
            "空白命名空间应回退到类上的 @Namespace",
            symbols.any { it.name == "annotatedHello" && it.namespace == "ann" }
        )
    }

    /** `#name(...)` 只放行变量表里有平铺 Method 的函数。 */
    fun testHashCallFormFollowsNamespaceAndRegistration() {
        val registered = symbol("hello", SpelSymbolKind.FUNCTION).copy(registered = true)
        val unregistered = symbol("time", SpelSymbolKind.FUNCTION).copy(registered = false)
        val namespaced = symbol("hi", SpelSymbolKind.FUNCTION)
            .copy(namespace = "util", registered = true)
        val context = SpelContext(
            known = true,
            stage = SpelLifecycle.CLASS,
            attribute = null,
            symbols = listOf(registered, unregistered, namespaced)
        )

        assertEquals("注册的无命名空间函数可用 #hello(...)", 1, context.functions("hello", null, false).size)
        assertTrue("未注册内置函数仍只能平铺", context.functions("time", null, false).isEmpty())
        assertTrue("带命名空间函数不能写 #hi(...)", context.functions("hi", null, false).isEmpty())
        assertEquals("带命名空间函数只能写 #util.hi(...)", 1, context.functions("hi", "util", false).size)
        assertEquals("平铺视图仍包含内置函数", 1, context.functions("time", null, true).size)
    }

    private fun addUserSources() {
        myFixture.addFileToProject(
            "p/MyFunctions.java",
            """
package p;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

public class MyFunctions {

    public static String hello(String name) {
        return name;
    }

    @FunctionAlias("hi")
    public static String greet(String name) {
        return name;
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/MyConfig.java",
            """
package p;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;

public class MyConfig {

    public void init(HttpClientProxyObjectFactory factory) {
        factory.addSpringElFunctionClass(MyFunctions.class);
        factory.addSpringElFunctionClass("util", MyFunctions.class);
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/SubFunctions.java",
            """
package p;

public class SubFunctions {
    public static String subHello() {
        return "";
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/MyFactory.java",
            """
package p;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;

public class MyFactory extends HttpClientProxyObjectFactory {

    public MyFactory() {
        addSpringElFunctionClass(SubFunctions.class);
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/FakeFunctions.java",
            """
package p;

public class FakeFunctions {
    public static String fakeHello() {
        return "";
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/FakeFactory.java",
            """
package p;

public class FakeFactory {

    public void addSpringElFunctionClass(Class<?> functionClass) {
    }

    public void init() {
        addSpringElFunctionClass(FakeFunctions.class);
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/CommentFunctions.java",
            """
package p;

public class CommentFunctions {
    public static String commentHello() {
        return "";
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/Noise.java",
            """
package p;

public class Noise {
    // addSpringElFunctionClass(CommentFunctions.class)
    private static final String TEXT = "addSpringElFunctionClass(CommentFunctions.class)";

    public void init() {
        String ignored = TEXT;
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/WrappedFunctions.java",
            """
package p;

public class WrappedFunctions {
    public static String wrappedHello() {
        return "";
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/WrappedConfig.java",
            """
package p;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.spel.ClassStaticElement;

public class WrappedConfig {

    public void init(HttpClientProxyObjectFactory factory) {
        factory.addSpringElFunctionClass(ClassStaticElement.create(WrappedFunctions.class));
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/AnnotatedFunctions.java",
            """
package p;

import com.luckyframework.httpclient.proxy.spel.Namespace;

@Namespace("ann")
public class AnnotatedFunctions {

    public static String annotatedHello() {
        return "";
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/NamespaceConfig.java",
            """
package p;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;

public class NamespaceConfig {

    private static final String NS = "util";

    public void init(HttpClientProxyObjectFactory factory) {
        factory.addSpringElFunctionClass(NS, AnnotatedFunctions.class);
        factory.addSpringElFunctionClass("  ", AnnotatedFunctions.class);
    }
}
"""
        )
    }
}
