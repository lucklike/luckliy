package com.luckyframework.idea.spel.evaluator

import junit.framework.TestCase

/**
 * 15.1：反射加载项目 classpath 中的 spring-expression（不打包）。
 */
class EvaluatorClassLoaderTest : TestCase() {

    fun testEmptyClasspathReturnsPrompt() {
        val result = EvaluatorClassLoader.of(emptyList()).load()

        assertTrue("空 classpath 应返回缺失提示: $result", result is EvaluatorLoadResult.Missing)
        val message = (result as EvaluatorLoadResult.Missing).message
        assertTrue("提示应说明缺少 spring-expression: $message", message.contains("spring-expression"))
    }

    fun testLoadsFromProvidedUrls() {
        val result = EvaluatorClassLoader.of(SpringTestSupport.urls()).load()

        assertTrue("应加载成功: $result", result is EvaluatorLoadResult.Available)
        val loader = (result as EvaluatorLoadResult.Available).loader
        val parser = Class.forName(EvaluatorClassLoader.PARSER_CLASS, false, loader)
        assertEquals(EvaluatorClassLoader.PARSER_CLASS, parser.name)
    }

    fun testIsolatedFromTestClasspath() {
        // 父加载器固定为 platform：即使测试本身依赖 spring-expression，
        // 空 URL 列表也必须返回缺失提示，避免掩盖「项目实际没有依赖」的情况。
        val result = EvaluatorClassLoader.of(emptyList()).load()
        assertTrue(result is EvaluatorLoadResult.Missing)
    }
}
