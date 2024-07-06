package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异步执行注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AsyncExecutor
public @interface Async {

    /**
     * 指定备用线程池{@link HttpClientProxyObjectFactory#alternativeAsyncExecutorMap}中的线程池行当前任务
     */
    @AliasFor(annotation = AsyncExecutor.class, attribute = "value")
    String value() default "";

    /**
     * 异步开关， 默认开启
     */
    boolean enable() default true;


}
