package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.retry.RetryDeciderContent;
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
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RetryMeta {

    /**
     * 任务名称
     */
    String name() default "#{$mc$.getSimpleSignature()}";

    /**
     * 最大重试次数
     */
    int retryCount() default 0;

    /**
     * 重试等待时间生成函数
     */
    Class<? extends RunBeforeRetryContext> beforeRetry() default RunBeforeRetryContext.class;

    /**
     * 重试等待时间生成函数的额外创建信息
     */
    String beforeRetryMsg() default "";

    /**
     * 重试决策者
     */
    Class<? extends RetryDeciderContent> decider() default RetryDeciderContent.class;

    /**
     * 重试决策者的额外创建信息
     */
    String deciderMsg() default "";

}
