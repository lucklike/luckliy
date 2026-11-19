package com.luckyframework.debug;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 协议 JSON 与异常文本的统一入口。
 *
 * ObjectMapper 是线程安全的，整个 launcher 共用一个实例；
 * 序列化时省略 null 字段，插件侧按「字段不存在」处理。
 */
final class DebugJson {

    static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    static ObjectNode object() {
        return MAPPER.createObjectNode();
    }

    static ArrayNode array() {
        return MAPPER.createArrayNode();
    }

    static String text(JsonNode node, String field, String fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    static String messageOf(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getName()
                : message;
    }

    static String stackOf(Throwable error) {
        if (error == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private DebugJson() {
    }
}
