package com.luckyframework.retry;

/**
 * 每次进行重试前都等待一个固定的时间
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 13:46
 */
public class FixedWaitBeforeRetry implements WaitBeforeRetry {

    /**
     * 重试等待时间（单位：毫秒）
     */
    private final long waitTimeMillis;

    public FixedWaitBeforeRetry(long waitTimeMillis) {
        this.waitTimeMillis = waitTimeMillis;
    }

    @Override
    public long getWaitTimeMillis(Integer retryNumber) {
        return waitTimeMillis;
    }
}
