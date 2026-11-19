package com.luckyframework.debug;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 返回值编码。
 *
 * Response 只取状态、头和正文；其他值优先 JSON，
 * 序列化失败时降级为文本；二进制统一走 Base64。
 */
final class DebugResultEncoder {

    private static final int MAX_BYTES = 1024 * 1024;
    private static final int MAX_TEXT = 256 * 1024;

    ObjectNode encode(Method method, Object result) {
        ObjectNode payload = DebugJson.object();
        Class<?> returnType = method.getReturnType();
        // 用泛型签名：异步接口能显示出 Future<String> 这样的真实返回类型。
        payload.put("returnType", method.getGenericReturnType().getTypeName());

        if (returnType == void.class) {
            payload.put("resultKind", "void");
            return payload;
        }
        if (result == null) {
            payload.put("resultKind", "null");
            return payload;
        }
        if (isResponse(result)) {
            payload.put("resultKind", "response");
            payload.set("response", encodeResponse(result));
            return payload;
        }
        if (result instanceof byte[]) {
            payload.put("resultKind", "bytes");
            payload.put("base64", Base64.getEncoder().encodeToString((byte[]) result));
            return payload;
        }
        if (result instanceof InputStream) {
            return encodeStream((InputStream) result, payload);
        }
        if (result instanceof CharSequence) {
            payload.put("resultKind", "text");
            payload.put("result", truncate(result.toString()));
            return payload;
        }
        try {
            String json = DebugJson.MAPPER.writeValueAsString(result);
            payload.put("resultKind", "json");
            payload.put("result", truncate(json));
        } catch (Exception e) {
            payload.put("resultKind", "text");
            payload.put("result", truncate(String.valueOf(result)));
        }
        return payload;
    }

    private boolean isResponse(Object result) {
        try {
            Class<?> type = Class.forName(
                    "com.luckyframework.httpclient.core.meta.Response",
                    false,
                    result.getClass().getClassLoader());
            return type.isInstance(result);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private ObjectNode encodeResponse(Object response) {
        ObjectNode node = DebugJson.object();
        Object status = call(response, "getStatus");
        if (status instanceof Number) {
            node.put("status", ((Number) status).intValue());
        }

        Object manager = call(response, "getHeaderManager");
        if (manager != null) {
            node.set("headers", headersOf(manager));
        }

        Object body = call(response, "getResult");
        if (body instanceof byte[]) {
            node.put("bodyBase64", Base64.getEncoder().encodeToString((byte[]) body));
            if (((byte[]) body).length > MAX_BYTES) {
                node.put("truncated", true);
            }
        }

        Object length = call(response, "getContentLength");
        if (length instanceof Number) {
            node.put("contentLength", ((Number) length).longValue());
        }
        Object contentType = call(response, "getContentType");
        if (contentType != null) {
            node.put("contentType", String.valueOf(contentType));
        }
        return node;
    }

    private ObjectNode headersOf(Object manager) {
        ObjectNode headers = DebugJson.object();
        Object map = call(manager, "getHeaderMap");
        if (!(map instanceof Map)) {
            return headers;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
            ArrayNode values = DebugJson.array();
            Object list = entry.getValue();
            if (list instanceof List) {
                for (Object header : (List<?>) list) {
                    Object value = call(header, "getValue");
                    if (value != null) {
                        values.add(String.valueOf(value));
                    }
                }
            }
            headers.set(String.valueOf(entry.getKey()), values);
        }
        return headers;
    }

    private ObjectNode encodeStream(InputStream stream, ObjectNode payload) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while (total < MAX_BYTES && (read = stream.read(chunk)) > 0) {
                int size = Math.min(read, MAX_BYTES - total);
                buffer.write(chunk, 0, size);
                total += size;
            }
            payload.put("resultKind", "bytes");
            payload.put("base64", Base64.getEncoder().encodeToString(buffer.toByteArray()));
            payload.put("truncated", total >= MAX_BYTES);
        } catch (Exception e) {
            payload.put("resultKind", "text");
            payload.put("result", "读取 InputStream 失败：" + DebugJson.messageOf(e));
        }
        return payload;
    }

    private Object call(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable e) {
            return null;
        }
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_TEXT) {
            return text;
        }
        return text.substring(0, MAX_TEXT);
    }
}
