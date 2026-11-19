package com.luckyframework.idea.debug.protocol

/**
 * 调试协议常量。
 *
 * 与 launcher 侧 com.luckyframework.debug.DebugProtocol 必须逐字一致：
 * 版本号、操作名、错误码任何一侧调整都要同步另一端。
 */
object DebugProtocol {

    const val VERSION = 1

    const val PORT_PREFIX = "LUCKY_DEBUG_PORT="

    const val MAIN_CLASS_PROPERTY = "lucky.debug.mainClass"

    const val FACTORY_BEAN_NAME = "__luckyHttpClientProxyFactory__"

    const val FACTORY_TYPE = "com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory"

    const val OP_HELLO = "hello"
    const val OP_STATUS = "status"
    const val OP_PING = "ping"
    const val OP_INVOKE = "invoke"
    const val OP_CANCEL = "cancel"
    const val OP_SHUTDOWN = "shutdown"

    const val STATE_STARTING = "STARTING"
    const val STATE_READY = "READY"
    const val STATE_FAILED = "FAILED"
    const val STATE_STOPPED = "STOPPED"

    const val DEFAULT_TIMEOUT_MILLIS = 60_000L
    const val CONNECT_TIMEOUT_MILLIS = 5_000
    const val PING_INTERVAL_MILLIS = 10_000L
}

/**
 * 协议错误码。
 *
 * [UNKNOWN] 只用于插件侧兜底：launcher 返回了当前版本不认识的错误码时，
 * 界面仍需展示原始文本而不是空白。
 */
enum class DebugErrorCode(val code: String, val displayName: String) {
    PROTOCOL_MISMATCH("PROTOCOL_MISMATCH", "协议版本不匹配"),
    CONTAINER_STARTING("CONTAINER_STARTING", "容器正在启动"),
    CONTAINER_FAILED("CONTAINER_FAILED", "容器启动失败"),
    BEAN_MISSING("BEAN_MISSING", "缺少 HttpClientProxyObjectFactory"),
    BEAN_TYPE_MISMATCH("BEAN_TYPE_MISMATCH", "工厂 Bean 类型不匹配"),
    CLASS_NOT_FOUND("CLASS_NOT_FOUND", "接口类不存在"),
    METHOD_NOT_FOUND("METHOD_NOT_FOUND", "方法不存在"),
    ARGUMENT_CONVERT("ARGUMENT_CONVERT", "参数转换失败"),
    INVOCATION_TARGET("INVOCATION_TARGET", "目标方法执行失败"),
    TIMEOUT("TIMEOUT", "执行超时"),
    CANCELLED("CANCELLED", "已取消"),
    INTERNAL("INTERNAL", "内部错误"),
    UNKNOWN("UNKNOWN", "未知错误");

    companion object {
        fun of(code: String?): DebugErrorCode = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}
