package com.luckyframework.exception;

/**
 * 雪花算法时钟回拨异常
 * <p>
 * 当检测到机器时钟回拨幅度超过配置的容忍上限（默认 1000ms）时抛出。
 * 此时拒绝生成 ID，让故障快速曝光，而不是静默生成可能重复的 ID。
 */
public class SnowflakeClockBackwardException extends LuckyRuntimeException {

    public SnowflakeClockBackwardException(String message) {
        super(message);
    }

    public SnowflakeClockBackwardException(String message, Throwable ex) {
        super(message, ex);
    }

    public SnowflakeClockBackwardException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }
}
