package com.luckyframework.idea.spel

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.spel.metadata.SpelFrameworkStubs
import com.luckyframework.idea.spel.metadata.SpelMetaStubs
import com.luckyframework.idea.spel.symbol.SpelRuntimeStubs

/**
 * 平台测试基类：默认给测试模块加入 lucky-httpclient 依赖桩。
 *
 * 桩包含 `HttpRequest`（依赖感知激活判定）、框架注解上的 `@SpelAttribute`
 * 声明（决定哪些属性参与 SpEL），以及运行时符号扫描所需的元注解、
 * `@SpelSymbol` 变量与注册函数；不需要引入真实框架 jar。
 *
 * 框架注解桩集中在 [SpelFrameworkStubs]，与 [com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase]
 * 共用，避免两套夹具对「属性是否参与 SpEL」给出不同答案。
 */
abstract class LuckyFixtureTestCase : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        (SpelMetaStubs.STUBS + SpelRuntimeStubs.STUBS + SpelFrameworkStubs.STUBS)
            .forEach { (path, text) -> myFixture.addFileToProject(path, text) }
    }
}
