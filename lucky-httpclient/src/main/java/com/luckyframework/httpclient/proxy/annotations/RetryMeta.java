package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;
import com.luckyframework.retry.RetryFailureException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 重试元注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 * @see Retryable
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RetryMeta {

    /**
     * 是否开启重试功能
     */
    String enable() default "true";

    /**
     * 任务名称
     */
    String name() default "#{$api$.name}";

    /**
     * 最大重试次数
     */
    String retryCount() default "0";

    /**
     * 用于创建{@link RunBeforeRetryContext}对象的生成器
     */
    ObjectGenerate beforeRetry();

    /**
     * 用于创建{@link RetryDeciderContext}重试决策者对象的生成器
     */
    ObjectGenerate decider();

    /**
     * 是否开启严格模式
     * <pre>
     *     严格模式下：重试流程结束后，不管有没有发生异常都会抛出{@link RetryFailureException}异常
     *     非严格模式下：重试流程结束后，如果没有发生异常时则直接返回最后一次调用的结果
     * </pre>
     */
    boolean strict() default false;

}
