package com.luckyframework.httpclient.generalapi.ai.vendor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 厂商能力标志集合。上层行为只依据标志集合决策，不允许按厂商名字符串比对。
 * 查询未知标志名时一律返回「不支持」而非抛异常。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public final class Capabilities {

    /**
     * 支持工具调用
     */
    public static final String TOOL_CALL = "toolCall";

    /**
     * 支持流式下的工具调用
     */
    public static final String STREAM_TOOL_CALL = "streamToolCall";

    /**
     * 支持并行工具调用
     */
    public static final String PARALLEL_TOOL_CALLS = "parallelToolCalls";

    /**
     * 支持JSON Schema严格模式
     */
    public static final String STRICT_SCHEMA = "strictSchema";

    private static final List<String> KNOWN_FLAGS =
            Collections.unmodifiableList(Arrays.asList(TOOL_CALL, STREAM_TOOL_CALL, PARALLEL_TOOL_CALLS, STRICT_SCHEMA));

    private final Set<String> enabledFlags;

    private Capabilities(Set<String> enabledFlags) {
        this.enabledFlags = Collections.unmodifiableSet(enabledFlags);
    }

    /**
     * 按启用的标志名构造能力集合
     */
    public static Capabilities of(String... flags) {
        Set<String> enabled = new LinkedHashSet<>();
        if (flags != null) {
            enabled.addAll(Arrays.asList(flags));
        }
        return new Capabilities(enabled);
    }

    /**
     * 本次交付的三家的共同取值：支持工具调用与流式工具调用，不支持并行工具调用与严格模式
     */
    public static Capabilities standard() {
        return of(TOOL_CALL, STREAM_TOOL_CALL);
    }

    /**
     * 按标志名查询，未知标志名返回不支持
     */
    public boolean supports(String flag) {
        return flag != null && enabledFlags.contains(flag);
    }

    public boolean isToolCallSupported() {
        return supports(TOOL_CALL);
    }

    public boolean isStreamToolCallSupported() {
        return supports(STREAM_TOOL_CALL);
    }

    public boolean isParallelToolCallsSupported() {
        return supports(PARALLEL_TOOL_CALLS);
    }

    public boolean isStrictSchemaSupported() {
        return supports(STRICT_SCHEMA);
    }

    /**
     * 本框架已定义的全部标志名
     */
    public static List<String> knownFlags() {
        return KNOWN_FLAGS;
    }

    @Override
    public String toString() {
        return "Capabilities" + enabledFlags;
    }
}
