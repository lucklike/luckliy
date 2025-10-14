package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.retry.CallableRetryTaskNamedAdapter;
import com.luckyframework.retry.RetryFailureException;
import com.luckyframework.retry.RetryUtils;
import com.luckyframework.retry.TaskResult;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * 重试执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 04:10
 */
@SuppressWarnings("all")
public class RetryActuator {

    /**
     * 是否需要重试执行
     */
    private final boolean needRetry;

    /**
     * 任务名称
     */
    private final String taskName;

    /**
     * 最大重试次数
     */
    private final int retryCount;

    /**
     * 重试之前需要运行的逻辑
     */
    private final Function<MethodContext, RunBeforeRetryContext> beforeRetryContentFunction;

    /**
     * 重试决策者生成逻辑
     */
    private final Function<MethodContext, RetryDeciderContext> retryDeciderContentFunction;

    /**
     * 重试注解示例
     */
    private final Annotation retryAnnotation;

    /**
     * 是否开启严格模式
     */
    private final boolean strict;

    /**
     * 不需要重试的重试执行器示例
     */
    public static final RetryActuator DONT_RETRY = new RetryActuator(false, "", 0, null, null, false, null);

    /**
     * 重试执行器构造函数
     *
     * @param needRetry                   是否需要重试执行
     * @param taskName                    任务名称
     * @param retryCount                  最大重试次数
     * @param beforeRetryContentFunction  重试之前需要运行的逻辑
     * @param retryDeciderContentFunction 重试决策者生成逻辑
     * @param strict                      是否开启严格模式
     * @param retryAnnotation             重试注解示例
     */
    private RetryActuator(boolean needRetry,
                          String taskName,
                          int retryCount,
                          Function<MethodContext, RunBeforeRetryContext> beforeRetryContentFunction,
                          Function<MethodContext, RetryDeciderContext> retryDeciderContentFunction,
                          boolean strict,
                          Annotation retryAnnotation
    ) {
        this.needRetry = needRetry;
        this.taskName = taskName;
        this.retryCount = retryCount;
        this.beforeRetryContentFunction = beforeRetryContentFunction;
        this.retryDeciderContentFunction = retryDeciderContentFunction;
        this.strict = strict;
        this.retryAnnotation = retryAnnotation;
    }

    /**
     * 重试执行器构造函数
     *
     * @param taskName                    任务名称
     * @param retryCount                  最大重试次数
     * @param beforeRetryContentFunction  重试之前需要运行的逻辑
     * @param retryDeciderContentFunction 重试决策者生成逻辑
     * @param strict                      是否开启严格模式
     * @param retryAnnotation             重试注解示例
     */
    public RetryActuator(String taskName,
                         int retryCount,
                         Function<MethodContext, RunBeforeRetryContext> beforeRetryContentFunction,
                         Function<MethodContext, RetryDeciderContext> retryDeciderContentFunction,
                         boolean strict,
                         Annotation retryAnnotation
    ) {
        this(true, taskName, retryCount, beforeRetryContentFunction, retryDeciderContentFunction, strict, retryAnnotation);
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * 获取重试之前需要运行的逻辑
     *
     * @param methodContext 方法上下文
     * @return 重试之前需要运行的逻辑
     */
    public RunBeforeRetryContext<?> getBeforeRetryContext(MethodContext methodContext) {
        RunBeforeRetryContext beforeRetryContext = beforeRetryContentFunction.apply(methodContext);
        beforeRetryContext.setContext(methodContext);
        beforeRetryContext.setAnnotation(retryAnnotation);
        return beforeRetryContext;
    }

    /**
     * 获取重试决策者对象
     *
     * @param methodContext 方法上下文
     * @return 重试决策者对象
     */
    public RetryDeciderContext<Response> getRetryDeciderContext(MethodContext methodContext) {
        RetryDeciderContext<Response> retryDeciderContext = retryDeciderContentFunction.apply(methodContext);
        retryDeciderContext.setContext(methodContext);
        retryDeciderContext.setAnnotation(retryAnnotation);
        return retryDeciderContext;
    }

    /**
     * 是否需要重试执行
     *
     * @return 是否需要重试执行
     */
    public boolean isNeedRetry() {
        return needRetry;
    }

    /**
     * 获取重试注解示例
     *
     * @return 重试注解示例
     */
    public Annotation getRetryAnnotation() {
        return retryAnnotation;
    }

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * 尝试使用重试的方式执行任务
     *
     * @param task          任务
     * @param methodContext 方法上下文
     * @return 任务执行结果
     * @throws Exception 执行过程中可能出现的异常
     */
    public Response retryExecute(Callable<Response> task, MethodContext methodContext) throws Exception {
        // 不需要重试运行时，直接执行任务
        if (!isNeedRetry()) {
            return task.call();
        }

        try {
            // 尝试已重试的方式来执行任务
            RunBeforeRetryContext<?> beforeRetryContext = getBeforeRetryContext(methodContext);
            RetryDeciderContext<Response> retryDeciderContext = getRetryDeciderContext(methodContext);
            return RetryUtils.callReturn(createNamedCallabe(methodContext, task), this.retryCount, beforeRetryContext, retryDeciderContext);
        } catch (RetryFailureException rfe) {
            // 严格模式或者执行过程中存在异常时，直接抛出异常
            TaskResult taskResult = rfe.getTaskResult();
            if (strict || taskResult.hasException()) {
                throw rfe;
            }

            // 非严格模式下，如果执行过程中不存在异常，则返回最后一次的执行结果
            return (Response) taskResult.getResult();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * CallableRetryTaskNamedAdapter
     *
     * @param methodContext 方法上再问
     * @param task          重试任务
     * @return CallableRetryTaskNamedAdapter
     */
    private CallableRetryTaskNamedAdapter<Response> createNamedCallabe(MethodContext methodContext, Callable<Response> task) {
        String name = methodContext.parseExpression(this.taskName, String.class);
        return CallableRetryTaskNamedAdapter.<Response>create(name, task);
    }

}
