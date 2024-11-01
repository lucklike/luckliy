package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 支持降级处理的异常处理器注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/01 16:38
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExceptionHandleMeta(handle = @ObjectGenerate(ExceptionFallbackHandle.class))
public @interface ExceptionFallback {

    String value() default "";

    ObjectGenerate fallback() default @ObjectGenerate(Object.class);

}
