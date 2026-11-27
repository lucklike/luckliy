package com.luckyframework.idea.spel.spring

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Lucky 组件 Bean 索引：Spring 静态模型不认识的 `@HttpClient`/`@LuckyComponent`
 * 接口，按运行期 `LuckyHttpClientImportBeanDefinitionRegistrar` 的规则补充。
 */
class LuckyBeanIndexTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(HTTP_CLIENT_PATH, HTTP_CLIENT)
        myFixture.addFileToProject(LUCKY_COMPONENT_PATH, LUCKY_COMPONENT)
        myFixture.addFileToProject(HTTP_CLIENT_COMPONENT_PATH, HTTP_CLIENT_COMPONENT)
        myFixture.addFileToProject(STD_HTTP_CLIENT_PATH, STD_HTTP_CLIENT)
    }

    /** `@HttpClient` 接口按类名首字母小写注册为 Bean，例如 `MockApi` -> `mockApi`。 */
    fun testHttpClientInterfaceBecomesBean() {
        myFixture.addFileToProject(
            "src/org/example/api/MockApi.java",
            """
package org.example.api;

import io.github.lucklike.httpclient.discovery.HttpClient;

@HttpClient
public interface MockApi {
}
"""
        )

        val beans = LuckyBeanIndex.beansOf(myFixture.module)

        val bean = beans.firstOrNull { it.name == "mockApi" }
        assertNotNull("应识别 @HttpClient 接口为 Bean: ${beans.map { it.name }}", bean)
        assertEquals("org.example.api.MockApi", bean!!.typeText)
        assertEquals("org.example.api.MockApi", bean.beanClass?.qualifiedName)
        assertEquals("MockApi.java", bean.fileName)
        assertSame(bean.beanClass, bean.declaration)
    }

    /** `@HttpClient(beanId = ...)` 优先于类名推导。 */
    fun testHttpClientBeanIdOverridesClassName() {
        myFixture.addFileToProject(
            "src/org/example/api/CustomApi.java",
            """
package org.example.api;

import io.github.lucklike.httpclient.discovery.HttpClient;

@HttpClient(beanId = "customClient")
public interface CustomApi {
}
"""
        )

        val names = LuckyBeanIndex.beansOf(myFixture.module).map { it.name }

        assertTrue("应使用 beanId 作为 Bean 名: $names", "customClient" in names)
        assertFalse("类名推导名不应重复出现: $names", "customApi" in names)
    }

    /** `@LuckyComponent(name = ...)` 与 `@HttpClient(beanId = ...)` 规则一致。 */
    fun testLuckyComponentNameIsUsed() {
        myFixture.addFileToProject(
            "src/org/example/api/NamedApi.java",
            """
package org.example.api;

import io.github.lucklike.httpclient.annotation.LuckyComponent;

@LuckyComponent(name = "namedClient")
public interface NamedApi {
}
"""
        )

        val names = LuckyBeanIndex.beansOf(myFixture.module).map { it.name }

        assertTrue("应识别 @LuckyComponent 的 name: $names", "namedClient" in names)
    }

    /** 运行期只注册接口/抽象类，具体类不是 Lucky 组件。 */
    fun testConcreteClassIsNotComponent() {
        myFixture.addFileToProject(
            "src/org/example/api/ConcreteApi.java",
            """
package org.example.api;

import io.github.lucklike.httpclient.discovery.HttpClient;

@HttpClient
public class ConcreteApi {
}
"""
        )

        val names = LuckyBeanIndex.beansOf(myFixture.module).map { it.name }

        assertFalse("具体类不应成为 Lucky 组件: $names", "concreteApi" in names)
    }

    /** `@StdHttpClient` 是组合注解（元注解 `@HttpClient`），按元注解规则也要识别。 */
    fun testCompositeAnnotationInterfaceBecomesBean() {
        myFixture.addFileToProject(
            "src/org/example/api/std/DeepSeekApi.java",
            """
package org.example.api.std;

import io.github.lucklike.httpclient.std.StdHttpClient;

@StdHttpClient
public interface DeepSeekApi {
}
"""
        )

        val names = LuckyBeanIndex.beansOf(myFixture.module).map { it.name }

        assertTrue("组合注解接口应识别为 Bean: $names", "deepSeekApi" in names)
    }

    private companion object {
        const val HTTP_CLIENT_PATH = "io/github/lucklike/httpclient/discovery/HttpClient.java"
        const val LUCKY_COMPONENT_PATH = "io/github/lucklike/httpclient/annotation/LuckyComponent.java"
        const val HTTP_CLIENT_COMPONENT_PATH =
            "io/github/lucklike/httpclient/annotation/HttpClientComponent.java"
        const val STD_HTTP_CLIENT_PATH = "io/github/lucklike/httpclient/std/StdHttpClient.java"

        const val HTTP_CLIENT = """
package io.github.lucklike.httpclient.discovery;

import io.github.lucklike.httpclient.annotation.HttpClientComponent;

@HttpClientComponent
public @interface HttpClient {

    String beanId() default "";
}
"""

        const val LUCKY_COMPONENT = """
package io.github.lucklike.httpclient.annotation;

@HttpClientComponent
public @interface LuckyComponent {

    String name() default "";
}
"""

        const val HTTP_CLIENT_COMPONENT = """
package io.github.lucklike.httpclient.annotation;

public @interface HttpClientComponent {

    String name() default "";
}
"""

        const val STD_HTTP_CLIENT = """
package io.github.lucklike.httpclient.std;

import io.github.lucklike.httpclient.discovery.HttpClient;

@HttpClient
public @interface StdHttpClient {
}
"""
    }
}
