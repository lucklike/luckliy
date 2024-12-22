package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.Async;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Timeout;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将请求定义为SSE请求
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
@SseListener
@SseResultConvert
@Combination({SseResultConvert.class, Timeout.class})
public @interface Sse {

    /**
     * 异步开关，默认关闭
     */
    @AliasFor(annotation = SseResultConvert.class, attribute = "async")
    boolean async() default false;

    /**
     * 指定备用线程池{@link HttpClientProxyObjectFactory#alternativeAsyncExecutorMap}中的线程池行当前任务
     */
    @AliasFor(annotation = SseResultConvert.class, attribute = "executor")
    String executor() default "";

    /**
     * SSE事件监听器{@link EventListener}生成器
     */
    @AliasFor(annotation = SseListener.class, attribute = "listener")
    ObjectGenerate listener() default @ObjectGenerate(EventListener.class);

    /**
     * SSE事件监听器{@link EventListener}实例的Class
     */
    @AliasFor(annotation = SseListener.class, attribute = "listenerClass")
    Class<? extends EventListener> listenerClass() default EventListener.class;

    /**
     * 用于获取SSE监听器{@link EventListener}的SpEL表达式
     */
    @AliasFor(annotation = SseListener.class, attribute = "expression")
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
