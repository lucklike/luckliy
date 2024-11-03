package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.handle.ExceptionFallback;
import com.luckyframework.httpclient.proxy.handle.HttpExceptionHandle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异常处理注解
 * @see ExceptionHandle
 * @see ExceptionReturn
 * @see ExceptionFallback
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ExceptionHandleMeta {

    /**
     * 用于生成{@link HttpExceptionHandle}异常处理器的对象生成器
     */
    ObjectGenerate handle();

    /**
     * 条件表达式，当条件表达式成立时使用该处理器
     */
    String condition() default "";

    /**
     * 需要该处理器处理的异常
     */
    Class<? extends Throwable>[] exceptions() default {Exception.class};

}
