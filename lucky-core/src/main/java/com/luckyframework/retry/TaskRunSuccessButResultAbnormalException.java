package com.luckyframework.retry;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 任务运行成功但是运行结果不正常产生的异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 15:25
 */
public class TaskRunSuccessButResultAbnormalException extends LuckyRuntimeException {
    public TaskRunSuccessButResultAbnormalException(String message) {
        super(message);
    }
}
