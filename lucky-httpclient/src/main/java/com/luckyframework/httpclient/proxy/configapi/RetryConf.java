package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.retry.ExceptionModel;

import java.util.HashSet;
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
    private String taskName;

    /**
     * 最大重试次数
     */
    private Integer maxCount;

    /**
     * 等待时间
     */
    private Long waitMillis;

    /**
     * 等待倍数
     */
    private Double multiplier;

    /**
     * 最大等待时间
     */
    private Long maxWaitMillis;

    /**
     * 最小等待时间
     */
    private Long minWaitMillis;

    /**
     * 重试异常类型
     */
    private Set<Class<? extends Throwable>> exception = new HashSet<>();

    /**
     * 异常检验模型
     */
    private ExceptionModel exCheckModel = ExceptionModel.CHECK_ROOT_CAUSE;

    /**
     * 异常排除模型
     */
    private ExceptionModel exExcludeModel = ExceptionModel.CHECK_ALL_STACK;

    /**
     * 不用重试的异常类型
     */
    private Set<Class<? extends Throwable>> exclude = new HashSet<>();

    /**
     * 重试的状态码
     */
    private Set<Integer> exceptionStatus = new HashSet<>();

    /**
     * 正常的状态码
     */
    private Set<Integer> normalStatus = new HashSet<>();

    /**
     * 重试SpEL表达式
     */
    private String expression;

    /**
     * 指定重试SpEL函数名
     */
    private String funcName;


    /**
     *
     * @return
     */
    public Boolean getEnable() {
        return enable;
    }

    /**
     *
     * @param enable
     */
    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    /**
     *
     * @return
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     *
     * @param taskName
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     *
     * @return
     */
    public Integer getMaxCount() {
        return maxCount;
    }

    /**
     *
     * @param maxCount
     */
    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    /**
     *
     * @return
     */
    public Long getWaitMillis() {
        return waitMillis;
    }

    /**
     *
     * @param waitMillis
     */
    public void setWaitMillis(Long waitMillis) {
        this.waitMillis = waitMillis;
    }

    /**
     *
     * @return
     */
    public Double getMultiplier() {
        return multiplier;
    }

    /**
     *
     * @param multiplier
     */
    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }

    /**
     *
     * @return
     */
    public Long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    /**
     *
     * @param maxWaitMillis
     */
    public void setMaxWaitMillis(Long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    /**
     *
     * @return
     */
    public Long getMinWaitMillis() {
        return minWaitMillis;
    }

    /**
     *
     * @param minWaitMillis
     */
    public void setMinWaitMillis(Long minWaitMillis) {
        this.minWaitMillis = minWaitMillis;
    }

    /**
     *
     * @return
     */
    public Set<Class<? extends Throwable>> getException() {
        return exception;
    }

    /**
     *
     * @param exception
     */
    public void setException(Set<Class<? extends Throwable>> exception) {
        this.exception = exception;
    }

    /**
     *
     * @return
     */
    public Set<Class<? extends Throwable>> getExclude() {
        return exclude;
    }

    /**
     *
     * @param exclude
     */
    public void setExclude(Set<Class<? extends Throwable>> exclude) {
        this.exclude = exclude;
    }

    /**
     *
     * @return
     */
    public Set<Integer> getExceptionStatus() {
        return exceptionStatus;
    }

    /**
     *
     * @param exceptionStatus
     */
    public void setExceptionStatus(Set<Integer> exceptionStatus) {
        this.exceptionStatus = exceptionStatus;
    }

    /**
     *
     * @return
     */
    public Set<Integer> getNormalStatus() {
        return normalStatus;
    }

    /**
     *
     * @param normalStatus
     */
    public void setNormalStatus(Set<Integer> normalStatus) {
        this.normalStatus = normalStatus;
    }

    /**
     *
     * @return
     */
    public String getExpression() {
        return expression;
    }

    /**
     *
     * @param expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     *
     * @return
     */
    public String getFuncName() {
        return funcName;
    }

    /**
     *
     * @param funcName
     */
    public void setFuncName(String funcName) {
        this.funcName = funcName;
    }

    /**
     *
     * @return
     */
    public ExceptionModel getExCheckModel() {
        return exCheckModel;
    }

    /**
     *
     * @param exCheckModel
     */
    public void setExCheckModel(ExceptionModel exCheckModel) {
        this.exCheckModel = exCheckModel;
    }

    /**
     *
     * @return
     */
    public ExceptionModel getExExcludeModel() {
        return exExcludeModel;
    }

    /**
     *
     * @param exExcludeModel
     */
    public void setExExcludeModel(ExceptionModel exExcludeModel) {
        this.exExcludeModel = exExcludeModel;
    }
}
