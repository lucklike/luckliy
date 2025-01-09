package com.luckyframework.httpclient.proxy.context;

import java.lang.reflect.Method;

/**
 * 方法包装类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/5 16:59
 */
public class MethodWrap {

    /**
     * 空实例
     */
    public final static MethodWrap NULL_INSTANCES = wrap(null, null, null);

    private final Boolean appoint;
    private final String funcName;
    private final Method method;


    private MethodWrap(Boolean appoint, String funcName, Method method) {
        this.appoint = appoint;
        this.funcName = funcName;
        this.method = method;
    }

    public static MethodWrap wrap(String funcName, Method method, Boolean appoint) {
        return new MethodWrap(appoint, funcName, method);
    }

    public static MethodWrap appoint(String funcName, Method method) {
        return wrap(funcName, method, true);
    }

    public static MethodWrap def(String funcName, Method method) {
        return wrap(funcName, method, false);
    }

    public String getFuncName() {
        return funcName;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isAppoint() {
        return appoint;
    }

    public boolean isFound() {
        return method != null;
    }

    public boolean isNotFound() {
        return method == null;
    }
}
