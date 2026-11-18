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

    companion object {
        const val STARTER_MARKER_PATH = "io/github/lucklike/httpclient/SpringSpELConvert.java"

        private const val STARTER_MARKER = """
package io.github.lucklike.httpclient;

public class SpringSpELConvert {
}
"""
    }
}
