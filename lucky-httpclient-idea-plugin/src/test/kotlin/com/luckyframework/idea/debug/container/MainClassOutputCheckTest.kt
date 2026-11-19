package com.luckyframework.idea.debug.container

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 主类输出检查：自动解析到 PSI 类不代表 class 文件已经生成。
 */
class MainClassOutputCheckTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `returns true when class exists in module output`() {
        val output = temp.newFolder("out")
        writeClass(output, "com.example.App")

        assertTrue(
            MainClassOutputCheck.isCompiled(listOf(output), emptyList(), "com.example.App")
        )
    }

    @Test
    fun `returns true when class exists in a classpath directory`() {
        val dependencyOutput = temp.newFolder("dependency-out")
        writeClass(dependencyOutput, "com.example.App")

        assertTrue(
            MainClassOutputCheck.isCompiled(
                emptyList(),
                listOf(dependencyOutput.absolutePath),
                "com.example.App"
            )
        )
    }

    @Test
    fun `returns false when class is missing`() {
        val output = temp.newFolder("out")

        assertFalse(
            MainClassOutputCheck.isCompiled(listOf(output), emptyList(), "com.example.App")
        )
    }

    @Test
    fun `returns false when no output directory exists`() {
        assertFalse(
            MainClassOutputCheck.isCompiled(
                listOf(File(temp.root, "missing")),
                emptyList(),
                "com.example.App"
            )
        )
    }

    private fun writeClass(root: File, className: String) {
        val file = File(root, className.replace('.', '/') + ".class")
        file.parentFile.mkdirs()
        file.writeText("stub")
    }
}
