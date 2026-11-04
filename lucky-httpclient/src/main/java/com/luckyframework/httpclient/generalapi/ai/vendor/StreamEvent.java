package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;

/**
 * 流式拼接产生的事件，只有两类：文本增量与完整工具调用。
 * 协议层非法（帧结构错误、未见结束信号、参数JSON不完整）一律以
 * {@link StreamProtocolException} 抛出，不作为事件表达。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，不属于对外承诺的公开接口
 */
public final class StreamEvent {

    private final String textDelta;
    private final ToolCall toolCall;

    private StreamEvent(String textDelta, ToolCall toolCall) {
        this.textDelta = textDelta;
        this.toolCall = toolCall;
    }

    /**
     * 文本增量事件
     */
    public static StreamEvent textDelta(String text) {
        return new StreamEvent(text, null);
    }

    /**
     * 完整工具调用事件，只在厂商给出「该调用参数已完整」的信号后产生
     */
    public static StreamEvent toolCall(ToolCall toolCall) {
        return new StreamEvent(null, toolCall);
    }

    public boolean isTextDelta() {
        return toolCall == null;
    }

    public boolean isToolCall() {
        return toolCall != null;
    }

    public String getTextDelta() {
        return textDelta;
    }

    public ToolCall getToolCall() {
        return toolCall;
    }

    @Override
    public String toString() {
        return isToolCall() ? "StreamEvent{toolCall=" + toolCall + "}" : "StreamEvent{textDelta=" + textDelta + "}";
    }
}
