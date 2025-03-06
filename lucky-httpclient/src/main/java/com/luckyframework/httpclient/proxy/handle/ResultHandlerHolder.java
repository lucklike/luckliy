package com.luckyframework.httpclient.proxy.handle;

import org.springframework.core.ResolvableType;

/**
 * 结果处理器持有者
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/03/05 16:16
 */
@SuppressWarnings("all")
public class ResultHandlerHolder {

    private final ResultHandler resultHandler;
    private final ResolvableType resultType;


    public ResultHandlerHolder(ResultHandler<?> resultHandler, ResolvableType resultType) {
        this.resultHandler = resultHandler;
        this.resultType = resultType;
    }

    public ResultHandler getResultHandler() {
        return resultHandler;
    }

    public ResolvableType getResultType() {
        return resultType;
    }
}
