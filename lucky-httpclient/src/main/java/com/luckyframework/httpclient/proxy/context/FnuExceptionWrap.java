package com.luckyframework.httpclient.proxy.context;

import java.lang.reflect.Method;

/**
 * 函数异常包装器
 */
public class FnuExceptionWrap {
    private final Throwable throwable;
    private final Method method;


    private FnuExceptionWrap(Throwable throwable, Method method) {
        this.throwable = throwable;
        this.method = method;
    }

    public static FnuExceptionWrap of(Method method, Throwable throwable) {
        return new FnuExceptionWrap(throwable, method);
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Method getMethod() {
        return method;
    }
}
