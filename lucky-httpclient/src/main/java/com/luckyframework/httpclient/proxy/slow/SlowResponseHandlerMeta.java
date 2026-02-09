package com.luckyframework.httpclient.proxy.slow;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SlowResponseHandlerMeta {

    /**
     * 定义慢响应时间，接口响应时间超过该时间则会被认为是慢响应（单位：毫秒）
     */
    long slowTime() default -1L;

    /**
     * 慢响应处理器生成器
     */
    ObjectGenerate slowHandler() default @ObjectGenerate(SlowResponseHandler.class);

    /**
     * 慢响应处理器的Class
     */
    Class<? extends SlowResponseHandler> slowHandlerClass() default SlowResponseHandler.class;
}
