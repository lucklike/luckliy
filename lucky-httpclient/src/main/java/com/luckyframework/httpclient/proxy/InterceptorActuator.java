package com.luckyframework.httpclient.proxy;

/**
 * 拦截器执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 07:15
 */
public abstract class InterceptorActuator {

    public static final int TYPE_NULL = 0;
    public static final int TYPE_CLASS = 1;
    public static final int TYPE_METHOD = 2;

    private final int type;

    public InterceptorActuator(int type) {
        this.type = type;
    }

    public boolean isNullType() {
        return type == TYPE_NULL;
    }

    public boolean isClassType() {
        return type == TYPE_CLASS;
    }

    public boolean isMethodType() {
        return type == TYPE_METHOD;
    }
}
