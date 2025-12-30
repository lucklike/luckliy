package com.luckyframework.httpclient.proxy.spel;

import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Supplier;

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
     * 参数名
     */
    @NonNull
    private final String parameterName;

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
     * 包装类型
     */
    @NonNull
    private final WrapType wrapType;

    /**
     * 构造函数
     *
     * @param method         参数所在方法实例
     * @param parameter      参数实例
     * @param parameterName  参数名称
     * @param resolvableType 参数类型
     */
    private ParameterInfo(@NonNull Method method,
                          @NonNull Parameter parameter,
                          @Nullable String parameterName,
                          @NonNull ResolvableType resolvableType
    ) {
        this.method = method;
        this.parameter = parameter;
        this.parameterName = parameterName == null ? parameter.getName() : parameterName;
        this.resolvableType = resolvableType;
        this.wrapType = WrapType.of(parameter.getType());
    }

    /**
     * 创建一个参数信息
     *
     * @param method        参数所在方法实例
     * @param parameterName 参数名称
     * @param index         参数在方法中的位置
     * @return 参数信息
     */
    public static ParameterInfo create(Method method, String parameterName, int index) {
        return new ParameterInfo(method, method.getParameters()[index], parameterName, ResolvableType.forMethodParameter(method, index));
    }

    /**
     * 创建一个参数信息
     *
     * @param method         参数所在方法实例
     * @param parameter      参数实例
     * @param parameterName  参数名称
     * @param resolvableType 参数类型
     * @return 参数信息
     */
    public static ParameterInfo create(Method method, Parameter parameter, String parameterName, ResolvableType resolvableType) {
        return new ParameterInfo(method, parameter, parameterName, resolvableType);
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
     * 获取参数名称
     *
     * @return 参数名称
     */
    @NonNull
    public String getParameterName() {
        return parameterName;
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

    /**
     * 获取包装类型
     *
     * @return 包装类型
     */
    @NonNull
    public WrapType getWrapType() {
        return wrapType;
    }

    /**
     * 获取真实类型{@link ResolvableType}
     *
     * @return 真实类型{@link ResolvableType}
     */
    @NonNull
    public ResolvableType getTargetResolvableType() {
        return wrapType.getTargetType(resolvableType);
    }

    /**
     * 获取真实类型{@link Class}
     *
     * @return 真实类型{@link Class}
     */
    @NonNull
    public Class<?> getTargetClass() {
        ResolvableType targetResolvableType = getTargetResolvableType();
        Class<?> resolve = targetResolvableType.resolve();
        return resolve == null ? Object.class : resolve;
    }

    /**
     * 对Value进行包装
     *
     * @param value 待包装的值
     * @return 包装后的值
     */
    public Object wrapValue(Object value) {
        return wrapType.wrap(() -> value);
    }

    /**
     * 对Value进行包装
     *
     * @param valueSupplier 待包装的值
     * @return 包装后的值
     */
    public Object wrapValue(Supplier<?> valueSupplier) {
        return wrapType.wrap(valueSupplier);
    }
}
