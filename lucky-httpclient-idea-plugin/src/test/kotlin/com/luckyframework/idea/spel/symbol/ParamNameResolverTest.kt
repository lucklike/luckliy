package com.luckyframework.idea.spel.symbol

import com.intellij.psi.PsiLiteralExpression
import com.luckyframework.idea.spel.metadata.ParamNameResolver

/**
 * `ParameterUtils.getParamName` 的插件侧复刻：合并 `@Param` 与 `@AliasFor` 链。
 *
 * 运行期注入 SpEL 上下文的参数名不是方法签名里的名字，而是合并后的
 * `@Param#value`；`@Param` 常以元注解形式出现在 `@Query`、`@Header`
 * 等标准参数注解上，名字再经 `@AliasFor` 链从 `value` 传到 `@Param`。
 */
class ParamNameResolverTest : SymbolFixtureTestCase() {

    private val resolver = ParamNameResolver()

    fun testDirectParamValue() {
        configure(
            """
package p;

import com.luckyframework.reflect.Param;

class Api {
    void query(@Param("keyword") String q) {
    }
}
"""
        )
        assertEquals("keyword", resolveParameter("query", 0))
    }

    fun testDirectParamWithoutValue() {
        configure(
            """
package p;

import com.luckyframework.reflect.Param;

class Api {
    void query(@Param String keyword) {
    }
}
"""
        )
        assertNull(resolveParameter("query", 0))
    }

    /** `@Query("req")`：`@Query#value` → `@DynamicParam#name` → `@Param#value`。 */
    fun testCrossAnnotationAliasChain() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {
    void query(@Query("req") String request) {
    }
}
"""
        )
        assertEquals("req", resolveParameter("query", 0))
    }

    /** 未写 `value` 时注解默认值不产生覆盖。 */
    fun testQueryWithoutValue() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {
    void query(@Query String request) {
    }
}
"""
        )
        assertNull(resolveParameter("query", 0))
    }

    /** 自定义组合注解：元注解实例上的显式值生效。 */
    fun testValueDeclaredOnMetaAnnotation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Query;

@Query("uid")
@interface UserId {
}

class Api {
    void query(@UserId String user) {
    }
}
"""
        )
        assertEquals("uid", resolveParameter("query", 0))
    }

    /** `@Param` 直接出现在自定义注解的元注解位置。 */
    fun testParamOnMetaAnnotation() {
        configure(
            """
package p;

import com.luckyframework.reflect.Param;

@Param("fixed")
@interface FixedName {
}

class Api {
    void query(@FixedName String q) {
    }
}
"""
        )
        assertEquals("fixed", resolveParameter("query", 0))
    }

    /** 元注解同名属性构成隐式别名：`@MyParam("uid")` → `@Param#value`。 */
    fun testImplicitAliasOnMetaAnnotation() {
        configure(
            """
package p;

import com.luckyframework.reflect.Param;

@Param
@interface MyParam {
    String value() default "";
}

class Api {
    void query(@MyParam("uid") String user) {
    }
}
"""
        )
        assertEquals("uid", resolveParameter("query", 0))
    }

    /** 多层元注解的同名属性同样是隐式别名链。 */
    fun testImplicitAliasAcrossMetaAnnotationChain() {
        configure(
            """
package p;

import com.luckyframework.reflect.Param;

@Param
@interface InnerParam {
    String value() default "";
}

@InnerParam
@interface OuterParam {
    String value() default "";
}

class Api {
    void query(@OuterParam("uid") String user) {
    }
}
"""
        )
        assertEquals("uid", resolveParameter("query", 0))
    }

    /** 显式别名先指向元注解的同名属性，再由隐式别名接到 `@Param#value`。 */
    fun testAliasForToMetaAnnotationAttribute() {
        configure(
            """
package p;

import com.luckyframework.reflect.Param;
import org.springframework.core.annotation.AliasFor;

@Param
@interface ParamMeta {
    String value() default "";
}

@ParamMeta
@interface QueryParam {

    @AliasFor(annotation = ParamMeta.class, attribute = "value")
    String name() default "";
}

class Api {
    void query(@QueryParam(name = "req") String request) {
    }
}
"""
        )
        assertEquals("req", resolveParameter("query", 0))
    }

    /** 同注解内的 `@AliasFor("realName")` 一跳后再跨注解指向 `@Param#value`。 */
    fun testSameAnnotationAliasHop() {
        configure(
            """
package p;

import com.luckyframework.reflect.Param;
import org.springframework.core.annotation.AliasFor;

@interface AliasName {

    @AliasFor("realName")
    String value() default "";

    @AliasFor(annotation = Param.class, attribute = "value")
    String realName() default "";
}

class Api {
    void query(@AliasName("aliased") String q) {
    }
}
"""
        )
        assertEquals("aliased", resolveParameter("query", 0))
    }

    /** 注解值是常量引用：运行期反射读到的是编译期内联值，插件侧同样求值。 */
    fun testConstantReference() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {

    static final String NAME = "req";

    void query(@Query(NAME) String request) {
    }
}
"""
        )
        assertEquals("req", resolveParameter("query", 0))
    }

    /** 常量拼接同样属于编译期常量表达式。 */
    fun testConstantConcatenation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {

    private static final String PREFIX = "user_";

    void query(@Query(PREFIX + "id") String userId, @Query(PREFIX + PREFIX) String repeated) {
    }
}
"""
        )
        assertEquals("user_id", resolveParameter("query", 0))
        assertEquals("user_user_", resolveParameter("query", 1))
    }

    /** 拼接中的基本类型常量按 Java 的字符串转换规则取值。 */
    fun testPrimitiveConstantInConcatenation() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {

    static final int SIZE = 20;

    void query(@Query("page_" + SIZE) String page) {
    }
}
"""
        )
        assertEquals("page_20", resolveParameter("query", 0))
    }

    /** 跨类常量引用可求值；非常量表达式（方法调用）没有静态值，回退声明名。 */
    fun testCrossClassConstantAndNonConstantFallback() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Query;

class Constants {
    static final String NAME = "req";
}

class Helpers {
    static String name() {
        return "runtime";
    }
}

class Api {
    void query(@Query(Constants.NAME) String request, @Query(Helpers.name()) String dynamic) {
    }
}
"""
        )
        assertEquals("req", resolveParameter("query", 0))
        assertNull(resolveParameter("query", 1))
    }

    /** 与 `@Param` 无关的注解不产生覆盖。 */
    fun testUnrelatedAnnotationIgnored() {
        configure(
            """
package p;

class Api {
    void query(@Deprecated String userId, String page) {
    }
}
"""
        )
        assertNull(resolveParameter("query", 0))
        assertNull(resolveParameter("query", 1))
    }

    /** 注入名同时记录注解中书写的名称位置，供跳转定位到注解处。 */
    fun testResolveNameReportsDeclaration() {
        configure(
            """
package p;

import com.luckyframework.reflect.Param;

class Api {
    void query(@Param("keyword") String q, @Param String other) {
    }
}
"""
        )
        val parameters = methodNamed("query").parameterList.parameters
        val resolved = resolver.resolveName(parameters[0])

        assertNotNull(resolved)
        assertEquals("keyword", resolved!!.name)
        assertTrue(resolved.declaration is PsiLiteralExpression)
        assertEquals("\"keyword\"", resolved.declaration!!.text)
        // 没有显式值时不存在可跳转的注解位置，回退参数声明
        assertNull(resolver.resolveName(parameters[1]))
    }

    /** 常量引用同样落在注解中书写名称的表达式上。 */
    fun testResolveNamePointsToConstantReference() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {

    static final String NAME = "req";

    void query(@Query(NAME) String request) {
    }
}
"""
        )
        val resolved = resolver.resolveName(methodNamed("query").parameterList.parameters[0])

        assertNotNull(resolved)
        assertEquals("req", resolved!!.name)
        assertEquals("NAME", resolved.declaration!!.text)
    }

    private fun resolveParameter(method: String, index: Int): String? =
        resolver.resolve(methodNamed(method).parameterList.parameters[index])
}
