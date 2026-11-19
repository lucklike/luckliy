package com.luckyframework.debug;

/**
 * 调试协议常量。
 *
 * 与插件侧 com.luckyframework.idea.debug.protocol.DebugProtocol 必须逐字一致，
 * 版本号、操作名、错误码任何一侧调整都要同步另一端。
 */
public final class DebugProtocol {

    /** 协议版本，两端不一致时拒绝服务。 */
    public static final int VERSION = 1;

    /** 端口握手行前缀：launcher 启动后向 stdout 输出的第一行。 */
    public static final String PORT_PREFIX = "LUCKY_DEBUG_PORT=";

    /** 主类系统属性名。 */
    public static final String MAIN_CLASS_PROPERTY = "lucky.debug.mainClass";

    /** starter 中 HttpClientProxyObjectFactory 的 bean 名。 */
    public static final String FACTORY_BEAN_NAME = "__luckyHttpClientProxyFactory__";

    /** 必须在容器中存在的工厂类型。 */
    public static final String FACTORY_TYPE =
            "com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory";

    /** 容器上下文接口，全部方法通过该接口反射调用。 */
    public static final String CONTEXT_TYPE =
            "org.springframework.context.ConfigurableApplicationContext";

    public static final String OP_HELLO = "hello";
    public static final String OP_STATUS = "status";
    public static final String OP_PING = "ping";
    public static final String OP_INVOKE = "invoke";
    public static final String OP_CANCEL = "cancel";
    public static final String OP_SHUTDOWN = "shutdown";

    public static final String STATE_STARTING = "STARTING";
    public static final String STATE_READY = "READY";
    public static final String STATE_FAILED = "FAILED";
    public static final String STATE_STOPPED = "STOPPED";

    public static final String ERROR_PROTOCOL_MISMATCH = "PROTOCOL_MISMATCH";
    public static final String ERROR_CONTAINER_STARTING = "CONTAINER_STARTING";
    public static final String ERROR_CONTAINER_FAILED = "CONTAINER_FAILED";
    public static final String ERROR_BEAN_MISSING = "BEAN_MISSING";
    public static final String ERROR_BEAN_TYPE_MISMATCH = "BEAN_TYPE_MISMATCH";
    public static final String ERROR_CLASS_NOT_FOUND = "CLASS_NOT_FOUND";
    public static final String ERROR_METHOD_NOT_FOUND = "METHOD_NOT_FOUND";
    public static final String ERROR_ARGUMENT_CONVERT = "ARGUMENT_CONVERT";
    public static final String ERROR_INVOCATION_TARGET = "INVOCATION_TARGET";
    public static final String ERROR_TIMEOUT = "TIMEOUT";
    public static final String ERROR_CANCELLED = "CANCELLED";
    public static final String ERROR_INTERNAL = "INTERNAL";

    private DebugProtocol() {
    }
}
