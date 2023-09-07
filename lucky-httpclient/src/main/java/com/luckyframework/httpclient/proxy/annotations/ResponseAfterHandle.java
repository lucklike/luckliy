package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.RequestAfterProcessor;
import com.luckyframework.httpclient.proxy.ResponseAfterProcessor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 响应处理注解
 * <pre>
 *    响应处理配置注解，为程序提供个性化响应结果处理的能力，开发自己的响应处理组件必须遵守以下原则：
 *    1.自定义的注解必须被@RequestProcessor注解标注
 *    2.自定义注解必须定义以下三个属性：
 *          // 请求处理器的Class
 *          Class<? extends ResponseAfterProcessor> responseProcessor() default ResponseAfterProcessor.class;
 *          // 请求处理器的额外创建信息
 *          String responseProcessorMsg() default "";
 *          // 执行的优先级，数值越小优先级越高
 *          int responsePriority() default Integer.MAX_VALUE;
 *    3.开发自己的ResponseAfterProcessor组件并设置给value属性
 * </pre>
 *
 * @see PrintLog
 * @see PrintRequestLog
 * @see PrintResponseLog
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(ResponseAfterHandles.class)
public @interface ResponseAfterHandle {

    /**
     * 响应处理器的Class
     */
    Class<? extends ResponseAfterProcessor> responseProcessor() default ResponseAfterProcessor.class;

    /**
     * 响应处理器的额外创建信息
     */
    String responseProcessorMsg() default "";

    /**
     * 执行的优先级，数值越小优先级越高
     */
    int responsePriority() default Integer.MAX_VALUE;

}
