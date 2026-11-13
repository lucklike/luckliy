package com.luckyframework.idea.spel.symbol

/**
 * 6.1 符号模型：6 种 kind 与 7 种来源，均由上下文构建器真实产出。
 */
class SpelSymbolTest : SymbolFixtureTestCase() {

    fun testAllKindsAreDeclared() {
        assertEquals(
            setOf("VARIABLE", "ROOT_VARIABLE", "FUNCTION", "NAMESPACE", "TYPE", "BEAN"),
            SpelSymbolKind.values().map { it.name }.toSet()
        )
        assertEquals(
            setOf(
                "PARAMETER",
                "METHOD_IMPORT",
                "CLASS_IMPORT",
                "CLASS_STATIC",
                "BUILTIN",
                "GLOBAL",
                "UNKNOWN"
            ),
            SpelSymbolOrigin.values().map { it.name }.toSet()
        )
    }

    fun testContextProducesAllOriginLayers() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(rootLit = "user=admin")
class Api {

    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{x}")
    void query(String host) {
    }
}
"""
        )
        val context = newBuilder(
            builtin = listOf(symbol("\$status\$", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.BUILTIN, "Integer")),
            global = listOf(symbol("gvar", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.GLOBAL))
        ).build(literalOf("#{x}"))

        assertTrue(context.known)
        assertEquals(SpelSymbolOrigin.PARAMETER, context.find("host", SpelSymbolKind.VARIABLE).single().origin)

        val user = context.find("user", SpelSymbolKind.ROOT_VARIABLE).single()
        assertEquals(SpelSymbolOrigin.CLASS_IMPORT, user.origin)
        assertNotNull(user.declaration)
        assertNotNull(user.sourceRange)

        val md5 = context.find("md5", SpelSymbolKind.FUNCTION).single()
        assertEquals(SpelSymbolOrigin.METHOD_IMPORT, md5.origin)
        assertEquals("String", md5.type)
        assertEquals("crypto", md5.namespace)
        assertEquals("md5(String)", md5.signature)

        assertEquals(SpelSymbolOrigin.METHOD_IMPORT, context.find("crypto", SpelSymbolKind.NAMESPACE).single().origin)
        assertEquals(SpelSymbolOrigin.BUILTIN, context.find("\$status\$").single().origin)
        assertEquals(SpelSymbolOrigin.GLOBAL, context.find("gvar").single().origin)
    }

    fun testFunctionRulesFollowClassStaticElement() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))
        val names = context.ofKind(SpelSymbolKind.FUNCTION).map { it.name }.toSet()

        assertTrue("md5" in names)
        assertTrue("sha" in names)
        assertFalse("sha256" in names)
        assertFalse("filtered" in names)
        assertFalse("hidden" in names)
        assertFalse("instanceMethod" in names)
    }

    /** 自定义注解按同名属性把 value 传给 `@Namespace`（隐式别名）。 */
    fun testNamespaceFromCustomAnnotation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@Namespace
@interface MyNamespace {
    String value() default "";
}

@MyNamespace("secret")
class SecretUtils {
    public static String sign(String text) {
        return text;
    }
}

class Api {
    @SpELImport(SecretUtils.class)
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))

        assertEquals(
            SpelSymbolOrigin.METHOD_IMPORT,
            context.find("secret", SpelSymbolKind.NAMESPACE).single().origin
        )
        assertEquals("secret", context.find("sign", SpelSymbolKind.FUNCTION).single().namespace)
    }

    /** `@Namespace` 带 `@Inherited`，导入子类时命名空间来自父类。 */
    fun testNamespaceInheritedFromSuperClass() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@Namespace("base")
class BaseUtils {
    public static String baseFn(String text) {
        return text;
    }
}

class ChildUtils extends BaseUtils {
    public static String childFn(String text) {
        return text;
    }
}

class Api {
    @SpELImport(ChildUtils.class)
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))

        assertEquals("base", context.find("base", SpelSymbolKind.NAMESPACE).single().name)
        assertEquals("base", context.find("baseFn", SpelSymbolKind.FUNCTION).single().namespace)
        assertEquals("base", context.find("childFn", SpelSymbolKind.FUNCTION).single().namespace)
    }

    /** `@FunctionFilter` 出现在任意层元注解上时，函数都应按运行期 `isAnnotated` 被过滤。 */
    fun testFunctionFilterThroughNestedMetaAnnotation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@FunctionFilter
@interface Hidden {
}

@Hidden
@interface DeepHidden {
}

class SecretUtils {
    @DeepHidden
    public static String hiddenFn(String text) {
        return text;
    }

    public static String visibleFn(String text) {
        return text;
    }
}

class Api {
    @SpELImport(SecretUtils.class)
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))
        val names = context.ofKind(SpelSymbolKind.FUNCTION).map { it.name }.toSet()

        assertFalse("hiddenFn" in names)
        assertTrue("visibleFn" in names)
    }

    /** `@Namespace` 也能从接口继承（运行期按 `TYPE_HIERARCHY` 查找）。 */
    fun testNamespaceInheritedFromInterface() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@Namespace("iface")
interface NsContract {
}

class IfaceUtils implements NsContract {
    public static String echo(String text) {
        return text;
    }
}

class Api {
    @SpELImport(IfaceUtils.class)
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))

        assertEquals("iface", context.find("iface", SpelSymbolKind.NAMESPACE).single().name)
        assertEquals("iface", context.find("echo", SpelSymbolKind.FUNCTION).single().namespace)
    }

    /** 显式别名先指向自定义注解的元注解属性，再由隐式别名接到 `@Namespace#value`。 */
    fun testNamespaceFromAliasForToMetaAttribute() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import org.springframework.core.annotation.AliasFor;

@Namespace
@interface NsMeta {
    String value() default "";
}

@NsMeta
@interface NsAlias {

    @AliasFor(annotation = NsMeta.class, attribute = "value")
    String name() default "";
}

@NsAlias(name = "alias")
class AliasUtils {
    public static String ok(String text) {
        return text;
    }
}

class Api {
    @SpELImport(AliasUtils.class)
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))

        assertEquals("alias", context.find("ok", SpelSymbolKind.FUNCTION).single().namespace)
    }

    fun testStandaloneTypeAndBeanKinds() {
        assertEquals(SpelSymbolKind.TYPE, SpelSymbol("java.util.List", SpelSymbolKind.TYPE).kind)
        assertEquals(SpelSymbolKind.BEAN, SpelSymbol("@userMapper", SpelSymbolKind.BEAN).kind)
    }
}
