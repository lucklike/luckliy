package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.reflect.MethodUtils;

import java.lang.reflect.Method;

public class Event<T> {

    private final MethodContext context;
    private final T message;


    public Event(MethodContext context, T message) {
        this.context = context;
        this.message = message;
    }

    public MethodContext getContext() {
        return context;
    }

    public T getMessage() {
        return message;
    }

    public void reconnection() {
        Object proxyObject = context.getProxyObject();
        Object[] arguments = context.getArguments();
        Method method = context.getCurrentAnnotatedElement();
        MethodUtils.invoke(proxyObject, method, arguments);
    }
}
