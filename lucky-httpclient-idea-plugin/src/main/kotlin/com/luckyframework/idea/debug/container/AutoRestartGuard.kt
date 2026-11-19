package com.luckyframework.idea.debug.container

/**
 * 自动重启熔断器。
 *
 * 连续 [MAX_CONSECUTIVE_FAILURES] 次自动重启失败后停止自动重启，
 * 避免编译监听与失败容器互相触发形成无限重启。
 * 用户手动启动成功后清零，重新允许自动重启。
 */
class AutoRestartGuard(
    private val maxConsecutiveFailures: Int = MAX_CONSECUTIVE_FAILURES
) {

    var consecutiveFailures: Int = 0
        private set

    var suspended: Boolean = false
        private set

    /**
     * 记录一次自动重启结果。
     *
     * @return 是否允许继续自动重启：成功恒为 true，失败达到阈值后为 false。
     */
    @Synchronized
    fun record(success: Boolean): Boolean {
        if (success) {
            consecutiveFailures = 0
            suspended = false
            return true
        }
        consecutiveFailures++
        if (consecutiveFailures >= maxConsecutiveFailures) {
            suspended = true
        }
        return !suspended
    }

    /** 用户手动启动时调用：清除熔断计数。 */
    @Synchronized
    fun reset() {
        consecutiveFailures = 0
        suspended = false
    }

    @Synchronized
    fun canAutoRestart(): Boolean = !suspended

    companion object {
        /** 用户确认的阈值：连续 2 次自动重启失败即停止。 */
        const val MAX_CONSECUTIVE_FAILURES = 2
    }
}
