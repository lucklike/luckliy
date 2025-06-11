package com.luckyframework.httpclient.proxy.spel;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 参数信息类
 */
public class ParameterInfo {

    /**
     * 参数实例
     */
    private final Parameter parameter;

    /**
     * 参数类型
     */
    private final ResolvableType resolvableType;

    /**
     * 构造函数
     *
     * @param parameter      参数实例
     * @param resolvableType 参数类型
     */
    private ParameterInfo(Parameter parameter, ResolvableType resolvableType) {
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
        return new ParameterInfo(method.getParameters()[index], ResolvableType.forMethodParameter(method, index));
    }

    /**
     * 创建一个参数信息
     *
     * @param parameter      参数实例
     * @param resolvableType 参数类型
     * @return 参数信息
     */
    public static ParameterInfo create(Parameter parameter, ResolvableType resolvableType) {
        return new ParameterInfo(parameter, resolvableType);
    }

    /**
     * 获取参数实例
     *
     * @return 参数实例
     */
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * 获取参数类型
     *
     * @return 参数类型
     */
    public ResolvableType getResolvableType() {
        return resolvableType;
    }
}
