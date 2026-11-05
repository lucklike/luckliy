package com.luckyframework.httpclient.generalapi.ai.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 统一的结束原因。各家厂商的原始取值（如 end_turn、STOP、max_tokens）由方言层归一后落入本枚举，
 * 模型层与引擎层只认识这里定义的取值。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public enum FinishReason {

    /**
     * 模型要求调用工具
     */
    TOOL_CALLS("tool_calls"),

    /**
     * 正常文本结束
     */
    STOP("stop"),

    /**
     * 达到长度上限
     */
    LENGTH("length"),

    /**
     * 被内容风控拦截
     */
    CONTENT_FILTER("content_filter"),

    /**
     * 无法识别的结束原因
     */
    UNKNOWN("unknown");

    private final String value;

    FinishReason(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * 按统一报文取值解析，null与未识别取值一律归为UNKNOWN，保证厂商新增取值不破坏解析
     */
    @JsonCreator
    public static FinishReason of(String value) {
        for (FinishReason reason : values()) {
            if (reason.value.equalsIgnoreCase(value)) {
                return reason;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return value;
    }
}
