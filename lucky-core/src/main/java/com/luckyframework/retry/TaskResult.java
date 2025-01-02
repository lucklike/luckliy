package com.luckyframework.retry;

import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.ClassUtils;

import java.util.Arrays;

import static com.luckyframework.retry.TaskResult.TaskType.RETURN;
import static com.luckyframework.retry.TaskResult.TaskType.VOID;

/**
 * 任务执行结果
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 13:07
 */
public class TaskResult<T> {

    /**
     * 任务名称
     */
    private final String name;

    /**
     * 任务类型
     */
    private final TaskType type;

    /**
     * 当下任务执行的结果
     */
    private final T result;

    /**
     * 当下任务执行时出现的异常
     */
    private final Throwable throwable;

    /**
     * 当前即将进行重试的次数
     */
    private final int retryNum;

    /**
     * 剩余的重试次数
     */
    private final int surplusNum;

    /**
     * 有返回值的任务，执行过程出现异常的情况
     *
     * @param taskName   任务名
     * @param throwable  执行过程中出现的异常
     * @param retryNum   即将重试的次数
     * @param surplusNum 剩余重试次数
     * @return 任务结果
     */
    public static <T> TaskResult<T> hasException(String taskName, Throwable throwable, int retryNum, int surplusNum) {
        return new TaskResult<>(taskName, RETURN, null, throwable, retryNum, surplusNum);
    }

    /**
     * 有返回值的任务，执行过程未出现异常的情况
     *
     * @param taskName   任务名
     * @param result     任务的返回结果
     * @param retryNum   即将重试的次数
     * @param surplusNum 剩余重试次数
     * @param <T>        任务结果的类型
     * @return 任务结果
     */
    public static <T> TaskResult<T> notException(String taskName, T result, int retryNum, int surplusNum) {
        return new TaskResult<>(taskName, RETURN, result, null, retryNum, surplusNum);
    }

    /**
     * 有返回值的任务，执行过程未出现异常但是依然需要重试的情况
     *
     * @param taskName   任务名
     * @param result     任务的返回结果
     * @param retryCause 重试的原因
     * @param retryNum   即将重试的次数
     * @param surplusNum 剩余重试次数
     * @param <T>        任务结果的类型
     * @return 任务结果
     */
    public static <T> TaskResult<T> notExceptionButNeedRetry(String taskName, T result, String retryCause, int retryNum, int surplusNum) {
        return new TaskResult<>(taskName, RETURN, result, new TaskRunSuccessButResultAbnormalException(retryCause), retryNum, surplusNum);
    }

    /**
     * 无返回值的任务，执行过程出现异常的情况
     *
     * @param taskName   任务名
     * @param throwable  执行过程中出现的异常
     * @param retryNum   即将重试的次数
     * @param surplusNum 剩余重试次数
     * @return 任务结果
     */
    public static TaskResult<Void> voidHasException(String taskName, Throwable throwable, int retryNum, int surplusNum) {
        return new TaskResult<>(taskName, VOID, null, throwable, retryNum, surplusNum);
    }

    /**
     * 无返回值的任务，执行过程正常的情况
     *
     * @param taskName   任务名
     * @param retryNum   即将重试的次数
     * @param surplusNum 剩余重试次数
     * @return 任务结果
     */
    public static TaskResult<Void> voidNotException(String taskName, int retryNum, int surplusNum) {
        return new TaskResult<>(taskName, VOID, null, null, retryNum, surplusNum);
    }


    /**
     * 私有的全参构造器
     *
     * @param name       任务名
     * @param type       任务类型
     * @param result     任务的执行结果
     * @param throwable  任务执行过程中出现的异常
     * @param retryNum   即将重试的次数
     * @param surplusNum 剩余重试次数
     */
    private TaskResult(String name, TaskType type, T result, Throwable throwable, int retryNum, int surplusNum) {
        this.name = name;
        this.type = type;
        this.result = result;
        this.throwable = throwable;
        this.surplusNum = surplusNum;
        this.retryNum = retryNum;
    }

    /**
     * 运行过程中出现的异常，未出现异常则返回null
     *
     * @return 运行过程中出现的异常，未出现异常则返回null
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * 运行结果，运行过程中出现异常或者VOID任务则会返回null
     *
     * @return 运行结果，运行过程中出现异常或者VOID任务则会返回null
     */
    public T getResult() {
        return isVoid() ? null : result;
    }

    /**
     * 当前即将进行重试的次数
     *
     * @return 当前即将进行重试的次数
     */
    public int getRetryNum() {
        return retryNum;
    }

    /**
     * 任务名
     *
     * @return 任务名
     */
    public String getName() {
        return name;
    }

    /**
     * 剩余重试次数
     *
     * @return 剩余重试次数
     */
    public int getSurplusNum() {
        return surplusNum;
    }

    /**
     * 是否有异常
     *
     * @return 是否有异常
     */
    public boolean hasException() {
        return throwable != null;
    }

    /**
     * 是否为VOID类型的任务
     *
     * @return 是否为VOID类型的任务
     */
    public boolean isVoid() {
        return type == VOID;
    }

    /**
     * 是否为RETURN类型的任务
     *
     * @return 是否为RETURN类型的任务
     */
    public boolean isReturn() {
        return type == RETURN;
    }

    /**
     * 任务是否有名称
     *
     * @return 任务是否有名称
     */
    public boolean hasName() {
        return StringUtils.hasText(name);
    }

    public TaskType getType() {
        return type;
    }

    /**
     * 执行过程中出现的异常是否属于参数列表中的某个异常
     *
     * @param throwableClasses 异常类型列表
     * @return 执行过程中出现的异常是否属于参数列表中的某个异常
     */
    @SafeVarargs
    public final boolean exceptionIsAssignableFroms(Class<? extends Throwable>... throwableClasses) {
        return hasException() && ExceptionUtils.isAssignableFrom(Arrays.asList(throwableClasses), throwable.getClass());
    }

    @Override
    public String toString() {
        return "TaskResult{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", result=" + result +
                ", throwable=" + ClassUtils.getClassName(throwable) +
                ", retryNum=" + retryNum +
                ", surplusNum=" + surplusNum +
                '}';
    }

    /**
     * 任务类型
     */
    public enum TaskType {
        /**
         * 没有返回值的任务
         */
        VOID,

        /**
         * 有返回值的任务
         */
        RETURN
    }
}
