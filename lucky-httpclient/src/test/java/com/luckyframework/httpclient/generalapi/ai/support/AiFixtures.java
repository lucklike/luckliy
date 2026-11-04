package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatMessage;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI测试公共夹具：录制报文样本的读取入口与「查询天气」这一组贯穿全部切片的数据。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public final class AiFixtures {

    private AiFixtures() {
    }

    /**
     * 读取 src/test/resources 下的录制样本
     */
    public static String resource(String path) {
        try (Reader reader = new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load the recorded sample: " + path, e);
        }
    }

    /**
     * 读取录制样本并解析为Map
     */
    public static Map<String, Object> jsonResource(String path) {
        return AiJsonUtils.fromJson(resource(path), new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    /**
     * 读取录制的SSE文本并按行解析为「单帧Map」序列，供流式拼接器直接喂入；
     * {@code data: [DONE]} 这类非JSON帧会被跳过
     */
    public static List<Map<String, Object>> sseFrames(String path) {
        List<Map<String, Object>> frames = new ArrayList<>();
        for (String line : resource(path).split("\\r?\\n")) {
            String text = line.trim();
            if (!text.startsWith("data:")) {
                continue;
            }
            text = text.substring("data:".length()).trim();
            if (text.isEmpty() || !text.startsWith("{")) {
                continue;
            }
            frames.add(AiJsonUtils.fromJson(text, new SerializationTypeToken<Map<String, Object>>() {
            }));
        }
        return frames;
    }

    /**
     * 与 spec 样例一致的 get_weather 工具声明
     */
    public static ToolSpec weatherToolSpec() {
        return new ToolSpec("get_weather", "查询城市天气", weatherSchema());
    }

    public static ConfigurationMap weatherSchema() {
        Map<String, Object> city = new LinkedHashMap<>();
        city.put("type", "string");
        city.put("description", "城市名");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("city", city);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", Collections.singletonList("city"));
        return new ConfigurationMap(schema);
    }

    /**
     * 一轮完整往返之后的会话请求：四条消息 + 一条工具声明，golden-file 与回环断言都以它为基准
     */
    public static ChatRequest weatherRequest() {
        ChatRequest request = new ChatRequest("deepseek-chat");
        request.addMessage(ChatMessage.system("你是一个助手"));
        request.addMessage(ChatMessage.user("北京今天天气怎么样？"));
        request.addAssistantToolCalls(Collections.singletonList(
                ToolCall.of("call_001", "get_weather", "{\"city\":\"北京\"}")));
        request.addToolResult("call_001", "{\"temp\":12,\"weather\":\"晴\"}");
        request.setToolSpecs(Collections.singletonList(weatherToolSpec()));
        request.setToolChoice("auto");
        request.setTemperature(0.7);
        request.setMaxTokens(1024);
        request.setStream(false);
        return request;
    }

    /**
     * 只有两条对话消息的最小请求
     */
    public static ChatRequest minimalRequest() {
        ChatRequest request = new ChatRequest("deepseek-chat");
        request.addMessage(ChatMessage.system("你是一个助手"));
        request.addMessage(ChatMessage.user("北京今天天气怎么样？"));
        return request;
    }
}
