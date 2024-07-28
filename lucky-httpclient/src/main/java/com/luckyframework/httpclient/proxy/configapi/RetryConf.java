package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 重试配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/28 21:36
 */
public class RetryConf {


    /**
     * 是否启用重试
     */
    private Boolean enable;

    /**
     * 任务名称
     */
    @TargetField("task-name")
    private String taskName;

    /**
     * 最大重试次数
     */
    @TargetField("max-count")
    private Integer maxCount;

    /**
     * 等待时间
     */
    @TargetField("wait-millis")
    private Long waitMillis;

    /**
     * 等待倍数
     */
    @TargetField("multiplier")
    private Double multiplier;

    /**
     * 最大等待时间
     */
    @TargetField("max-wait-millis")
    private Long maxWaitMillis;

    /**
     * 最小等待时间
     */
    @TargetField("min-wait-millis")
    private Long minWaitMillis;

    /**
     * 重试异常类型
     */
    private Set<Class<? extends Throwable>> exception = new HashSet<>();

    /**
     * 不用重试的异常类型
     */
    private Set<Class<? extends Throwable>> exclude = new HashSet<>();

    /**
     * 重试的状态码
     */
    @TargetField("exception-status")
    private Set<Integer> exceptionStatus = new HashSet<>();

    /**
     * 正常的状态码
     */
    @TargetField("normal-status")
    private Set<Integer> normalStatus = new HashSet<>();

    /**
     * 重试SpEL表达式
     */
    private String expression;


    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public Long getWaitMillis() {
        return waitMillis;
    }

    public void setWaitMillis(Long waitMillis) {
        this.waitMillis = waitMillis;
    }

    public Double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }

    public Long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(Long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public Long getMinWaitMillis() {
        return minWaitMillis;
    }

    public void setMinWaitMillis(Long minWaitMillis) {
        this.minWaitMillis = minWaitMillis;
    }

    public Set<Class<? extends Throwable>> getException() {
        return exception;
    }

    public void setException(Set<Class<? extends Throwable>> exception) {
        this.exception = exception;
    }

    public Set<Class<? extends Throwable>> getExclude() {
        return exclude;
    }

    public void setExclude(Set<Class<? extends Throwable>> exclude) {
        this.exclude = exclude;
    }

    public Set<Integer> getExceptionStatus() {
        return exceptionStatus;
    }

    public void setExceptionStatus(Set<Integer> exceptionStatus) {
        this.exceptionStatus = exceptionStatus;
    }

    public Set<Integer> getNormalStatus() {
        return normalStatus;
    }

    public void setNormalStatus(Set<Integer> normalStatus) {
        this.normalStatus = normalStatus;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
