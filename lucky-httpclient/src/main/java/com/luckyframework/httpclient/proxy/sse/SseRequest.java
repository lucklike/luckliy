package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
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
 * 定义一个SSL监听器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@HttpRequest
@SseListener
@SseResultConvert
@Timeout
@Combination({SseResultConvert.class, Timeout.class})
public @interface SseRequest {

    @AliasFor(annotation = HttpRequest.class, attribute = "url")
    String value() default "";

    @AliasFor(annotation = HttpRequest.class, attribute = "method")
    RequestMethod method() default RequestMethod.GET;

    @AliasFor(annotation = SseListener.class, attribute = "listener")
    ObjectGenerate listener() default @ObjectGenerate(EventListener.class);

    @AliasFor(annotation = Timeout.class, attribute = "connectionTimeout")
    int connectionTimeout() default -1;

    @AliasFor(annotation = Timeout.class, attribute = "connectionTimeoutExp")
    String connectionTimeoutExp() default "";

    @AliasFor(annotation = Timeout.class, attribute = "readTimeout")
    int readTimeout() default -1;

    @AliasFor(annotation = Timeout.class, attribute = "readTimeoutExp")
    String readTimeoutExp() default "";

    @AliasFor(annotation = Timeout.class, attribute = "writeTimeout")
    int writeTimeout() default -1;

    @AliasFor(annotation = Timeout.class, attribute = "writeTimeoutExp")
    String writeTimeoutExp() default "";

}
