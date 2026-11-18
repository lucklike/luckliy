package com.luckyframework.idea.spel.environment

import com.luckyframework.idea.spel.LuckyFixtureTestCase

/**
 * 带 `lucky-httpclient-spring-boot-starter` 桩的夹具。
 *
 * [SpringEnvironmentService] 以 `SpringSpELConvert` 类是否存在判断 starter 依赖，
 * 这里提供一个最小源码桩，让 `${...}` 能力在测试模块中处于激活状态。
 */
abstract class SpringEnvironmentFixtureTestCase : LuckyFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(STARTER_MARKER_PATH, STARTER_MARKER)
        myFixture.addFileToProject(STD_HTTP_CLIENT_PATH, STD_HTTP_CLIENT)
        myFixture.addFileToProject(HTTP_CLIENT_PATH, HTTP_CLIENT)
    }

    /** 把待测文本写进 `@HttpRequest(url = ...)` 注解属性。 */
    protected fun configureHttpRequest(value: String): String {
        myFixture.configureByText(
            "Demo.java",
            """
class Demo {
    @com.luckyframework.httpclient.proxy.annotations.HttpRequest(url = "$value")
    void call() {
    }
}
"""
        )
        return myFixture.file.text
    }

    /** 把待测文本写进 Spring `@Value` 注解属性。 */
    protected fun configureValue(value: String): String {
        myFixture.configureByText(
            "DemoValue.java",
            """
class DemoValue {
    @org.springframework.beans.factory.annotation.Value("$value")
    private String field;
}
"""
        )
        return myFixture.file.text
    }

    companion object {
        const val STARTER_MARKER_PATH = "io/github/lucklike/httpclient/SpringSpELConvert.java"

        private const val STD_HTTP_CLIENT_PATH = "io/github/lucklike/httpclient/std/StdHttpClient.java"

        private const val HTTP_CLIENT_PATH = "io/github/lucklike/httpclient/discovery/HttpClient.java"

        /** 与真实源码一致：`path` 通过 `@AliasFor` 映射到 `@HttpClient#path`。 */
        private const val STD_HTTP_CLIENT = """
package io.github.lucklike.httpclient.std;

import io.github.lucklike.httpclient.discovery.HttpClient;
import org.springframework.core.annotation.AliasFor;

public @interface StdHttpClient {

    @AliasFor(annotation = HttpClient.class, attribute = "path")
    String path() default "";
}
"""

        /** 与真实源码一致：`path` 标注 `@SpelAttribute`，参与 SpEL/占位符解析。 */
        private const val HTTP_CLIENT = """
package io.github.lucklike.httpclient.discovery;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;

public @interface HttpClient {

    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String path() default "";
}
"""

        private const val STARTER_MARKER = """
package io.github.lucklike.httpclient;

public class SpringSpELConvert {
}
"""
    }
}
