package com.luckyframework.httpclient.proxy.slow;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 处理慢响应的注解
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SlowResponseHandlerMeta(slowHandlerClass = DefaultSlowResponseHandler.class)
public @interface HandlerSlowResponse {

    /**
     * 定义慢响应时间，超过此时间时会被标记为慢响应
     */
    String slowTime();

    /**
     * 慢响应处理器生成器
     */
    ObjectGenerate handler() default @ObjectGenerate(SlowResponseHandlerFunction.class);

    /**
     * 慢响应处理器的Class
     */
    Class<? extends SlowResponseHandlerFunction> handlerClass() default SlowResponseHandlerFunction.class;
}
