package com.luckyframework.httpclient.proxy.handle;


import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 结果上下文
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/03/05 16:16
 */
public class ResultContext<T> {

    private final MethodContext context;
    private final T result;

    public ResultContext(MethodContext context, T result) {
        this.context = context;
        this.result = result;
    }

    public MethodContext getContext() {
        return context;
    }

    public T getResult() {
        return result;
    }
}
