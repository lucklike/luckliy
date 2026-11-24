package com.luckyframework.idea.debug.container

/**
 * 子进程输出的日志缓冲。
 *
 * 端口握手行也会保留：容器启动失败时，这些行是定位问题的主要线索。
 * 不额外添加时间戳：Spring Boot 日志本身已带时间，重复前缀只会挤占展示空间。
 *
 * 默认保留全部输出：调试面板的 Logs 需要完整展示一次请求产生的日志，
 * 不能因为缓冲容量把开头的启动与请求日志挤掉。只有显式传入正数 [capacity]
 * 时才裁剪为最近若干行（测试与特殊场景使用）。
 *
 * [onAppend] 在每次成功追加后触发，供 UI 实时刷新日志；回调在锁外执行，
 * 避免写线程与界面线程互相等待。
 */
class DebugLogBuffer(
    capacity: Int? = null,
    private val onAppend: (() -> Unit)? = null
) {

    /** 保留上限；null 表示不裁剪，全部保留。 */
    private val limit: Int? = capacity?.coerceAtLeast(1)

    private val lines = ArrayDeque<String>(limit ?: DEFAULT_INITIAL_CAPACITY)

    /** 下一行将获得的序号；只增不减，界面可据此跳过已清空的旧日志。 */
    private var nextSequence = 0L

    fun append(line: String, stderr: Boolean = false) {
        synchronized(this) {
            val cap = limit
            if (cap != null) {
                while (lines.size >= cap) {
                    lines.removeFirst()
                }
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

    /** 保留的全部日志及其序号范围；Logs 页面要求完整展示且不截断。 */
    @Synchronized
    fun allWithSequence(): Tail {
        val all = lines.toList()
        return Tail(all, nextSequence - all.size, nextSequence)
    }

    @Synchronized
    fun snapshot(): List<String> = lines.toList()

    @Synchronized
    fun clear() {
        lines.clear()
    }

    companion object {
        /** 不裁剪时 ArrayDeque 的初始容量，避免按最大行数预分配内存。 */
        private const val DEFAULT_INITIAL_CAPACITY = 16

        const val DEFAULT_TAIL = 80
    }
}
