package com.luckyframework.httpclient.proxy.spel;

import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 参数信息类
 */
public class ParameterInfo {

    /**
     * 方法实例
     */
    @NonNull
    private final Method method;

    /**
     * 参数实例
     */
    @NonNull
    private final Parameter parameter;

    /**
     * 参数类型
     */
    @NonNull
    private final ResolvableType resolvableType;

    /**
     * 构造函数
     *
     * @param parameter      参数实例
     * @param resolvableType 参数类型
     */
    private ParameterInfo(@NonNull Method method,
                          @NonNull Parameter parameter,
                          @NonNull ResolvableType resolvableType
    ) {
        this.method = method;
        this.parameter = parameter;
        this.resolvableType = resolvableType;
    }

    /**
     * 创建一个参数信息
     *
     * @param method 参数所在方法实例
     * @param index  参数在方法中的位置
     * @return 参数信息
     */
    public static ParameterInfo create(Method method, int index) {
        return new ParameterInfo(method, method.getParameters()[index], ResolvableType.forMethodParameter(method, index));
    }

    /**
     * 创建一个参数信息
     *
     * @param parameter      参数实例
     * @param resolvableType 参数类型
     * @return 参数信息
     */
    public static ParameterInfo create(Method method, Parameter parameter, ResolvableType resolvableType) {
        return new ParameterInfo(method, parameter, resolvableType);
    }

    /**
     * 获取方法实例
     *
     * @return 方法实例
     */
    @NonNull
    public Method getMethod() {
        return method;
    }

    /**
     * 获取参数实例
     *
     * @return 参数实例
     */
    @NonNull
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * 获取参数类型
     *
     * @return 参数类型
     */
    @NonNull
    public ResolvableType getResolvableType() {
        return resolvableType;
    }
}
