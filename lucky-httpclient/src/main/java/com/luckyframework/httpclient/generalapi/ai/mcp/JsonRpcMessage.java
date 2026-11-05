package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON-RPC 2.0 的一条消息，四类形态（请求/响应/通知/错误响应）共用同一个结构：
 * 有 {@code id} 且有 {@code method} 的是请求，有 {@code id} 且无 {@code method} 的是响应，
 * 无 {@code id} 的是通知，响应里带 {@code error} 节点的是错误响应。
 * <p>
 * 本能力只用得到 initialize / tools/list / tools/call / ping / notifications.* 这几个方法，
 * 其余方法一律按「未知通知容忍、未知响应报错」处理，因此消息面保持扁平（不做继承体系）。
 *
 * @param <T> 参数或结果的类型
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，由 {@link McpClient} 与 {@link JsonRpcChannel} 使用
 */
public class JsonRpcMessage<T> {

    /**
     * 协议版本，JSON-RPC 2.0固定取值
     */
    public static final String JSONRPC_VERSION = "2.0";

    private final Integer id;
    private final String method;
    private final T payload;
    private final JsonRpcError error;

    private JsonRpcMessage(Integer id, String method, T payload, JsonRpcError error) {
        this.id = id;
        this.method = method;
        this.payload = payload;
        this.error = error;
    }

    /**
     * 请求：带id与method，payload为参数对象
     */
    public static <T> JsonRpcMessage<T> request(int id, String method, T params) {
        return new JsonRpcMessage<>(id, method, params, null);
    }

    /**
     * 通知：不带id，payload为参数对象
     */
    public static <T> JsonRpcMessage<T> notification(String method, T params) {
        return new JsonRpcMessage<>(null, method, params, null);
    }

    /**
     * 成功响应：payload为result节点
     */
    public static <T> JsonRpcMessage<T> response(int id, T result) {
        return new JsonRpcMessage<>(id, null, result, null);
    }

    /**
     * 错误响应
     */
    public static JsonRpcMessage<Void> error(int id, int code, String message) {
        return new JsonRpcMessage<>(id, null, null, new JsonRpcError(code, message));
    }

    /**
     * 从一帧已解析的JSON构造消息，供各Transport的读循环使用
     *
     * @param frame SSE/stdio读到的单个JSON对象
     * @return 消息实例，帧里既没有method也没有result/error时返回null（表达这不是JSON-RPC消息）
     */
    public static JsonRpcMessage<Object> fromFrame(Map<String, Object> frame) {
        if (frame == null) {
            return null;
        }
        Integer id = asInteger(frame.get("id"));
        String method = frame.get("method") == null ? null : String.valueOf(frame.get("method"));
        Object result = frame.get("result");
        Object error = frame.get("error");
        if (method == null && result == null && error == null) {
            return null;
        }
        JsonRpcError jsonRpcError = null;
        if (error instanceof Map) {
            Map<String, Object> errorNode = asMap(error);
            Integer code = asInteger(errorNode.get("code"));
            String message = errorNode.get("message") == null ? null : String.valueOf(errorNode.get("message"));
            jsonRpcError = new JsonRpcError(code == null ? 0 : code, message);
        }
        Object payload = method != null ? frame.get("params") : result;
        return new JsonRpcMessage<>(id, method, payload, jsonRpcError);
    }

    /**
     * 是否为请求（有id也有method，需要对方给出同id的响应）
     */
    public boolean isRequest() {
        return id != null && method != null;
    }

    /**
     * 是否为通知（无id，不参与请求-响应配对）
     */
    public boolean isNotification() {
        return id == null && method != null;
    }

    /**
     * 是否为响应（有id无method）
     */
    public boolean isResponse() {
        return id != null && method == null;
    }

    /**
     * 是否为错误响应
     */
    public boolean isError() {
        return error != null;
    }

    public Integer getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    /**
     * 请求与通知的参数对象，或响应的result对象
     */
    public T getPayload() {
        return payload;
    }

    /**
     * 响应里的result节点，非Map时返回空Map（表达「无结果对象」）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResultMap() {
        return payload instanceof Map ? (Map<String, Object>) payload : Collections.<String, Object>emptyMap();
    }

    public JsonRpcError getError() {
        return error;
    }

    /**
     * 序列化为待发送的JSON文本（一帧一个完整对象，不带缩进）
     */
    public String toJson() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("jsonrpc", JSONRPC_VERSION);
        if (id != null) {
            node.put("id", id);
        }
        if (method != null) {
            node.put("method", method);
        }
        if (error != null) {
            Map<String, Object> errorNode = new LinkedHashMap<>();
            errorNode.put("code", error.getCode());
            errorNode.put("message", error.getMessage());
            node.put("error", errorNode);
        } else if (payload != null) {
            node.put(isRequest() || isNotification() ? "params" : "result", payload);
        } else if (isResponse()) {
            node.put("result", Collections.emptyMap());
        }
        return AiJsonUtils.toJson(node);
    }

    @Override
    public String toString() {
        return "JsonRpcMessage{id=" + id + ", method=" + method + ", payload=" + payload + ", error=" + error + "}";
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    /**
     * JSON-RPC错误节点（错误码与错误说明）
     */
    public static class JsonRpcError {

        private final int code;
        private final String message;

        public JsonRpcError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "JsonRpcError{code=" + code + ", message=" + message + "}";
        }
    }
}
