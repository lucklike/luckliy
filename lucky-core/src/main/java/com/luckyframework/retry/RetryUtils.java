package com.luckyframework.retry;

import com.luckyframework.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * 重试相关的工具类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/22 12:45
 */
@SuppressWarnings("all")
public abstract class RetryUtils {

    private static final Logger log = LoggerFactory.getLogger(RetryUtils.class);

    /**
     * 获取任务名称，会检测传入的任务有没有实现{@link RetryTaskNamed}接口，如果实现则会调用{@link RetryTaskNamed#getTaskName()}
     * 方法获取任务名，没有实现则返回空字符串
     *
     * @param task
     * @return
     */
    private static String getTaskName(Object task) {
        if (task instanceof RetryTaskNamed) {
            return ((RetryTaskNamed) task).getTaskName();
        }
        return "";
    }

    /**
     * 打印重试中的日志
     *
     * @param taskName   任务名称
     * @param retryNum   当前要重试的次数
     * @param retryCount 剩余的重试次数
     */
    private static void printLogWithRetry(String taskName, int retryNum, int retryCount) {
        if (StringUtils.hasText(taskName)) {
            log.warn("The task named【{}】starts the {} retry. The remaining number of retries is {}.", taskName, retryNum, retryCount);
        } else {
            log.warn("The {} retry is started. The remaining number of retries is {}.", retryNum, retryCount);
        }
    }

    /**
     * 打印重试成功时的日志
     *
     * @param taskName 任务名称
     */
    private static void printLogWithSuccess(String taskName) {
        if (StringUtils.hasText(taskName)) {
            log.info("The task named【{}】was retried successfully.", taskName);
        } else {
            log.info("Task retry succeeded.");
        }
    }

    //----------------------------------------------------------------------------------------
    //                                  void task retry
    //----------------------------------------------------------------------------------------


    /**
     * 运行没有返回值的任务，如果任务失败则会尝试重试
     *
     * @param task         无返回值的任务
     * @param retryCount   重试次数
     * @param beforeRetry  重试之前执行的逻辑
     * @param retryDecider 是否进行重试的决策对象
     */
    public static void call(Runnable task, int retryCount, RunBeforeRetry beforeRetry, RetryDecider<Void> retryDecider) {
        String taskName = getTaskName(task);
        int retryNum = 1;
        TaskResult<Void> taskResult;
        try {
            task.run();
            taskResult = TaskResult.voidNotException(taskName, retryNum, retryCount);
        } catch (Throwable e) {
            taskResult = TaskResult.voidHasException(taskName, e, retryNum, retryCount);
        }

        while (retryCount > 0 && retryDecider.needRetry(taskResult)) {
            beforeRetry.beforeRetry(taskResult);
            printLogWithRetry(taskName, retryNum, retryCount - 1);
            try {
                task.run();
                taskResult = TaskResult.voidNotException(taskName, retryNum, retryCount);
            } catch (Throwable e1) {
                taskResult = TaskResult.voidHasException(taskName, e1, retryNum, retryCount);
            } finally {
                retryCount--;
                retryNum++;
            }
        }

        // retryNum == 1 说明没有进行重试，首次便运行成功了
        if (retryNum == 1) {
            return;
        }
        // retryCount == 0 重试次数用完了，此时因该再执行一次决策判断
        if (retryCount == 0 && !retryDecider.needRetry(taskResult)) {
            printLogWithSuccess(taskName);
            return;
        }

        throw new RetryFailureException(taskResult, "The retry failed, and no exception was found during the task execution, but the task was judged as a failure by the decision maker.").printException(log);
    }

    /**
     * 运行没有返回值的任务，如果任务失败则会尝试重试，最终失败会执行一个指定的任务
     *
     * @param task            无返回值的任务
     * @param retryCount      重试次数
     * @param beforeRetry     重试之前执行的逻辑
     * @param retryDecider    是否进行重试的决策对象
     * @param failureRunnable 最终重试失败之后执行的任务逻辑
     */
    public static void call(Runnable task, int retryCount, RunBeforeRetry beforeRetry, RetryDecider<Void> retryDecider, RetryFailureRunning<Void> failureRunnable) {
        try {
            call(task, retryCount, beforeRetry, retryDecider);
        } catch (RetryFailureException e) {
            failureRunnable.failureRunning(e.getTaskResult());
        }
    }

    /**
     * 执行没有返回值的任务，出现异常时尝试进行重试
     *
     * @param task        没有返回值的任务
     * @param retryCount  重试次数
     * @param beforeRetry 重试之前执行的逻辑
     */
    public static void callExRetry(Runnable task, int retryCount, RunBeforeRetry beforeRetry) {
        call(task, retryCount, beforeRetry, TaskResult::hasException);
    }

    /**
     * 执行没有返回值的任务，出现异常时尝试进行重试，最终失败会执行一个指定的任务
     *
     * @param task            没有返回值的任务
     * @param retryCount      重试次数
     * @param beforeRetry     重试之前执行的逻辑
     * @param failureRunnable 最终重试失败之后执行的任务逻辑
     */
    public static void callExRetry(Runnable task, int retryCount, RunBeforeRetry beforeRetry, RetryFailureRunning<Void> failureRunnable) {
        call(task, retryCount, beforeRetry, TaskResult::hasException, failureRunnable);
    }

    /**
     * 执行没有返回值的任务，出现异常时尝试进行重试
     *
     * @param task           没有返回值的任务
     * @param retryCount     重试次数
     * @param waitTimeMillis 重试等待时间
     */
    public static void callExRetry(Runnable task, int retryCount, long waitTimeMillis) {
        callExRetry(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis));
    }

    /**
     * 执行没有返回值的任务，出现异常时尝试进行重试，最终失败会执行一个指定的任务
     *
     * @param task            没有返回值的任务
     * @param retryCount      重试次数
     * @param waitTimeMillis  重试等待时间
     * @param failureRunnable 最终重试失败之后执行的任务逻辑
     */
    public static void callExRetry(Runnable task, int retryCount, long waitTimeMillis, RetryFailureRunning<Void> failureRunnable) {
        callExRetry(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis), failureRunnable);
    }

    /**
     * 执行没有返回值的任务，出现异常时尝试进行重试  <b>重试等待时间1秒</b>
     *
     * @param task       没有返回值的任务
     * @param retryCount 重试次数
     */
    public static void callExRetry(Runnable task, int retryCount) {
        callExRetry(task, retryCount, 1000L);
    }

    /**
     * 执行没有返回值的任务，出现异常时尝试进行重试 ，最终失败会执行一个指定的任务  <b>重试等待时间1秒</b>
     *
     * @param task            没有返回值的任务
     * @param retryCount      重试次数
     * @param failureRunnable 最终重试失败之后执行的任务逻辑
     */
    public static void callExRetry(Runnable task, int retryCount, RetryFailureRunning<Void> failureRunnable) {
        callExRetry(task, retryCount, 1000L, failureRunnable);
    }

    /**
     * 执行没有返回值的任务，出现异常时尝试进行重试
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task 没有返回值的任务
     */
    public static void callExRetry(Runnable task) {
        callExRetry(task, 3);
    }

    /**
     * 执行没有返回值的任务，出现异常时尝试进行重试，最终失败会执行一个指定的任务
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task            没有返回值的任务
     * @param failureRunnable 最终重试失败之后执行的任务逻辑
     */
    public static void callExRetry(Runnable task, RetryFailureRunning<Void> failureRunnable) {
        callExRetry(task, 3, failureRunnable);
    }

    /**
     * 执行没有返回值的任务，出现指定异常时进行重试
     *
     * @param task        没有返回值的任务
     * @param retryCount  重试次数
     * @param beforeRetry 重试之前执行的逻辑
     * @param retryExs    异常列表，只有出现这些异常时才会触发重试
     */
    @SafeVarargs
    public static void callSpecifiedExRetry(Runnable task, int retryCount, RunBeforeRetry beforeRetry, Class<? extends Throwable>... retryExs) {
        call(task, retryCount, beforeRetry, tr -> tr.exceptionIsAssignableFroms(retryExs));
    }

    /**
     * 执行没有返回值的任务，出现指定异常时进行重试，最终失败会执行一个指定的任务
     *
     * @param task            没有返回值的任务
     * @param retryCount      重试次数
     * @param beforeRetry     重试之前执行的逻辑
     * @param failureRunnable 最终重试失败之后执行的任务逻辑
     * @param retryExs        异常列表，只有出现这些异常时才会触发重试
     */
    @SafeVarargs
    public static void callSpecifiedExRetry(Runnable task, int retryCount, RunBeforeRetry beforeRetry, RetryFailureRunning<Void> failureRunnable, Class<? extends Throwable>... retryExs) {
        call(task, retryCount, beforeRetry, tr -> tr.exceptionIsAssignableFroms(retryExs), failureRunnable);
    }

    /**
     * 执行没有返回值的任务，出现指定异常时进行重试
     *
     * @param task           没有返回值的任务
     * @param retryCount     重试次数
     * @param waitTimeMillis 重试等待时间
     * @param retryExs       异常列表，只有出现这些异常时才会触发重试
     */
    @SafeVarargs
    public static void callSpecifiedExRetry(Runnable task, int retryCount, long waitTimeMillis, Class<? extends Throwable>... retryExs) {
        call(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis), tr -> tr.exceptionIsAssignableFroms(retryExs));
    }

    /**
     * 执行没有返回值的任务，出现指定异常时进行重试，最终失败会执行一个指定的任务
     *
     * @param task            没有返回值的任务
     * @param retryCount      重试次数
     * @param waitTimeMillis  重试等待时间
     * @param failureRunnable 最终重试失败之后执行的任务逻辑
     * @param retryExs        异常列表，只有出现这些异常时才会触发重试
     */
    @SafeVarargs
    public static void callSpecifiedExRetry(Runnable task, int retryCount, long waitTimeMillis, RetryFailureRunning<Void> failureRunnable, Class<? extends Throwable>... retryExs) {
        call(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis), tr -> tr.exceptionIsAssignableFroms(retryExs), failureRunnable);
    }

    /**
     * 执行没有返回值的任务，出现指定异常时进行重试, <b>重试等待时间1秒</b>
     *
     * @param task       没有返回值的任务
     * @param retryCount 重试次数
     * @param retryExs   异常列表，只有出现这些异常时才会触发重试
     */
    @SafeVarargs
    public static void callSpecifiedExRetry(Runnable task, int retryCount, Class<? extends Throwable>... retryExs) {
        callSpecifiedExRetry(task, retryCount, 1000L, retryExs);
    }

    /**
     * 执行没有返回值的任务，出现指定异常时进行重试, 最终失败会执行一个指定的任务, <b>重试等待时间1秒</b>
     *
     * @param task            没有返回值的任务
     * @param retryCount      重试次数
     * @param failureRunnable 最终重试失败之后执行的任务逻辑
     * @param retryExs        异常列表，只有出现这些异常时才会触发重试
     */
    @SafeVarargs
    public static void callSpecifiedExRetry(Runnable task, int retryCount, RetryFailureRunning<Void> failureRunnable, Class<? extends Throwable>... retryExs) {
        callSpecifiedExRetry(task, retryCount, 1000L, failureRunnable, retryExs);
    }

    /**
     * 执行没有返回值的任务，出现指定异常时进行重试
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task     没有返回值的任务
     * @param retryExs 异常列表，只有出现这些异常时才会触发重试
     */
    @SafeVarargs
    public static void callSpecifiedExRetry(Runnable task, Class<? extends Throwable>... retryExs) {
        callSpecifiedExRetry(task, 3, 1000L, retryExs);
    }

    /**
     * 执行没有返回值的任务，出现指定异常时进行重试，最终失败会执行一个指定的任务
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task            没有返回值的任务
     * @param failureRunnable 最终重试失败之后执行的任务逻辑
     * @param retryExs        异常列表，只有出现这些异常时才会触发重试
     */
    @SafeVarargs
    public static void callSpecifiedExRetry(Runnable task, RetryFailureRunning<Void> failureRunnable, Class<? extends Throwable>... retryExs) {
        callSpecifiedExRetry(task, 3, 1000L, failureRunnable, retryExs);
    }

    //----------------------------------------------------------------------------------------
    //                               non void task retry
    //----------------------------------------------------------------------------------------


    /**
     * 运行有返回值的任务，如果任务失败则会尝试重试
     *
     * @param task         有返回值的任务
     * @param retryCount   重试次数
     * @param beforeRetry  重试之前执行的逻辑
     * @param retryDecider 是否进行重试的决策对象
     * @param <T>          返回结果的类型
     * @return 重试成功后返回的结果
     */

    public static <T> T callReturn(Callable<T> task, int retryCount, RunBeforeRetry beforeRetry, RetryDecider<T> retryDecider) {
        String taskName = getTaskName(task);
        int retryNum = 1;
        TaskResult<T> taskResult;

        try {
            taskResult = TaskResult.notException(taskName, task.call(), retryNum, retryCount);
        } catch (Throwable e) {
            taskResult = TaskResult.hasException(taskName, e, retryNum, retryCount);
        }

        while (retryCount > 0 && retryDecider.needRetry(taskResult)) {
            beforeRetry.beforeRetry(taskResult);
            printLogWithRetry(taskName, retryNum, retryCount - 1);
            try {
                taskResult = TaskResult.notException(taskName, task.call(), retryNum, retryCount);
            } catch (Throwable e) {
                taskResult = TaskResult.hasException(taskName, e, retryNum, retryCount);
            } finally {
                retryCount--;
                retryNum++;
            }
        }

        // retryNum == 1 说明没有进行重试，首次便运行成功了
        if (retryNum == 1) {
            return taskResult.getResult();
        }

        // retryCount == 0 重试次数用完了，此时因该再执行一次决策判断
        if (retryCount == 0 && !retryDecider.needRetry(taskResult)) {
            printLogWithSuccess(taskName);
            taskResult.getResult();
        }
        throw new RetryFailureException(taskResult, "The retry failed, and no exception was found during the task execution, but the task was judged as a failure by the decision maker.").printException(log);
    }

    /**
     * 运行有返回值的任务，如果任务失败则会尝试重试，最终失败会返回参数中指定的默认值
     *
     * @param task          有返回值的任务
     * @param retryCount    重试次数
     * @param beforeRetry   重试之前执行的逻辑
     * @param retryDecider  是否进行重试的决策对象
     * @param failureReturn 最终重试失败之后会返回该结果
     * @param <T>           返回结果的类型
     * @return 重试成功后返回的结果
     */
    public static <T> T callReturn(Callable<T> task, int retryCount, RunBeforeRetry beforeRetry, RetryDecider<T> retryDecider, T failureReturn) {
        try {
            return callReturn(task, retryCount, beforeRetry, retryDecider);
        } catch (RetryFailureException e) {
            return failureReturn;
        }
    }

    /**
     * 运行有返回值的任务，如果任务失败则会尝试重试，最终失败会从参数中指定的RetryFailureRunning中获取结果返回
     *
     * @param task            有返回值的任务
     * @param retryCount      重试次数
     * @param beforeRetry     重试之前执行的逻辑
     * @param retryDecider    是否进行重试的决策对象
     * @param failureCallable 最终重试失败之后会执行该接口的逻辑并将返回结果返回
     * @param <T>             返回结果的类型
     * @return 重试成功后返回的结果
     */
    public static <T> T callReturn(Callable<T> task, int retryCount, RunBeforeRetry beforeRetry, RetryDecider<T> retryDecider, RetryFailureRunning<T> failureCallable) {
        try {
            return callReturn(task, retryCount, beforeRetry, retryDecider);
        } catch (RetryFailureException e) {
            return (T) failureCallable.failureRunning(e.getTaskResult());
        }
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试
     *
     * @param task        有返回结果的任务
     * @param retryCount  重试次数
     * @param beforeRetry 重试之前执行的逻辑
     * @param <T>         任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, int retryCount, RunBeforeRetry beforeRetry) {
        return callReturn(task, retryCount, beforeRetry, TaskResult::hasException);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试，最终失败会返回参数中指定的默认值
     *
     * @param task          有返回结果的任务
     * @param retryCount    重试次数
     * @param beforeRetry   重试之前执行的逻辑
     * @param failureReturn 最终重试失败之后会返回该结果
     * @param <T>           任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, int retryCount, RunBeforeRetry beforeRetry, T failureReturn) {
        return callReturn(task, retryCount, beforeRetry, TaskResult::hasException, failureReturn);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试，最终失败会从参数中指定的RetryFailureRunning中获取结果返回
     *
     * @param task            有返回结果的任务
     * @param retryCount      重试次数
     * @param beforeRetry     重试之前执行的逻辑
     * @param failureCallable 最终重试失败之后会执行该接口的逻辑并将返回结果返回
     * @param <T>             任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, int retryCount, RunBeforeRetry beforeRetry, RetryFailureRunning<T> failureCallable) {
        return callReturn(task, retryCount, beforeRetry, TaskResult::hasException, failureCallable);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试
     *
     * @param task           有返回结果的任务
     * @param retryCount     重试次数
     * @param waitTimeMillis 重试等待时间
     * @param <T>            任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, int retryCount, long waitTimeMillis) {
        return callReturnExRetry(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis));
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试，最终失败会返回参数中指定的默认值
     *
     * @param task           有返回结果的任务
     * @param retryCount     重试次数
     * @param waitTimeMillis 重试等待时间
     * @param failureReturn  最终重试失败之后会返回该结果
     * @param <T>            任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, int retryCount, long waitTimeMillis, T failureReturn) {
        return callReturnExRetry(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis), failureReturn);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试，最终失败会从参数中指定的RetryFailureRunning中获取结果返回
     *
     * @param task            有返回结果的任务
     * @param retryCount      重试次数
     * @param waitTimeMillis  重试等待时间
     * @param failureCallable 最终重试失败之后会执行该接口的逻辑并将返回结果返回
     * @param <T>             任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, int retryCount, long waitTimeMillis, RetryFailureRunning<T> failureCallable) {
        return callReturnExRetry(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis), failureCallable);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试, <b>重试等待时间1秒</b>
     *
     * @param task       有返回结果的任务
     * @param retryCount 重试次数
     * @param <T>        任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, int retryCount) {
        return callReturnExRetry(task, retryCount, 1000L);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试, 最终失败会返回参数中指定的默认值, <b>重试等待时间1秒</b>
     *
     * @param task          有返回结果的任务
     * @param retryCount    重试次数
     * @param failureReturn 最终重试失败之后会返回该结果
     * @param <T>           任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, int retryCount, T failureReturn) {
        return callReturnExRetry(task, retryCount, 1000L, failureReturn);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试, 最终失败会从参数中指定的RetryFailureRunning中获取结果返回, <b>重试等待时间1秒</b>
     *
     * @param task            有返回结果的任务
     * @param retryCount      重试次数
     * @param failureCallable 最终重试失败之后会执行该接口的逻辑并将返回结果返回
     * @param <T>             任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, int retryCount, RetryFailureRunning<T> failureCallable) {
        return callReturnExRetry(task, retryCount, 1000L, failureCallable);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task 有返回结果的任务
     * @param <T>  任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task) {
        return callReturnExRetry(task, 3);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试, 最终失败会返回参数中指定的默认值
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task          有返回结果的任务
     * @param failureReturn 最终重试失败之后会返回该结果
     * @param <T>           任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, T failureReturn) {
        return callReturnExRetry(task, 3, failureReturn);
    }

    /**
     * 执行有返回结果的任务，出现异常则进行重试, 最终失败会从参数中指定的RetryFailureRunning中获取结果返回
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task            有返回结果的任务
     * @param failureCallable 最终重试失败之后会执行该接口的逻辑并将返回结果返回
     * @param <T>             任务返回结果的类型
     * @return 任务的返回结果
     */
    public static <T> T callReturnExRetry(Callable<T> task, RetryFailureRunning<T> failureCallable) {
        return callReturnExRetry(task, 3, failureCallable);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试
     *
     * @param task        有返回结果的任务
     * @param retryCount  重试次数
     * @param beforeRetry 重试之前执行的逻辑
     * @param retryExs    异常列表，只有出现这些异常时才会触发重试
     * @param <T>         任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, int retryCount, RunBeforeRetry beforeRetry, Class<? extends Throwable>... retryExs) {
        return callReturn(task, retryCount, beforeRetry, tr -> tr.exceptionIsAssignableFroms(retryExs));
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试，最终失败会返回参数中指定的默认值
     *
     * @param task          有返回结果的任务
     * @param retryCount    重试次数
     * @param beforeRetry   重试之前执行的逻辑
     * @param failureReturn 最终重试失败之后会返回该结果
     * @param retryExs      异常列表，只有出现这些异常时才会触发重试
     * @param <T>           任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, int retryCount, RunBeforeRetry beforeRetry, T failureReturn, Class<? extends Throwable>... retryExs) {
        return callReturn(task, retryCount, beforeRetry, tr -> tr.exceptionIsAssignableFroms(retryExs), failureReturn);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试，最终失败会从参数中指定的RetryFailureRunning中获取结果返回
     *
     * @param task            有返回结果的任务
     * @param retryCount      重试次数
     * @param beforeRetry     重试之前执行的逻辑
     * @param failureCallable 最终重试失败之后会执行该接口的逻辑并将返回结果返回
     * @param retryExs        异常列表，只有出现这些异常时才会触发重试
     * @param <T>             任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, int retryCount, RunBeforeRetry beforeRetry, RetryFailureRunning<T> failureCallable, Class<? extends Throwable>... retryExs) {
        return callReturn(task, retryCount, beforeRetry, tr -> tr.exceptionIsAssignableFroms(retryExs), failureCallable);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试
     *
     * @param task           有返回结果的任务
     * @param retryCount     重试次数
     * @param waitTimeMillis 重试等待时间
     * @param retryExs       异常列表，只有出现这些异常时才会触发重试
     * @param <T>            任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, int retryCount, long waitTimeMillis, Class<? extends Throwable>... retryExs) {
        return callReturnSpecifiedExRetry(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis), retryExs);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试，最终失败会返回参数中指定的默认值
     *
     * @param task           有返回结果的任务
     * @param retryCount     重试次数
     * @param waitTimeMillis 重试等待时间
     * @param failureReturn  最终重试失败之后会返回该结果
     * @param retryExs       异常列表，只有出现这些异常时才会触发重试
     * @param <T>            任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, int retryCount, long waitTimeMillis, T failureReturn, Class<? extends Throwable>... retryExs) {
        return callReturnSpecifiedExRetry(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis), failureReturn, retryExs);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试，最终失败会从参数中指定的RetryFailureRunning中获取结果返回
     *
     * @param task            有返回结果的任务
     * @param retryCount      重试次数
     * @param waitTimeMillis  重试等待时间
     * @param failureCallable 最终重试失败之后会执行该接口的逻辑并将返回结果返回
     * @param retryExs        异常列表，只有出现这些异常时才会触发重试
     * @param <T>             任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, int retryCount, long waitTimeMillis, RetryFailureRunning<T> failureCallable, Class<? extends Throwable>... retryExs) {
        return callReturnSpecifiedExRetry(task, retryCount, new FixedWaitBeforeRetry(waitTimeMillis), failureCallable, retryExs);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试, <b>重试等待时间1秒</b>
     *
     * @param task       有返回结果的任务
     * @param retryCount 重试次数
     * @param retryExs   异常列表，只有出现这些异常时才会触发重试
     * @param <T>        任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, int retryCount, Class<? extends Throwable>... retryExs) {
        return callReturnSpecifiedExRetry(task, retryCount, 1000L, retryExs);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试，最终失败会返回参数中指定的默认值, <b>重试等待时间1秒</b>
     *
     * @param task          有返回结果的任务
     * @param retryCount    重试次数
     * @param failureReturn 最终重试失败之后会返回该结果
     * @param retryExs      异常列表，只有出现这些异常时才会触发重试
     * @param <T>           任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, int retryCount, T failureReturn, Class<? extends Throwable>... retryExs) {
        return callReturnSpecifiedExRetry(task, retryCount, 1000L, failureReturn, retryExs);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试，最终失败会从参数中指定的RetryFailureRunning中获取结果返回, <b>重试等待时间1秒</b>
     *
     * @param task            有返回结果的任务
     * @param retryCount      重试次数
     * @param failureCallable 最终重试失败之后会执行该接口的逻辑并将返回结果返回
     * @param retryExs        异常列表，只有出现这些异常时才会触发重试
     * @param <T>             任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, int retryCount, RetryFailureRunning<T> failureCallable, Class<? extends Throwable>... retryExs) {
        return callReturnSpecifiedExRetry(task, retryCount, 1000L, failureCallable, retryExs);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task     有返回结果的任务
     * @param retryExs 异常列表，只有出现这些异常时才会触发重试
     * @param <T>      任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, Class<? extends Throwable>... retryExs) {
        return callReturnSpecifiedExRetry(task, 3, retryExs);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试, 最终失败会返回参数中指定的默认值
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task          有返回结果的任务
     * @param failureReturn 最终重试失败之后会返回该结果
     * @param retryExs      异常列表，只有出现这些异常时才会触发重试
     * @param <T>           任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, T failureReturn, Class<? extends Throwable>... retryExs) {
        return callReturnSpecifiedExRetry(task, 3, failureReturn, retryExs);
    }

    /**
     * 执行有返回结果的任务，出现指定异常时才会进行重试, 最终失败会从参数中指定的RetryFailureRunning中获取结果返回
     * <pre>
     *     <b>1.重试3次</b>
     *     <b>2.重试等待时间1秒</b>
     * </pre>
     *
     * @param task            有返回结果的任务
     * @param failureCallable 最终重试失败之后会执行该接口的逻辑并将返回结果返回
     * @param retryExs        异常列表，只有出现这些异常时才会触发重试
     * @param <T>             任务返回结果的类型
     * @return 任务的返回结果
     */
    @SafeVarargs
    public static <T> T callReturnSpecifiedExRetry(Callable<T> task, RetryFailureRunning<T> failureCallable, Class<? extends Throwable>... retryExs) {
        return callReturnSpecifiedExRetry(task, 3, failureCallable, retryExs);
    }

}
