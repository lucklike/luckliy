package com.luckyframework.httpclient.proxy.configapi.parse;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.retry.ExceptionModel;
import sun.net.ConnectionResetException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLProtocolException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.util.concurrent.TimeoutException;

/**
 * 重试相关的配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/9/17 01:04
 */
public class RetryConfiguration {

    /**
     * 默认的重试表达式
     */
    public static final String DEFAULT_CONDITION = "#{($status$ >= 500 and $status$ < 600) or {408, 429}.contains($status$)}";

    /**
     * 是否开启重试功能
     */
    private boolean enable = false;

    /**
     * 是否开启严格模式
     * <pre>
     *  严格模式下：重试流程结束后，不管有没有发生异常都会抛出RetryFailureException异常
     *  非严格模式下：重试流程结束后，如果没有发生异常时则直接返回最后一次调用的结果
     * </pre>
     */
    private boolean strictModel = false;

    /**
     * 任务名称
     */
    private String taskNameFormat = "[#{T(Thread).currentThread().getName()}][#{$req$.getUniqueId()}][#{$mec$.getMethodString()}]";

    /**
     * 最大重试次数，默认 3 次
     */
    private int count = 3;

    /**
     * 重试等待时长，默认 1 秒
     */
    private long waitMillis = 1000L;

    /**
     * 最大的重试等待时间，默认 10 秒
     */
    private long maxWaitMillis = 10000L;

    /**
     * 最小的重试等待时间，默认 0.5 秒
     */
    private long minWaitMillis = 500L;

    /**
     * 延时倍数，下一次等待时间与上一次等待时间的比值
     */
    private double multiplier = 0D;

    /**
     * 重试表达式，当该表达式返回true时才有可能进行重试
     */
    private String condition;

    /**
     * 重试函数，指定一个函数让该函数来觉得是否需要重试
     */
    private String conditionFunc;

    /**
     * 指定正常状态的状态码，响应的状态码在此范围内时，则不需要重试
     */
    private int[] normalStatus = {};

    /**
     * 指定异常状态的状态码，响应的状态码在此范围内时，则需要重试
     */
    private int[] exceptionStatus = {};

    /**
     * 指定需要重试的异常，出现这类异常时则需要进行重试
     */
    private Class<? extends Throwable>[] exceptionClasses = new Class[]{ConnectException.class, UnknownHostException.class, NoRouteToHostException.class, SocketException.class, SocketTimeoutException.class, ConnectionResetException.class, PortUnreachableException.class, UnknownServiceException.class, SSLHandshakeException.class, SSLProtocolException.class, SSLPeerUnverifiedException.class, InterruptedIOException.class, TimeoutException.class};

    /**
     * 指定需要排除的异常类型，出现这类异常时不需要进行重试
     */
    private Class<? extends Throwable>[] excludeClasses = new Class[0];


    /**
     * 异常校验模型
     */
    private ExceptionModel exCheckModel = ExceptionModel.CHECK_ROOT_CAUSE;

    /**
     * 异常排除模型
     */
    private ExceptionModel exExcludeModel = ExceptionModel.CHECK_ALL_STACK;


    /**
     * 是否开启重试功能
     *
     * @return 是否开启重试功能
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * 设置是否开启重试功能
     *
     * @param enable 是否开启重试功能
     */
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * 是否开启严格模式
     * <pre>
     *  严格模式下：重试流程结束后，不管有没有发生异常都会抛出RetryFailureException异常
     *  非严格模式下：重试流程结束后，如果没有发生异常时则直接返回最后一次调用的结果
     * </pre>
     *
     * @return 是否开启严格模式
     */
    public boolean isStrictModel() {
        return strictModel;
    }

    /**
     * 设置是否开启严格模式
     * <pre>
     *  严格模式下：重试流程结束后，不管有没有发生异常都会抛出RetryFailureException异常
     *  非严格模式下：重试流程结束后，如果没有发生异常时则直接返回最后一次调用的结果
     * </pre>
     *
     * @param strict 是否开启严格模式
     */
    public void setStrictModel(boolean strict) {
        this.strictModel = strict;
    }

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    public String getTaskNameFormat() {
        return taskNameFormat;
    }

    /**
     * 设置任务名称，支持 SpEL 表达式
     *
     * @param taskNameFormat 任务名称
     */
    public void setTaskNameFormat(String taskNameFormat) {
        this.taskNameFormat = taskNameFormat;
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    public int getCount() {
        return count;
    }

    /**
     * 设置最大重试次数，默认 3 次
     *
     * @param count 最大重试次数
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * 获取重试等待时长
     *
     * @return 重试等待时长
     */
    public long getWaitMillis() {
        return waitMillis;
    }

    /**
     * 设置重试等待时长，默认 1 秒
     *
     * @param waitMillis 重试等待时长
     */
    public void setWaitMillis(long waitMillis) {
        this.waitMillis = waitMillis;
    }

    /**
     * 获取最大的重试等待时间
     *
     * @return 最大的重试等待时间
     */
    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    /**
     * 设置最大的重试等待时间，默认 10 秒
     *
     * @param maxWaitMillis 最大的重试等待时间
     */
    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    /**
     * 获取最小的重试等待时间
     *
     * @return 最小的重试等待时间
     */
    public long getMinWaitMillis() {
        return minWaitMillis;
    }

    /**
     * 设置最小的重试等待时间，默认 0.5 秒
     *
     * @param minWaitMillis 最小的重试等待时间
     */
    public void setMinWaitMillis(long minWaitMillis) {
        this.minWaitMillis = minWaitMillis;
    }

    /**
     * 获取延时倍数，下一次等待时间与上一次等待时间的比值
     *
     * @return 延时倍数
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * 设置延时倍数，下一次等待时间与上一次等待时间的比值
     *
     * @param multiplier 延时倍数
     */
    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * 重试条件表达式，当该表达式返回true时才有可能进行重试
     *
     * @return 重试条件表达式
     */
    public String getCondition() {
        return condition;
    }

    /**
     * 重试条件表达式，当该表达式返回true时才有可能进行重试
     *
     * @param condition 重试条件表达式
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }


    /**
     * 获取重试函数，指定一个函数让该函数来觉得是否需要重试
     *
     * @return 重试函数
     */
    public String getConditionFunc() {
        return conditionFunc;
    }

    /**
     * 定一个函数让该函数来觉得是否需要重试
     *
     * @param conditionFunc 重试函数，指定一个函数让该函数来觉得是否需要重试
     */
    public void setConditionFunc(String conditionFunc) {
        this.conditionFunc = conditionFunc;
    }

    /**
     * 获取正常状态的状态码，响应的状态码在此范围内时，则不需要重试
     *
     * @return 正常状态的状态码
     */
    public int[] getNormalStatus() {
        return normalStatus;
    }

    /**
     * 指定正常状态的状态码，响应的状态码在此范围内时，则不需要重试
     *
     * @param normalStatus 正常状态的状态码
     */
    public void setNormalStatus(int[] normalStatus) {
        this.normalStatus = normalStatus;
    }

    /**
     * 获取异常状态的状态码，响应的状态码在此范围内时，则需要重试
     *
     * @return 异常状态的状态码
     */
    public int[] getExceptionStatus() {
        return exceptionStatus;
    }

    /**
     * 指定异常状态的状态码，响应的状态码在此范围内时，则需要重试
     *
     * @param exceptionStatus 异常状态的状态码
     */
    public void setExceptionStatus(int[] exceptionStatus) {
        this.exceptionStatus = exceptionStatus;
    }

    /**
     * 获取需要重试的异常，出现这类异常时则需要进行重试
     *
     * @return 需要重试的异常列表
     */
    public Class<? extends Throwable>[] getExceptionClasses() {
        return exceptionClasses;
    }

    /**
     * 指定需要重试的异常，出现这类异常时则需要进行重试
     *
     * @param exceptionClasses 需要重试的异常列表
     */
    public void setExceptionClasses(Class<? extends Throwable>[] exceptionClasses) {
        this.exceptionClasses = exceptionClasses;
    }

    /**
     * 获取排除的异常类型，出现这类异常时不需要进行重试
     *
     * @return 需要排除的异常列表
     */
    public Class<? extends Throwable>[] getExcludeClasses() {
        return excludeClasses;
    }

    /**
     * 指定需要排除的异常类型，出现这类异常时不需要进行重试
     *
     * @param excludeClasses 需要排除的异常列表
     */
    public void setExcludeClasses(Class<? extends Throwable>[] excludeClasses) {
        this.excludeClasses = excludeClasses;
    }

    /**
     * 获取异常校验模型
     *
     * @return 异常校验模型
     */
    public ExceptionModel getExCheckModel() {
        return exCheckModel;
    }

    /**
     * 设置异常校验模型
     *
     * @param exCheckModel 异常校验模型
     */
    public void setExCheckModel(ExceptionModel exCheckModel) {
        this.exCheckModel = exCheckModel;
    }

    /**
     * 获取异常排除模型
     *
     * @return 异常排除模型
     */
    public ExceptionModel getExExcludeModel() {
        return exExcludeModel;
    }

    /**
     * 设置异常排除模型
     *
     * @param exExcludeModel 异常排除模型
     */
    public void setExExcludeModel(ExceptionModel exExcludeModel) {
        this.exExcludeModel = exExcludeModel;
    }

    /**
     * 初始化配置
     */
    public void init() {
        if (!StringUtils.hasText(condition) && normalStatus.length == 0 && exceptionStatus.length == 0) {
            condition = DEFAULT_CONDITION;
        }
    }
}
