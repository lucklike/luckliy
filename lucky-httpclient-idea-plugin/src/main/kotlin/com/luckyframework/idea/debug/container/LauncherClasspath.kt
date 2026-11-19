package com.luckyframework.idea.debug.container

import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * 构造 launcher 子进程的 classpath。
 *
 * 组成顺序：模块生产依赖 → 模块输出目录 → launcher jar。
 * launcher jar 放最后，让用户项目的 Spring/Jackson 优先命中。
 */
object LauncherClasspath {

    /**
     * Windows 命令行上限 32767；给 JVM 参数与主类预留 767 字符。
     *
     * 超长时不做 argfile：launcher 运行在用户项目的 JDK 上，
     * JDK 8 不支持 `@argfile`。
     */
    const val MAX_LENGTH = 32_000

    /** 超长提示中附带的示例条目数。 */
    const val SAMPLE_SIZE = 8

    sealed interface Result {
        data class Ok(val entries: List<String>, val classpath: String) : Result

        /** 调用方必须把长度与示例路径展示给用户，而不是静默截断。 */
        data class TooLong(val length: Int, val limit: Int, val sample: List<String>) : Result

        data object Empty : Result
    }

    fun build(module: Module, launcherJar: File): Result {
        val entries = LinkedHashSet<String>()
        productionRoots(module).forEach { root ->
            entryOf(root)?.let(entries::add)
        }
        // OrderEnumerator 在部分模块布局下不返回输出根，显式补齐。
        // 只能取生产输出：测试输出里可能有 Spring 组件/接口（例如 src/test 下的
        // @HttpClient 接口），放进子进程 classpath 后会被默认组件扫描加载，
        // 导致正常 Application 能启动、调试容器却启动失败。
        CompilerPaths.getModuleOutputPath(module, false)?.let { path ->
            val file = File(path)
            if (file.exists()) {
                entries.add(file.absolutePath)
            }
        }
        // 防御性剔除：即使 OrderEnumerator 或其他布局带出了测试输出，
        // 也不能让 src/test 下的接口进入容器 classpath。
        stripTestOutput(
            entries,
            CompilerPaths.getModuleOutputPath(module, true)?.let { File(it).absolutePath }
        )
        entries.add(launcherJar.absolutePath)
        return evaluate(entries.toList())
    }

    /** 纯函数，便于单测：从 classpath 中剔除测试输出目录及其子目录。 */
    internal fun stripTestOutput(entries: MutableSet<String>, testOutput: String?) {
        if (testOutput == null) {
            return
        }
        entries.removeIf { it == testOutput || it.startsWith(testOutput + File.separator) }
    }

    /** 纯函数，便于单测：拼接、限长与截断样本。 */
    internal fun evaluate(entries: List<String>): Result {
        if (entries.isEmpty()) {
            return Result.Empty
        }
        val classpath = entries.joinToString(File.pathSeparator)
        return if (classpath.length <= MAX_LENGTH) {
            Result.Ok(entries, classpath)
        } else {
            Result.TooLong(classpath.length, MAX_LENGTH, entries.take(SAMPLE_SIZE))
        }
    }

    /** `jar://...!/` 取 jar 文件本身；目录根直接使用。不存在的条目丢弃。 */
    internal fun entryOf(root: VirtualFile): String? {
        val path = root.path
        val local = if (path.endsWith("!/")) path.dropLast(2) else path
        val file = File(local)
        return if (file.exists()) file.absolutePath else null
    }

    private fun productionRoots(module: Module): Array<VirtualFile> = runCatching {
        OrderEnumerator.orderEntries(module)
            .recursively()
            .productionOnly()
            .classes()
            .roots
    }.getOrDefault(emptyArray())
}
