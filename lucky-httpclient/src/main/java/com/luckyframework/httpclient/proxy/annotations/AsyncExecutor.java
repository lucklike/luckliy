package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
public @interface AsyncExecutor {

    /**
     * 优先级【1】<br/>
     * 使用一个<b>SpEL</b>表达式来返回一个{@link Executor}实例
     */
    String executor() default "";

    /**
     * 优先级【2】<br/>
     * 同{@link #poolName()}
     */
    @AliasFor("poolName")
    String value() default "";

    /**
     * 优先级【2】<br/>
     * 指定备用线程池{@link HttpClientProxyObjectFactory#alternativeAsyncExecutorMap}中的线程池行当前任务
     */
    @AliasFor("value")
    String poolName() default "";

    /**
     * 优先级【3】<br/>
     * 最大并发数，配置之后lucky会为当前方法创建一个专用的线程池
     * 使用{@link Executors#newFixedThreadPool(int)}创建
     */
    String concurrency() default "";

}
