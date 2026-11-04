package com.luckyframework.httpclient.generalapi.ai.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 归一化后的会话响应：候选回答列表与用量信息。三家厂商的原始报文一律先解析为Map，
 * 再由各家方言显式取字段构建本对象。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    private List<Choice> choices = new ArrayList<>();
    private Usage usage;

    public ChatResponse() {
    }

    public ChatResponse(List<Choice> choices, Usage usage) {
        if (choices != null) {
            this.choices = new ArrayList<>(choices);
        }
        this.usage = usage;
    }

    /**
     * 取第一个候选回答，没有任何候选时返回null
     */
    @JsonIgnore
    public Choice getFirstChoice() {
        return choices.isEmpty() ? null : choices.get(0);
    }

    /**
     * 取第一个候选回答的文本内容
     */
    @JsonIgnore
    public String getContent() {
        Choice choice = getFirstChoice();
        return choice == null || choice.getMessage() == null ? null : choice.getMessage().getContentText();
    }

    /**
     * 取第一个候选回答携带的工具调用列表，无工具调用时返回空列表
     */
    @JsonIgnore
    public List<ToolCall> getToolCalls() {
        Choice choice = getFirstChoice();
        if (choice == null || choice.getMessage() == null || choice.getMessage().getToolCalls() == null) {
            return Collections.emptyList();
        }
        return choice.getMessage().getToolCalls();
    }

    @JsonIgnore
    public boolean hasToolCalls() {
        return !getToolCalls().isEmpty();
    }

    /**
     * 取第一个候选回答的结束原因
     */
    @JsonIgnore
    public FinishReason getFinishReason() {
        Choice choice = getFirstChoice();
        return choice == null ? null : choice.getFinishReason();
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices == null ? new ArrayList<>() : choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    @Override
    public String toString() {
        return "ChatResponse{choices=" + choices + ", usage=" + usage + "}";
    }

    /**
     * 一条候选回答
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Choice {

        private Integer index;
        private ChatMessage message;
        private FinishReason finishReason;

        public Choice() {
        }

        public Choice(Integer index, ChatMessage message, FinishReason finishReason) {
            this.index = index;
            this.message = message;
            this.finishReason = finishReason;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public ChatMessage getMessage() {
            return message;
        }

        public void setMessage(ChatMessage message) {
            this.message = message;
        }

        @JsonProperty("finish_reason")
        public FinishReason getFinishReason() {
            return finishReason;
        }

        @JsonProperty("finish_reason")
        public void setFinishReason(FinishReason finishReason) {
            this.finishReason = finishReason;
        }

        @Override
        public String toString() {
            return "Choice{index=" + index + ", message=" + message + ", finishReason=" + finishReason + "}";
        }
    }
}
