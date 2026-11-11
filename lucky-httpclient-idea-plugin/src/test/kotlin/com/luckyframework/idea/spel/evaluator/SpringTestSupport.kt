package com.luckyframework.idea.spel.evaluator

import java.io.File
import java.net.URL

/**
 * 测试专用：把测试 classpath 中的 spring-expression 及其依赖
 * 以隔离类加载器的方式提供给求值器，模拟插件运行期从项目
 * classpath 反射加载的环境。
 */
object SpringTestSupport {

    private val SPRING_CLASSES = listOf(
        "org.springframework.expression.spel.standard.SpelExpressionParser",
        "org.springframework.core.ResolvableType",
        "org.apache.commons.logging.LogFactory"
    )

    /**
     * 从类资源 URL 反推 jar 位置；不依赖 `ProtectionDomain`，
     * 测试类加载器可能不提供 code source。
     */
    fun urls(): List<URL> = SPRING_CLASSES.map { name -> locationOf(name) }.distinct()

    private fun locationOf(className: String): URL {
        val resource = className.replace('.', '/') + ".class"
        val url = SpringTestSupport::class.java.classLoader.getResource(resource)
            ?: throw IllegalStateException("测试 classpath 中找不到 $className")
        if (url.protocol == "jar") {
            return URL(url.toString().removePrefix("jar:").substringBefore("!/"))
        }
        val relative = resource.replace('/', File.separatorChar)
        val root = url.path.removeSuffix(relative).removeSuffix(File.separator)
        return File(root).toURI().toURL()
    }

    fun loader(): ClassLoader = when (val result = EvaluatorClassLoader.of(urls()).load()) {
        is EvaluatorLoadResult.Available -> result.loader
        is EvaluatorLoadResult.Missing -> error("无法从测试 classpath 加载 spring-expression：${result.message}")
    }

    fun engine(): SpringEvaluatorEngine = SpringEvaluatorEngine(loader())
}
