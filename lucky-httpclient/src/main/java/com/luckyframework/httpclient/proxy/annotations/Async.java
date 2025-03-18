package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.async.Model;
import com.luckyframework.threadpool.ThreadPoolFactory;
import com.luckyframework.threadpool.ThreadPoolParam;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

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
     * 同{@link #executor()}
     */
    @AliasFor(annotation = AsyncExecutor.class, attribute = "value")
    String value() default "";

    /**
     * <pre>
     *   指定异步任务的执行器（支持SpEL表达式）
     *     1.如果表达式结果类型为{@link Executor}时直接使用该执行器
     *     2.如果表达式结果类型为{@link ThreadPoolParam}时，使用{@link ThreadPoolFactory#createThreadPool(ThreadPoolParam)}来创建执行器
     *     3.如果表达式结果类型为{@link String}时，使用{@link HttpClientProxyObjectFactory#getAlternativeAsyncExecutor(String)}来获取执行器
     *     4.返回结果为其他类型时将报错
     * </pre>
     */
    @AliasFor(annotation = AsyncExecutor.class, attribute = "executor")
    String executor() default "";

    /**
     * 最大并发数
     * <pre>
     *     Java线程模型下  ：使用{@link Semaphore}进行并发控制
     *     Kotlin协程模型下：使用{@link kotlinx.coroutines.CoroutineDispatcher#limitedParallelism(int)}进行并发控制
     * </pre>
     */
    @AliasFor(annotation = AsyncExecutor.class, attribute = "concurrency")
    String concurrency() default "";

    /**
     * 异步模型，默认使用公用的异步模型
     */
    @AliasFor(annotation = AsyncExecutor.class, attribute = "model")
    Model model() default Model.USE_COMMON;


    /**
     * 异步开关， 默认开启
     */
    boolean enable() default true;


}
