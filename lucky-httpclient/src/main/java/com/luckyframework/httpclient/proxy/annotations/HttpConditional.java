package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.RequestAfterProcessor;
import com.luckyframework.httpclient.proxy.ResponseAfterProcessor;
import com.luckyframework.httpclient.proxy.impl.PrintLogProcessor;
import com.luckyframework.httpclient.proxy.impl.RequestAndResponseCondition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求响应日志输出处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@RequestAfterHandle
@ResponseAfterHandle
public @interface HttpConditional {

    /**
     * 请求实例必须满足的条件
     */
    String[] request() default {};

    /**
     * 响应实例必须满足的条件
     */
    String[] response() default {};

    /**
     * 请求处理器的Class
     */
    Class<? extends RequestAfterProcessor> requestProcessor() default RequestAndResponseCondition.class;

    /**
     * 请求处理器的额外创建信息
     */
    String requestProcessorMsg() default "";

    /**
     * 请求处理器执行的优先级，数值越小优先级越高
     */
    int requestPriority() default Integer.MIN_VALUE + 1;

    /**
     * 响应处理器的Class
     */
    Class<? extends ResponseAfterProcessor> responseProcessor() default RequestAndResponseCondition.class;

    /**
     * 响应处理器的额外创建信息
     */
    String responseProcessorMsg() default "";

    /**
     * 响应处理器执行的优先级，数值越小优先级越高
     */
    int responsePriority() default Integer.MIN_VALUE + 1;

}
