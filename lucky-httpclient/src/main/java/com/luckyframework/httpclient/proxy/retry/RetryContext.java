package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 重试注解上下文信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 17:01
 */
public abstract class RetryContext extends AnnotationContext {
    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
    }
}
