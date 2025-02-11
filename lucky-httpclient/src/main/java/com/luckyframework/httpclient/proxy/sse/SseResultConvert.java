package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.Async;
import com.luckyframework.httpclient.proxy.annotations.AutoCloseResponse;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
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
import java.util.concurrent.Executors;

/**
 * SSE响应结果转换器注解
 * standard
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Async
@AutoCloseResponse(false)
@ResultConvertMeta(convert = @ObjectGenerate(SseResponseConvert.class))
public @interface SseResultConvert {

    /**
     * 异步开关，默认关闭
     */
    @AliasFor(annotation = Async.class, attribute = "enable")
    boolean async() default false;

    /**
     * 优先级【1】<br/>
     * <pre>
     *   指定异步任务的执行器（支持SpEL表达式）
     *     1.如果表达式结果类型为{@link Executor}时直接使用该执行器
     *     2.如果表达式结果类型为{@link ThreadPoolParam}时，使用{@link ThreadPoolFactory#createThreadPool(ThreadPoolParam)}来创建执行器
     *     3.如果表达式结果类型为{@link String}时，使用{@link HttpClientProxyObjectFactory#getAlternativeAsyncExecutor(String)}来获取执行器
     *     4.返回结果为其他类型时将报错
     * </pre>
     */
    @AliasFor(annotation = Async.class, attribute = "executor")
    String executor() default "";

    /**
     * 优先级【2】<br/>
     * 最大并发数，配置之后lucky会为当前方法创建一个专用的线程池
     * 使用{@link Executors#newFixedThreadPool(int)}创建
     */
    @AliasFor(annotation = Async.class, attribute = "concurrency")
    String concurrency() default "";

}
