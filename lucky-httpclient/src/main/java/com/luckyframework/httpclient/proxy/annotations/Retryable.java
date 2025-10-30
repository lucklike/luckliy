package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.retry.BackoffWaitingBeforeRetryContext;
import com.luckyframework.httpclient.proxy.retry.ExceptionModel;
import com.luckyframework.httpclient.proxy.retry.HttpExceptionRetryDeciderContext;
import com.luckyframework.retry.RetryFailureException;
import org.springframework.core.annotation.AliasFor;
import sun.net.ConnectionResetException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLProtocolException;
import java.io.InterruptedIOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.util.concurrent.TimeoutException;

/**
 * 异常重试注解
 * 约定配置
 * <pre>
 *     当检测到当前类中存在方法名+$NeedRetry的静态方法时，会使用此方法来决定是否进行重试
 *     {@code
 *
 *          @Retryable
 *          @Get("/retry")
 *          void testRetry()
 *
 *          // 决定testRetry方法是否进行重试的方法
 *          static boolean testRetry$NeedRetry(MethodContext context, TaskResult<Response> taskResult) {
 *
 *             .......
 *
 *             return false
 *          }
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@RetryMeta(
        decider = @ObjectGenerate(clazz = HttpExceptionRetryDeciderContext.class, scope = Scope.METHOD_CONTEXT),
        beforeRetry = @ObjectGenerate(BackoffWaitingBeforeRetryContext.class)
)
public @interface Retryable {

    /**
     * 是否开启重试功能
     */
    @AliasFor(annotation = RetryMeta.class, attribute = "enable")
    String enable() default "true";

    /**
     * 任务名称
     */
    @AliasFor(annotation = RetryMeta.class, attribute = "name")
    String name() default "[#{T(Thread).currentThread().getName()}][#{$unique_id$}][#{$api$.name}]";

    /**
     * 异常校验模型
     * <pre>
     *     CHECK_ROOT_CAUSE : 检验根异常
     *     CHECK_TOP_CAUSE  : 检验顶层异常
     *     CHECK_ALL_STACK  : 检验所有异常堆栈中出现的异常
     * </pre>
     */
    ExceptionModel exCheckModel() default ExceptionModel.CHECK_ROOT_CAUSE;

    /**
     * 异常排除模型
     * <pre>
     *     CHECK_ROOT_CAUSE : 检验根异常
     *     CHECK_TOP_CAUSE  : 检验顶层异常
     *     CHECK_ALL_STACK  : 检验所有异常堆栈中出现的异常
     * </pre>
     */
    ExceptionModel exExcludeModel() default ExceptionModel.CHECK_ALL_STACK;

    /**
     * 需要重试的异常列表
     */
    @AliasFor("retryFor")
    Class<? extends Throwable>[] value() default {
            ConnectException.class,
            UnknownHostException.class,
            NoRouteToHostException.class,
            SocketException.class,
            SocketTimeoutException.class,
            ConnectionResetException.class,
            PortUnreachableException.class,
            UnknownServiceException.class,
            SSLHandshakeException.class,
            SSLProtocolException.class,
            SSLPeerUnverifiedException.class,
            InterruptedIOException.class,
            TimeoutException.class
    };

    /**
     * 最大重试次数
     */
    @AliasFor(annotation = RetryMeta.class, attribute = "retryCount")
    String retryCount() default "3";

    /**
     * 是否开启严格模式
     * <pre>
     *     严格模式下：重试流程结束后，不管有没有发生异常都会抛出{@link RetryFailureException}异常
     *     非严格模式下：重试流程结束后，如果没有发生异常时则直接返回最后一次调用的结果
     * </pre>
     */
    @AliasFor(annotation = RetryMeta.class, attribute = "strict")
    boolean strict() default false;

    /**
     * 重试等待时长
     */
    String waitMillis() default "1000";

    /**
     * 最大的重试等待时间
     */
    String maxWaitMillis() default "10000";

    /**
     * 最小的重试等待时间
     */
    String minWaitMillis() default "500";

    /**
     * 延时倍数，下一次等待时间与上一次等待时间的比值
     */
    String multiplier() default "0";

    /**
     * 需要重试的异常列表
     */
    @AliasFor("value")
    Class<? extends Throwable>[] retryFor() default {
            ConnectException.class,
            UnknownHostException.class,
            NoRouteToHostException.class,
            SocketException.class,
            SocketTimeoutException.class,
            ConnectionResetException.class,
            PortUnreachableException.class,
            UnknownServiceException.class,
            SSLHandshakeException.class,
            SSLProtocolException.class,
            SSLPeerUnverifiedException.class,
            InterruptedIOException.class,
            TimeoutException.class
    };

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
     * @see SpELVariableNote
     */
    String condition() default "#{($status$ >= 500 and $status$ < 600) or {408, 429}.contains($status$)}";

    /**
     * 指定上下文中的某个SpEL函数来，让这个函数来决定当前任务是否需要重试
     */
    String conditionFunc() default "";
}
