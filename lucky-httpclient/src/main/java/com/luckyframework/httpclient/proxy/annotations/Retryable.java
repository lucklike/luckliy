package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.retry.BackoffWaitingBeforeRetryContext;
import com.luckyframework.httpclient.proxy.retry.HttpExceptionRetryDeciderContext;
import com.luckyframework.reflect.ExtendFor;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异常重试注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExceptionHandle
@RetryMeta(
        decider = @ObjectGenerate(clazz = HttpExceptionRetryDeciderContext.class, scope = Scope.METHOD_CONTEXT),
        beforeRetry = @ObjectGenerate(BackoffWaitingBeforeRetryContext.class)
)
public @interface Retryable {
    /**
     * 任务名称
     */
    @AliasFor(annotation = RetryMeta.class, attribute = "name")
    String name() default "#{$mc$.getSimpleSignature()}";

    /**
     * 需要重试的异常列表
     */
    @AliasFor("retryFor")
    Class<? extends Throwable>[] value() default Exception.class;

    /**
     * 最大重试次数
     */
    @AliasFor(annotation = RetryMeta.class, attribute = "retryCount")
    int retryCount() default 3;

    /**
     * 重试等待时长
     */
    long waitMillis() default 1000L;

    /**
     * 最大的重试等待时间
     */
    long maxWaitMillis() default 10000L;

    /**
     * 最小的重试等待时间
     */
    long minWaitMillis() default 500L;

    /**
     * 延时倍数，下一次等待时间与上一次等待时间的比值
     */
    double multiplier() default 0D;

    /**
     * 需要重试的异常列表
     */
    Class<? extends Throwable>[] retryFor() default Exception.class;

    /**
     * 需要重试的异常列表
     */
    @AliasFor("retryFor")
    Class<? extends Throwable>[] include() default Exception.class;

    /**
     * 不需要处理的异常列表
     */
    Class<? extends Throwable>[] exclude() default {};

    /**
     * 正常情况下的HTTP响应状态码, 这些状态码以外的状态码均需要进行重试
     */
    int[] normalStatus() default {};

    /**
     * 异常情况的状态码，出现这些状态码时需要进行重试
     */
    int[] exceptionStatus() default {};

    /**
     * 重试表达式，当该表达式返回true时才有可能进行重试，<b>SpEL表达式部分需要写在#{}中，且表达式的结果必须为boolean类型</b>
     *
     * <pre>
     * SpEL表达式内置参数有：
     *  1.通用内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     * }
     * </pre>
     */
    String retryExpression() default "";

    /**
     * 同{@link ExceptionHandle#excHandleExp()}
     * 重试失败之后触发的异常处理逻辑，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *       <b>Throwable : </b>
     *       {@value TAG#THROWABLE}
     * }
     * </pre>
     */
    @ExtendFor("excHandleExp")
    String excHandleExp() default "";
}
