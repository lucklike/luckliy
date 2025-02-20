package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.sse.standard.StandardEventListener;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个SSE事件监听器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SseListener {

    /**
     * SSE事件监听器{@link EventListener}生成器
     */
    ObjectGenerate listener() default @ObjectGenerate(EventListener.class);

    /**
     * SSE事件监听器{@link StandardEventListener}实例的Class
     */
    Class<? extends EventListener> listenerClass() default EventListener.class;

    /**
     * 用于获取SSE事件监听器{@link EventListener}的SpEL表达式
     */
    String expression() default "";
}
