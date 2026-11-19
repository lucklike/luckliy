package com.luckyframework.idea.debug.container

import java.io.File

/**
 * 判断自动解析出的主类是否已经编译到输出目录。
 *
 * 只检查目录，不打开依赖 jar：Spring Boot 主类来自模块源码，
 * 未构建时 PSI 能看到类，但 launcher 的 classpath 中没有对应 class 文件。
 */
internal object MainClassOutputCheck {

    fun isCompiled(
        outputDirs: List<File>,
        classpathEntries: List<String>,
        mainClass: String
    ): Boolean {
        val relative = mainClass.replace('.', '/') + ".class"
        return (outputDirs + classpathEntries.map(::File))
            .filter { it.isDirectory }
            .any { File(it, relative).isFile }
    }
}
