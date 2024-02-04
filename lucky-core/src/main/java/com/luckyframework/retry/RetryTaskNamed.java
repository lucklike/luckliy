package com.luckyframework.retry;

/**
 * 重试任务名称
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 20:48
 */
public interface RetryTaskNamed {

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    String getTaskName();
}
