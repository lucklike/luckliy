package com.luckyframework.idea.debug.container

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * 把随插件分发的 launcher jar 解压到临时目录。
 *
 * 子进程的 classpath 不能直接引用插件 jar 内的资源，必须先落盘。
 * 目录按插件版本隔离，升级插件后不会复用旧字节码；解压采用
 * 「临时文件 + 原子改名」，多个模块并发启动时不会读到半个 jar。
 */
object LauncherJarProvider {

    private val LOG = logger<LauncherJarProvider>()

    private const val RESOURCE = "/launcher/lucky-debug-launcher.jar"
    private const val FILE_NAME = "lucky-debug-launcher.jar"
    private const val ROOT_DIRECTORY = "lucky-debug-launcher"
    private const val FALLBACK_VERSION = "dev"

    /** 与 build.gradle.kts 中的插件 ID 保持一致。 */
    private const val PLUGIN_ID = "com.luckyframework.idea.httpclient"

    @Volatile
    private var cached: Path? = null

    /** 返回可直接加入子进程 classpath 的 launcher jar。 */
    fun jarPath(): Path {
        cached?.takeIf { Files.isRegularFile(it) }?.let { return it }
        synchronized(this) {
            cached?.takeIf { Files.isRegularFile(it) }?.let { return it }
            val target = extract(pluginVersion())
            cached = target
            return target
        }
    }

    private fun extract(version: String): Path {
        val directory = Path.of(PathManager.getTempPath()).resolve(ROOT_DIRECTORY).resolve(version)
        Files.createDirectories(directory)
        val target = directory.resolve(FILE_NAME)
        if (Files.isRegularFile(target) && Files.size(target) > 0L) {
            return target
        }
        val stream = LauncherJarProvider::class.java.getResourceAsStream(RESOURCE)
            ?: throw IllegalStateException("插件资源缺失：$RESOURCE")
        stream.use { input ->
            val temp = Files.createTempFile(directory, FILE_NAME, ".tmp")
            try {
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING)
                moveInto(temp, target)
            } finally {
                Files.deleteIfExists(temp)
            }
        }
        LOG.info("调试 launcher 已解压到 $target")
        return target
    }

    private fun moveInto(temp: Path, target: Path) {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun pluginVersion(): String {
        val version = runCatching {
            PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.version
        }.getOrNull() ?: FALLBACK_VERSION
        return FileUtil.sanitizeFileName(version).ifBlank { FALLBACK_VERSION }
    }
}
