package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;

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
 *
 * @see Retryable
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RetryMeta {

    /**
     * 任务名称
     */
    String name() default "#{describe($mc$).name}";

    /**
     * 最大重试次数
     */
    int retryCount() default 0;

    /**
     * 用于创建{@link RunBeforeRetryContext}对象的生成器
     */
    ObjectGenerate beforeRetry();

    /**
     * 用于创建{@link RetryDeciderContext}重试决策者对象的生成器
     */
    ObjectGenerate decider();

}
