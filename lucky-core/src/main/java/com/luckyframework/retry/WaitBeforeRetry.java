package com.luckyframework.retry;

/**
 * 在重试之前等待一定的时间
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 13:19
 */
public interface WaitBeforeRetry extends RunBeforeRetry<Object> {
    @Override
    default void beforeRetry(TaskResult<Object> taskResult) {
        long waitTime = getWaitTimeMillis(taskResult.getRetryNum());
        if (waitTime <= 0) {
            return;
        }
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            throw new RetryWaiteException("Retry wait exception.", e);
        }
    }

    /**
     * 根据重试次数获取等待时间（单位：毫秒）
     *
     * @param retryNumber 重试次数
     * @return 重试前等待的时间（单位：毫秒）
     */
    long getWaitTimeMillis(Integer retryNumber);
}
