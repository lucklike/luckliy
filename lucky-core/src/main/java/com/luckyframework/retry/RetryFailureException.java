package com.luckyframework.retry;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 重试失败异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/22 13:24
 */
@SuppressWarnings("all")
public class RetryFailureException extends LuckyRuntimeException {

    private final TaskResult taskResult;

    public RetryFailureException(TaskResult taskResult, String message) {
        super(taskResult.hasException() ? taskResult.getThrowable() : new TaskRunSuccessButResultAbnormalException(message));
        this.taskResult = taskResult;
    }

    public TaskResult getTaskResult() {
        return taskResult;
    }

    @Override
    public String getMessage() {
        if (taskResult.hasName()) {
            return StringUtils.format("The {} task named '{}' failed after [{}] retries, and the nesting exception was {}.",
                    taskResult.getType(), taskResult.getName(), taskResult.getRetryNum() - 1, super.getMessage());
        }
        return "";
    }

}
