package com.luckyframework.httpclient.generalapi.ai.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 会话消息角色，与OpenAI chat/completions的角色取值同名。
 * Anthropic/Gemini的角色名映射由方言层完成，本枚举不承载厂商字段名。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public enum ChatRole {

    /**
     * 系统指令
     */
    SYSTEM("system"),

    /**
     * 用户输入
     */
    USER("user"),

    /**
     * 模型回答
     */
    ASSISTANT("assistant"),

    /**
     * 工具执行结果
     */
    TOOL("tool");

    private final String value;

    ChatRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * 按报文取值解析角色，未知取值返回null（由方言层自行处理）
     */
    @JsonCreator
    public static ChatRole of(String value) {
        for (ChatRole role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
