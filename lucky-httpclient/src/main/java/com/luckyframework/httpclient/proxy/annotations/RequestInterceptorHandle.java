package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.RequestInterceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求处理注解
 * <pre>
 *    请求处理配置注解，为程序提供个性化的请求处理能力，开发自己的请求处理数组件必须遵守以下原则：
 *    1.自定义的注解必须被@RequestProcessor注解标注
 *    2.自定义注解必须定义以下三个属性：
 *          // 请求处理器的Class
 *          Class<? extends RequestAfterProcessor> requestProcessor() default RequestAfterProcessor.class;
 *          // 请求处理器的额外创建信息
 *          String requestProcessorMsg() default "";
 *          // 执行的优先级，数值越小优先级越高
 *          int requestPriority() default Integer.MAX_VALUE;
 *    3.开发自己的RequestAfterProcessor组件并设置给value属性
 * </pre>
 *
 * @see PrintLog
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(RequestInterceptorHandles.class)
public @interface RequestInterceptorHandle {

    /**
     * 请求处理器的Class
     */
    Class<? extends RequestInterceptor> requestProcessor() default RequestInterceptor.class;

    /**
     * 请求处理器的额外创建信息
     */
    String requestProcessorMsg() default "";

    /**
     * 执行的优先级，数值越小优先级越高
     */
    int requestPriority() default Integer.MAX_VALUE;

}
