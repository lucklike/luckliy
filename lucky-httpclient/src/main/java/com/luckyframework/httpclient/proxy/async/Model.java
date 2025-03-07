package com.luckyframework.httpclient.proxy.async;

/**
 * 异步模型
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/6 23:30
 */
public enum Model {

    /**
     * Java线程模型
     */
    JAVA_THREAD,

    /**
     * Kotlin协程模型
     */
    KOTLIN_COROUTINE,

    /**
     * 使用公用的异步模型
     */
    USE_COMMON

}
