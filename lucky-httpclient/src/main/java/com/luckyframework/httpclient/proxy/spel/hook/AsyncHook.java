package com.luckyframework.httpclient.proxy.spel.hook;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.async.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异步线程池选择器，用于指定执行异步HTTP任务的线程池
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface AsyncHook {

    /**
     * 指定备用线程池{@link HttpClientProxyObjectFactory#alternativeAsyncExecutorMap}中的线程池行当前任务
     */
    String value() default "";

    /**
     * 是否异步的执行
     */
    boolean async() default true;

    /**
     * 异步模型，默认使用公用的异步模型
     */
    Model model() default Model.USE_COMMON;
}
