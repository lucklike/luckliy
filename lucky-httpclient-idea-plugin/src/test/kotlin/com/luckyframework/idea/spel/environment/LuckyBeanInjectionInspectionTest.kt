package com.luckyframework.idea.spel.environment

import com.intellij.lang.annotation.HighlightSeverity
import com.luckyframework.idea.spel.inspection.UnresolvedSpelSymbolInspection

/**
 * `@Bean("name")` 注入名的解析。
 *
 * Lucky 组件（`@HttpClient` 接口）由 starter 在运行期通过
 * `ImportBeanDefinitionRegistrar` 注册，Spring 静态模型看不到；本插件按
 * 运行期规则补充后，`@Bean("mockApi")` 不能再报「未解析的 Bean」。
 */
class LuckyBeanInjectionInspectionTest : SpringEnvironmentFixtureTestCase() {

    /** `MockApi` 接口按类名首字母小写注册为 `mockApi`。 */
    fun testHttpClientInterfaceBeanIsResolved() {
        addMockApi()
        configureBean("mockApi")

        assertNoWarningContains("未解析的 Bean")
    }

    /** 不存在的 Bean 仍然要报，避免检查被整体放行。 */
    fun testMissingBeanIsStillWarned() {
        addMockApi()
        configureBean("missingApi")

        assertWarningContains("未解析的 Bean")
    }

    private fun addMockApi() {
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
    }

    private fun configureBean(name: String) {
        myFixture.configureByText(
            "BeanInjectionDemo.java",
            """
class BeanInjectionDemo {
    static void call(@com.luckyframework.httpclient.proxy.spel.Bean("$name") Object api) {
    }
}
"""
        )
        myFixture.enableInspections(UnresolvedSpelSymbolInspection())
    }

    private fun assertWarningContains(message: String) {
        val warnings = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.WARNING }
        assertTrue(
            "未找到包含「$message」的警告: ${warnings.map { it.description }}",
            warnings.any { it.description?.contains(message) == true }
        )
    }

    private fun assertNoWarningContains(message: String) {
        val warnings = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.WARNING }
        assertFalse(
            "不应出现包含「$message」的警告: ${warnings.map { it.description }}",
            warnings.any { it.description?.contains(message) == true }
        )
    }
}
