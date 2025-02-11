package com.luckyframework.httpclient.proxy.stream;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.stream.sse.SseEventListener;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个流式数据监听器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface StreamListener {

    /**
     * 流式数据监听器{@link StreamEventListener}生成器
     */
    ObjectGenerate listener() default @ObjectGenerate(StreamEventListener.class);

    /**
     * 流式数据监听器{@link SseEventListener}实例的Class
     */
    Class<? extends StreamEventListener> listenerClass() default StreamEventListener.class;

    /**
     * 用于获取流式数据监听器{@link StreamEventListener}的SpEL表达式
     */
    String expression() default "";
}
