package com.luckyframework.idea.debug.container

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 子进程输出的环形缓冲。
 *
 * 端口握手行也会保留：容器启动失败时，这些行是定位问题的主要线索。
 * 只保留最近 [capacity] 行，长时间运行的容器不会无限占用内存。
 */
class DebugLogBuffer(private val capacity: Int = DEFAULT_CAPACITY) {

    private val lines = ArrayDeque<String>(capacity.coerceAtLeast(1))
    private val stamp = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    @Synchronized
    fun append(line: String, stderr: Boolean = false) {
        val limit = capacity.coerceAtLeast(1)
        while (lines.size >= limit) {
            lines.removeFirst()
        }
        val prefix = if (stderr) "[err] " else ""
        lines.addLast("${LocalTime.now().format(stamp)} $prefix$line")
    }

    @Synchronized
    fun appendAll(messages: Iterable<String>, stderr: Boolean = false) {
        messages.forEach { append(it, stderr) }
    }

    @Synchronized
    fun tail(count: Int = DEFAULT_TAIL): List<String> =
        lines.toList().takeLast(count.coerceAtLeast(0))

    @Synchronized
    fun snapshot(): List<String> = lines.toList()

    @Synchronized
    fun clear() {
        lines.clear()
    }

    companion object {
        const val DEFAULT_CAPACITY = 2_000
        const val DEFAULT_TAIL = 80
    }
}
