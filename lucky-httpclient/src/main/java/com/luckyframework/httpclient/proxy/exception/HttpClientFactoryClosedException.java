package com.luckyframework.httpclient.proxy.exception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * HttpClient代理对象工厂已关闭异常
 * <p>
 * 工厂调用过 {@code shutdown()}、{@code shutdownNow()} 或 {@code close()} 之后，
 * 其持有的线程池、连接池等资源已经被释放，不再允许创建新的代理对象。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-11-02
 */
public class HttpClientFactoryClosedException extends LuckyRuntimeException {

    public HttpClientFactoryClosedException(String message) {
        super(message);
    }

    public HttpClientFactoryClosedException(Throwable ex) {
        super(ex);
    }

    public HttpClientFactoryClosedException(String message, Throwable ex) {
        super(message, ex);
    }

    public HttpClientFactoryClosedException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public HttpClientFactoryClosedException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
