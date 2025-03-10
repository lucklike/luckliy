package com.luckyframework.httpclient.proxy.spel.hook;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.async.Model;
import com.luckyframework.threadpool.ThreadPoolFactory;
import com.luckyframework.threadpool.ThreadPoolParam;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;

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
     * <pre>
     *   指定异步任务的执行器（支持SpEL表达式）
     *     1.如果表达式结果类型为{@link Executor}时直接使用该执行器
     *     2.如果表达式结果类型为{@link ThreadPoolParam}时，使用{@link ThreadPoolFactory#createThreadPool(ThreadPoolParam)}来创建执行器
     *     3.如果表达式结果类型为{@link String}时，使用{@link HttpClientProxyObjectFactory#getAlternativeAsyncExecutor(String)}来获取执行器
     *     4.返回结果为其他类型时将报错
     * </pre>
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
