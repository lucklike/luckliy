package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;

import java.util.List;

/**
 * 工具调用回环异常：回环无法按契约收敛（轮次超限、方法参数缺少会话请求对象）时抛出。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ToolCallException extends LuckyRuntimeException {

    public ToolCallException(String message) {
        super(message);
    }

    public ToolCallException(String message, Throwable ex) {
        super(message, ex);
    }

    /**
     * 达到轮次上限而模型仍在要求调用工具时的异常，消息里带上已完成轮次与最后一轮待执行的工具调用
     *
     * @param maxRounds    轮次上限
     * @param lastRoundCalls 最后一轮模型要求调用的工具列表
     * @return 回环异常
     */
    public static ToolCallException roundLimit(int maxRounds, List<ToolCall> lastRoundCalls) {
        StringBuilder calls = new StringBuilder();
        for (ToolCall call : lastRoundCalls) {
            if (calls.length() > 0) {
                calls.append(", ");
            }
            calls.append(call.getName()).append("(").append(call.getArguments()).append(")");
        }
        return new ToolCallException("The tool-call loop reached the round limit " + maxRounds
                + " (轮次上限 " + maxRounds + "), but the model is still requesting tool calls: [" + calls + "]");
    }
}
