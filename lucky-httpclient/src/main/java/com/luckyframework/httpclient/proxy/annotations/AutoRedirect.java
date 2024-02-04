package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.interceptor.RedirectInterceptor;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动执行重定向的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 04:34
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(InterceptorRegister.class)
@InterceptorRegister(intercept = RedirectInterceptor.class, priority = Integer.MAX_VALUE - 1)
public @interface AutoRedirect {
}
