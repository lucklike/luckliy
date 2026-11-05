package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.mcp.JsonRpcMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP测试的取值小工具：JSON-RPC帧都是「已解析的Map」，断言时按显式取字段的方式读取。
 *
 * @author fukang
 * @version 3.0.3
 */
public final class McpTestSupport {

    private McpTestSupport() {
    }

    /**
     * 响应帧里的result对象
     */
    public static Map<String, Object> result(Map<String, Object> frame) {
        return JsonRpcMessage.fromFrame(frame).getResultMap();
    }

    /**
     * 请求帧里的params对象
     */
    public static Map<String, Object> paramsOf(Map<String, Object> frame) {
        return asMap(frame.get("params"));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object node) {
        return node instanceof Map ? (Map<String, Object>) node : Collections.<String, Object>emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object node) {
        return node instanceof List ? new ArrayList<>((List<Object>) node) : Collections.emptyList();
    }

    /**
     * 构造一个单项参数对象
     */
    public static Map<String, Object> param(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    /**
     * 远端工具清单里的工具名，按声明顺序
     */
    public static List<String> toolNames(Map<String, Object> toolsListResult) {
        List<String> names = new ArrayList<>();
        for (Object tool : asList(toolsListResult.get("tools"))) {
            names.add((String) asMap(tool).get("name"));
        }
        return names;
    }

    /**
     * {@code result.content} 里第一个text分片的文本
     */
    public static String firstText(Map<String, Object> toolsCallResult) {
        List<Object> content = asList(toolsCallResult.get("content"));
        return content.isEmpty() ? null : (String) asMap(content.get(0)).get("text");
    }

    /**
     * {@code result.content} 里全部分片的类型名，按顺序
     */
    public static List<String> contentTypes(Map<String, Object> toolsCallResult) {
        List<String> types = new ArrayList<>();
        for (Object part : asList(toolsCallResult.get("content"))) {
            types.add((String) asMap(part).get("type"));
        }
        return types;
    }
}
