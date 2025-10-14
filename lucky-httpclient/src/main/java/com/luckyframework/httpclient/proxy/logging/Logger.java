package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义一个日志处理器
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
public @interface Logger {

    /**
     * 日志处理器Class
     */
    Class<? extends LoggerHandler> handlerClass() default LoggerHandler.class;

    /**
     * 用于生成日志处理器对象的对象生成器
     */
    ObjectGenerate handler() default @ObjectGenerate(LoggerHandler.class);

}
