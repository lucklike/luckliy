package com.luckyframework.idea.debug.container

/**
 * 子进程输出的环形缓冲。
 *
 * 端口握手行也会保留：容器启动失败时，这些行是定位问题的主要线索。
 * 不额外添加时间戳：Spring Boot 日志本身已带时间，重复前缀只会挤占展示空间。
 * 只保留最近 [capacity] 行，长时间运行的容器不会无限占用内存。
 *
 * [onAppend] 在每次成功追加后触发，供 UI 实时刷新日志；回调在锁外执行，
 * 避免写线程与界面线程互相等待。
 */
class DebugLogBuffer(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val onAppend: (() -> Unit)? = null
) {

    private val lines = ArrayDeque<String>(capacity.coerceAtLeast(1))

    /** 下一行将获得的序号；只增不减，界面可据此跳过已清空的旧日志。 */
    private var nextSequence = 0L

    fun append(line: String, stderr: Boolean = false) {
        synchronized(this) {
            val limit = capacity.coerceAtLeast(1)
            while (lines.size >= limit) {
                lines.removeFirst()
            }
            val prefix = if (stderr) "[err] " else ""
            lines.addLast("$prefix$line")
            nextSequence++
        }
        onAppend?.invoke()
    }

    fun appendAll(messages: Iterable<String>, stderr: Boolean = false) {
        messages.forEach { append(it, stderr) }
    }

    @Synchronized
    fun tail(count: Int = DEFAULT_TAIL): List<String> =
        lines.toList().takeLast(count.coerceAtLeast(0))

    /** 日志尾部及其在缓冲区中的序号范围。 */
    data class Tail(val lines: List<String>, val startSequence: Long, val nextSequence: Long)

    /**
     * 返回尾部日志与序号范围。
     *
     * 界面清空显示后记录 [nextSequence]，之后只渲染序号更新的日志；
     * 序号只增不减，容器重启清空缓冲也不会误跳过新日志。
     */
    @Synchronized
    fun tailWithSequence(count: Int = DEFAULT_TAIL): Tail {
        val tail = lines.toList().takeLast(count.coerceAtLeast(0))
        return Tail(tail, nextSequence - tail.size, nextSequence)
    }

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
