package com.luckyframework.definition;

import com.luckyframework.reflect.MethodUtils;

import java.lang.reflect.Method;

/***
 * Setter方法
 */
public class SetterValue {

    /** 方法名*/
    private final String methodName;
    /** 方法参数类型列表*/
    private final Class<?>[] parameterTypes;
    /** 方法参数列表*/
    private final Object[] parameterValues;
    /** 具体方法*/
    private Method method;


    public SetterValue(String methodName, Class<?>[] parameterTypes, Object[] parameterValues) {
        this(methodName,parameterTypes,parameterValues,null);
    }


    public SetterValue(String methodName, Class<?>[] parameterTypes, Object[] parameterValues, Method method) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameterValues = parameterValues;
        this.method = method;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getParameterValues() {
        return parameterValues;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Method getMethod() {
        return method;
    }

    public Method getMethod(Class<?> aClass){
        if(method == null){
            method = MethodUtils.getDeclaredMethod(aClass,methodName,parameterTypes);
        }
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
