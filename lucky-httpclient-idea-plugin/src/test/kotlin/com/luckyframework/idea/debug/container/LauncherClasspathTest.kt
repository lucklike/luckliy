package com.luckyframework.idea.debug.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LauncherClasspathTest {

    @Test
    fun `strip test output removes exact and nested entries`() {
        val testOutput = File("test-classes").absolutePath
        val production = File("classes").absolutePath
        val entries = linkedSetOf(
            production,
            testOutput,
            File(testOutput, "generated").absolutePath,
            "$testOutput-other"
        )

        LauncherClasspath.stripTestOutput(entries, testOutput)

        assertEquals(listOf(production, "$testOutput-other"), entries.toList())
    }

    @Test
    fun `strip test output keeps entries when test output is absent`() {
        val entries = linkedSetOf("a.jar", "b.jar")

        LauncherClasspath.stripTestOutput(entries, null)

        assertEquals(listOf("a.jar", "b.jar"), entries.toList())
    }

    @Test
    fun `evaluate joins entries with platform separator`() {
        val entries = listOf("a.jar", "b.jar")

        val result = LauncherClasspath.evaluate(entries) as LauncherClasspath.Result.Ok

        assertEquals(entries.joinToString(File.pathSeparator), result.classpath)
        assertEquals(entries, result.entries)
    }

    @Test
    fun `evaluate returns empty for no entries`() {
        assertEquals(LauncherClasspath.Result.Empty, LauncherClasspath.evaluate(emptyList()))
    }

    @Test
    fun `evaluate accepts classpath at exact limit`() {
        val entry = "x".repeat(LauncherClasspath.MAX_LENGTH)

        val result = LauncherClasspath.evaluate(listOf(entry))

        assertTrue(result is LauncherClasspath.Result.Ok)
    }

    @Test
    fun `evaluate reports too long with sample and length`() {
        val entries = (1..10).map { "x".repeat(8_000) + it }

        val result = LauncherClasspath.evaluate(entries) as LauncherClasspath.Result.TooLong

        assertTrue(result.length > result.limit)
        assertEquals(LauncherClasspath.SAMPLE_SIZE, result.sample.size)
        assertEquals(entries.first(), result.sample.first())
    }

    @Test
    fun `too long sample keeps insertion order`() {
        val entries = (1..10).map { index -> "x".repeat(8_000) + index }

        val result = LauncherClasspath.evaluate(entries) as LauncherClasspath.Result.TooLong

        assertEquals(entries.take(LauncherClasspath.SAMPLE_SIZE), result.sample)
    }
}
