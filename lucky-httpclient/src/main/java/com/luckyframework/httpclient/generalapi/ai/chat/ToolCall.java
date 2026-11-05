package com.luckyframework.httpclient.generalapi.ai.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 模型发起的一次工具调用请求，承载调用标识、工具名与参数JSON文本。
 * 报文按OpenAI chat/completions的形状表达（参数挂在 {@code function} 键下），
 * Anthropic/Gemini的结构翻译由方言层完成。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCall {

    /**
     * 调用类型，本次只使用function
     */
    public static final String TYPE_FUNCTION = "function";

    private String id;
    private String type = TYPE_FUNCTION;
    private Payload function;

    public ToolCall() {
    }

    public ToolCall(String id, String name, String arguments) {
        this.id = id;
        this.type = TYPE_FUNCTION;
        this.function = new Payload(name, arguments);
    }

    /**
     * 构造一次工具调用
     */
    public static ToolCall of(String id, String name, String arguments) {
        return new ToolCall(id, name, arguments);
    }

    @JsonIgnore
    public String getName() {
        return function == null ? null : function.getName();
    }

    @JsonIgnore
    public String getArguments() {
        return function == null ? null : function.getArguments();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Payload getFunction() {
        return function;
    }

    public void setFunction(Payload function) {
        this.function = function;
    }

    @Override
    public String toString() {
        return "ToolCall{id=" + id + ", name=" + getName() + ", arguments=" + getArguments() + "}";
    }

    /**
     * 工具名与参数文本的载体，对应报文里的 function 层
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Payload {

        private String name;
        private String arguments;

        public Payload() {
        }

        public Payload(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
}
