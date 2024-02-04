package com.luckyframework.retry;

/**
 * 支持时间补偿的重试等待时间策略
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 13:53
 */
public class BackoffWaitBeforeRetry implements WaitBeforeRetry {

    /**
     * 原始等待时长
     */
    private final long originalWaitTimeMillis;

    /**
     * 时间补充倍数
     * <pre>
     * 1.当multiplier > 1 时
     *     最终等待时间 = 倍数 *（重试次数-1)  * 最初等待时间
     * 2.当0 < multiplier < 1 时
     *     最终等待时间 = 倍数^(重试次数-1) * 最初等待时间
     * </pre>
     */
    private final double multiplier;

    /**
     * 最大等待时长
     */
    private final long maxWaitMillis;

    /**
     * 最小等待时长
     */
    private final long minWaitMillis;

    /**
     * 全惨构造函数
     *
     * @param originalWaitTimeMillis 原始等待时长
     * @param multiplier             时间补充倍数
     * @param maxWaitMillis          最大等待时长
     * @param minWaitMillis          最小等待时长
     */
    public BackoffWaitBeforeRetry(long originalWaitTimeMillis, double multiplier, long maxWaitMillis, long minWaitMillis) {
        this.originalWaitTimeMillis = originalWaitTimeMillis;
        this.multiplier = multiplier;
        this.maxWaitMillis = maxWaitMillis;
        this.minWaitMillis = minWaitMillis;
    }

    /**
     * 无惨构造函数
     * <pre>
     *     originalWaitTimeMillis = 1000L
     *     multiplier = 2D
     *     maxWaitMillis = 30000L
     *     minWaitMillis = 100L
     * </pre>
     */
    public BackoffWaitBeforeRetry() {
        this(1000L, 2D, 30000L, 100L);
    }

    @Override
    public long getWaitTimeMillis(Integer retryNumber) {
        // 等待时间小于0时直接返回
        if (originalWaitTimeMillis <= 0) {
            return -1L;
        }

        // 倍数小于0时抛异常
        if (multiplier < 0) {
            throw new IllegalArgumentException("'multiplier' cannot be less than 0.");
        }

        // 根据倍数、执行次数、初始等待时长计算最终等待时长
        // 倍数大于1时：等待时间 = 倍数 *（重试次数 - 1)  * 最初等待时间
        // 倍数小于1时：等待时间 = 倍数^(重试次数 -1 ) * 最初等待时间
        long finalWaitMillis = multiplier == 0 || retryNumber == 1
                ? originalWaitTimeMillis
                : multiplier > 1
                ? (long) (originalWaitTimeMillis * multiplier * (retryNumber - 1))
                : (long) (originalWaitTimeMillis * Math.pow(multiplier, (retryNumber - 1)));

        // 最终等待时间不可小于最小等待时间且不可大于最大时间
        if (maxWaitMillis > 0) {
            finalWaitMillis = Math.min(maxWaitMillis, finalWaitMillis);
        }
        return Math.max(finalWaitMillis, minWaitMillis);
    }
}
