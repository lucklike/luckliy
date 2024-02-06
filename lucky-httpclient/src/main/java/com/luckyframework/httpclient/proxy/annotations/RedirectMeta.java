package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.interceptor.RedirectInterceptor2;
import com.luckyframework.httpclient.proxy.interceptor.RedirectRule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 从定向元注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 04:34
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@InterceptorRegister(intercept = @ObjectGenerate(clazz = RedirectInterceptor2.class))
public @interface RedirectMeta {

    /**
     * 重定向规则
     */
    Class<? extends RedirectRule> rule();

}
