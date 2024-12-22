package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.Async;
import com.luckyframework.httpclient.proxy.annotations.AsyncExecutor;
import com.luckyframework.httpclient.proxy.annotations.AutoCloseResponse;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SSE结果转换器注解
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
     * 指定备用线程池{@link HttpClientProxyObjectFactory#alternativeAsyncExecutorMap}中的线程池行当前任务
     */
    @AliasFor(annotation = Async.class, attribute = "value")
    String executor() default "";
}
