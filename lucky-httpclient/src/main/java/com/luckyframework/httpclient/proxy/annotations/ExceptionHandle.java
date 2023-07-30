package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.HttpExceptionHandle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异常处理注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionHandle {

    /**
     * 异常处理器
     */
    Class<? extends HttpExceptionHandle> value();

    String handleMsg() default "";

}
