package com.luckyframework.httpclient.generalapi.ai.tool;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;

/**
 * 工具执行结果，只有成功与失败两态。
 * 失败结果的内容是可让模型区分失败的错误说明文本，由回环引擎作为工具结果回填给模型自纠，
 * 而不是抛给业务方。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ToolResult {

    private static final String NO_EXECUTOR = "无执行实现（has no executable implementation），无法执行该调用。";

    private final boolean success;
    private final String content;

    private ToolResult(boolean success, String content) {
        this.success = success;
        this.content = content;
    }

    /**
     * 按序列化规则生成成功结果：字符串与基本类型取其文本，对象/集合/Map序列化为JSON文本；
     * 执行实现已自行判定成败（返回ToolResult，如MCP远端 {@code isError}）时按其原样作为本次结果
     */
    public static ToolResult success(Object returnValue) {
        if (returnValue instanceof ToolResult) {
            return (ToolResult) returnValue;
        }
        return new ToolResult(true, toContent(returnValue));
    }

    /**
     * 生成失败结果，内容为可诊断的错误说明文本
     */
    public static ToolResult failure(String text) {
        return new ToolResult(false, text);
    }

    /**
     * 生成「工具无执行实现」的失败结果
     */
    public static ToolResult noExecutor(String toolName) {
        return failure("工具 [" + toolName + "] " + NO_EXECUTOR);
    }

    /**
     * 把工具返回值序列化为结果内容
     */
    public static String toContent(Object returnValue) {
        if (returnValue == null) {
            return "null";
        }
        if (returnValue instanceof CharSequence || returnValue instanceof Number || returnValue instanceof Boolean
                || returnValue instanceof Character || returnValue instanceof Enum) {
            return String.valueOf(returnValue);
        }
        return AiJsonUtils.toJson(returnValue);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "ToolResult{success=" + success + ", content=" + content + "}";
    }
}
