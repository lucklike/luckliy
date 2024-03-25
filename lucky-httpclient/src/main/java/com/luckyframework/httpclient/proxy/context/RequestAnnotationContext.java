package com.luckyframework.httpclient.proxy.context;


import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
import org.springframework.core.ResolvableType;

import java.util.function.Consumer;

/**
 * 携带请求实例的注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/23 14:07
 */
public class RequestAnnotationContext extends AnnotationContext {

    private final Request request;

    public RequestAnnotationContext(Request request) {
        this.request = request;
    }

    @Override
    public <T> T parseExpression(String expression, ResolvableType returnType, Consumer<ContextParamWrapper> paramSetter) {
        return super.parseExpression(expression, returnType, cpw -> {
            cpw.extractRequest(request);
            paramSetter.accept(cpw);
        });
    }
}
