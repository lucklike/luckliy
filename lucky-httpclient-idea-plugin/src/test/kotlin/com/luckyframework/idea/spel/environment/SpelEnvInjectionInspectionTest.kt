package com.luckyframework.idea.spel.environment

import com.intellij.lang.annotation.HighlightSeverity
import com.luckyframework.idea.spel.inspection.UnresolvedSpelSymbolInspection

/**
 * `@Env` 注入名的配置键检查。
 *
 * `@Env("key:default")` 与 `${key:default}` 同义：键缺失时运行期回退默认值，
 * 不能按「未定义的环境配置键」报警。
 */
class SpelEnvInjectionInspectionTest : SpringEnvironmentFixtureTestCase() {

    fun testUndefinedKeyWithDefaultIsNotWarned() {
        configureEnv("server.missing:45")

        assertNoWarningContains("未定义的环境配置键")
    }

    fun testUndefinedKeyWithoutDefaultIsWarned() {
        configureEnv("server.missing")

        assertWarningContains("未定义的环境配置键")
    }

    fun testDefinedKeyIsNotWarned() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "server.port=45\n"
        )
        configureEnv("server.port")

        assertNoWarningContains("未定义的环境配置键")
    }

    /** `spring.config.import` 导入的配置文件中的键也被 `@Env` 识别。 */
    fun testKeyFromImportedConfigIsNotWarned() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            "spring:\n  config:\n    import: \"classpath:extra.yml\"\n"
        )
        myFixture.addFileToProject(
            "src/main/resources/extra.yml",
            "server:\n  port: 45\n"
        )
        configureEnv("server.port")

        assertNoWarningContains("未定义的环境配置键")
    }

    private fun configureEnv(name: String) {
        myFixture.configureByText(
            "EnvDemo.java",
            """
class EnvDemo {
    static void call(@com.luckyframework.httpclient.proxy.spel.Env("$name") Integer port) {
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
