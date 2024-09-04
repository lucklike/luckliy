package com.luckyframework.httpclient.proxy.sse;

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

    @AliasFor(annotation = SseListener.class, attribute = "listener")
    ObjectGenerate listener() default @ObjectGenerate(EventListener.class);

    @AliasFor(annotation = SseListener.class, attribute = "expression")
    String expression() default "";

    @AliasFor(annotation = Timeout.class, attribute = "connectionTimeout")
    int connectionTimeout() default -1;

    @AliasFor(annotation = Timeout.class, attribute = "connectionTimeoutExp")
    String connectionTimeoutExp() default "";

    @AliasFor(annotation = Timeout.class, attribute = "readTimeout")
    int readTimeout() default 600000;

    @AliasFor(annotation = Timeout.class, attribute = "readTimeoutExp")
    String readTimeoutExp() default "";

    @AliasFor(annotation = Timeout.class, attribute = "writeTimeout")
    int writeTimeout() default -1;

    @AliasFor(annotation = Timeout.class, attribute = "writeTimeoutExp")
    String writeTimeoutExp() default "";

}
