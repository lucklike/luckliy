package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.interceptor.Interceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注册拦截器的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 02:53
 *
 * @see AutoRedirect
 * @see PrintLog
 * @see PrintResponseLog
 * @see PrintRequestLog
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(InterceptorRegisters.class)
public @interface InterceptorRegister {

    /**
     * 用于生成{@link Interceptor} 拦截器对象的生成器
     */
    ObjectGenerate intercept();

    /**
     * 优先级，数值越高优先级越低
     */
    int priority() default Integer.MAX_VALUE;

}
