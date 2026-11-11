package com.luckyframework.idea.spel.evaluator

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.net.URL
import java.net.URLClassLoader

/**
 * 从项目 classpath 反射加载 `spring-expression`（design D11，spec 15.1）。
 *
 * 插件不打包 Spring：URL 列表来自模块依赖库，父加载器使用 JDK 平台
 * 加载器（只提供 `java.*` 与平台类），避免插件自己的类路径意外满足依赖。
 * 找不到解析器类时返回 [EvaluatorLoadResult.Missing]，由 UI 给出提示。
 */
class EvaluatorClassLoader private constructor(private val urls: List<URL>) {

    fun load(): EvaluatorLoadResult {
        if (urls.isEmpty()) {
            return EvaluatorLoadResult.Missing(MISSING_MESSAGE)
        }
        val loader = URLClassLoader(urls.toTypedArray(), ClassLoader.getPlatformClassLoader())
        return try {
            Class.forName(PARSER_CLASS, false, loader)
            EvaluatorLoadResult.Available(loader)
        } catch (e: ClassNotFoundException) {
            loader.close()
            EvaluatorLoadResult.Missing(MISSING_MESSAGE)
        } catch (e: LinkageError) {
            loader.close()
            EvaluatorLoadResult.Missing("$MISSING_MESSAGE（${e.javaClass.simpleName}: ${e.message}）")
        }
    }

    companion object {
        const val PARSER_CLASS = "org.springframework.expression.spel.standard.SpelExpressionParser"

        const val MISSING_MESSAGE =
            "未在当前模块的 classpath 中找到 spring-expression，无法求值。" +
                "请确认项目依赖中包含 Spring（提供 SpEL 解析器）。"

        /** 模块依赖库的 URL 列表；不包含模块源码输出。 */
        fun fromModule(module: Module): EvaluatorClassLoader {
            val urls = runCatching {
                OrderEnumerator.orderEntries(module)
                    .recursively()
                    .librariesOnly()
                    .classes()
                    .getRoots()
                    .mapNotNull { root -> localUrlOf(root) }
                    .distinct()
            }.getOrDefault(emptyList())
            return EvaluatorClassLoader(urls)
        }

        /** `jar://...!/` 形式的根取 jar 文件本身；普通目录直接使用。 */
        private fun localUrlOf(root: VirtualFile): URL? {
            val path = root.path
            val local = if (path.endsWith("!/")) path.dropLast(2) else path
            return runCatching { File(local).toURI().toURL() }.getOrNull()
        }

        /** 测试与自定义来源使用。 */
        fun of(urls: List<URL>): EvaluatorClassLoader = EvaluatorClassLoader(urls.distinct())
    }
}

/** 加载结果：可用时返回类加载器，否则返回面向用户的提示。 */
sealed interface EvaluatorLoadResult {
    data class Available(val loader: ClassLoader) : EvaluatorLoadResult

    data class Missing(val message: String) : EvaluatorLoadResult
}
