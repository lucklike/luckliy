package com.luckyframework.idea.spel

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 平台测试基类：默认给测试模块加入 lucky-httpclient 依赖桩。
 *
 * 桩只提供 `HttpRequest` 注解类，用于通过依赖感知激活判定；
 * 不需要引入真实框架 jar。
 */
abstract class LuckyFixtureTestCase : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        addHttpRequestStub()
    }

    protected fun addHttpRequestStub() {
        myFixture.addFileToProject(HTTP_REQUEST_STUB_PATH, HTTP_REQUEST_STUB)
    }

    companion object {
        const val HTTP_REQUEST_STUB_PATH =
            "com/luckyframework/httpclient/proxy/annotations/HttpRequest.java"

        const val HTTP_REQUEST_STUB =
            "package com.luckyframework.httpclient.proxy.annotations;\npublic @interface HttpRequest {}"
    }
}
