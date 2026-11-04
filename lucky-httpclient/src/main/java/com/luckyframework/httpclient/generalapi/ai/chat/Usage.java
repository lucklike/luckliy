package com.luckyframework.httpclient.generalapi.ai.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * token用量，统一为输入/输出/总数三项。各家的不同命名由方言层归一到本对象，
 * 厂商未给出的项保持为null（表达「未提供」而非报错）。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Usage {

    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;

    public Usage() {
    }

    public Usage(Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
    }

    /**
     * 输入与输出都已知而总数缺失时，以两者之和补齐总数
     */
    public void fillTotalIfAbsent() {
        if (this.totalTokens == null && this.inputTokens != null && this.outputTokens != null) {
            this.totalTokens = this.inputTokens + this.outputTokens;
        }
    }

    @JsonProperty("prompt_tokens")
    public Integer getInputTokens() {
        return inputTokens;
    }

    @JsonProperty("prompt_tokens")
    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    @JsonProperty("completion_tokens")
    public Integer getOutputTokens() {
        return outputTokens;
    }

    @JsonProperty("completion_tokens")
    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    @JsonProperty("total_tokens")
    public Integer getTotalTokens() {
        return totalTokens;
    }

    @JsonProperty("total_tokens")
    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    @Override
    public String toString() {
        return "Usage{input=" + inputTokens + ", output=" + outputTokens + ", total=" + totalTokens + "}";
    }
}
