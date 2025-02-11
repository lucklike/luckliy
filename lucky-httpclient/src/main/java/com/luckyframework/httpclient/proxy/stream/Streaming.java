package com.luckyframework.httpclient.proxy.stream;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Timeout;
import com.luckyframework.httpclient.proxy.stream.sse.SseEventListener;
import com.luckyframework.reflect.Combination;
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
 * 将请求定义为流式请求
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Timeout
@StreamListener
@StreamResultConvert
@Combination({StreamResultConvert.class, Timeout.class})
public @interface Streaming {

    /**
     * 异步开关，默认关闭
     */
    @AliasFor(annotation = StreamResultConvert.class, attribute = "async")
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
    @AliasFor(annotation = StreamResultConvert.class, attribute = "executor")
    String executor() default "";

    /**
     * 优先级【2】<br/>
     * 最大并发数，配置之后lucky会为当前方法创建一个专用的线程池
     * 使用{@link Executors#newFixedThreadPool(int)}创建
     */
    @AliasFor(annotation = StreamResultConvert.class, attribute = "concurrency")
    String concurrency() default "";

    /**
     * 流式数据监听器{@link StreamEventListener}生成器
     */
    @AliasFor(annotation = StreamListener.class, attribute = "listener")
    ObjectGenerate listener() default @ObjectGenerate(StreamEventListener.class);

    /**
     * 流式数据件监听器{@link SseEventListener}实例的Class
     */
    @AliasFor(annotation = StreamListener.class, attribute = "listenerClass")
    Class<? extends StreamEventListener> listenerClass() default StreamEventListener.class;

    /**
     * 用于获取流式数据监听器{@link StreamEventListener}的SpEL表达式
     */
    @AliasFor(annotation = StreamListener.class, attribute = "expression")
    String expression() default "";

    /**
     * 连接超时时间
     */
    @AliasFor(annotation = Timeout.class, attribute = "connectionTimeout")
    int connectionTimeout() default -1;

    /**
     * 连接超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    @AliasFor(annotation = Timeout.class, attribute = "connectionTimeoutExp")
    String connectionTimeoutExp() default "";

    /**
     * 读取超时时间
     */
    @AliasFor(annotation = Timeout.class, attribute = "readTimeout")
    int readTimeout() default 600000;

    /**
     * 读取超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    @AliasFor(annotation = Timeout.class, attribute = "readTimeoutExp")
    String readTimeoutExp() default "";

    /**
     * 写超时时间
     */
    @AliasFor(annotation = Timeout.class, attribute = "writeTimeout")
    int writeTimeout() default -1;

    /**
     * 写超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    @AliasFor(annotation = Timeout.class, attribute = "writeTimeoutExp")
    String writeTimeoutExp() default "";

}
