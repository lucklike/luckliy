package com.luckyframework.httpclient.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 注解环境
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/25 10:53
 */
public class AnnotationEnv {

    private final Class<?> clazz;
    private final Method method;

    public AnnotationEnv(Class<?> clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }

//    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
//
//    }




}
