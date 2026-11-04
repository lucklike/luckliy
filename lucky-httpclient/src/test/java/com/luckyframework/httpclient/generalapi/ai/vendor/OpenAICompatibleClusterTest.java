package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.ClusterTestApi;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * OpenAI兼容簇的复用验证：同一份会话请求与工具声明，只换base-url、模型标识与鉴权配置，
 * 发出的请求体除model外逐字段一致，目标地址与鉴权头按各自配置生成，
 * 且不为兼容簇厂商新增任何独立分支或报文结构。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class OpenAICompatibleClusterTest {

    /**
     * AI能力的全部源码目录，用来机械核对「没有为兼容簇厂商单独造类」
     */
    private static final String AI_SOURCE_DIR = "src/main/java/com/luckyframework/httpclient/generalapi/ai";

    /**
     * 兼容簇厂商名，出现任何一个都意味着新增了独立分支
     */
    private static final List<String> CLUSTER_VENDOR_NAMES =
            Arrays.asList("deepseek", "tongyi", "qwen", "kimi", "moonshot", "zhipu", "doubao", "ollama");

    @Before
    public void clearCapturedRequests() {
        ClusterTestApi.CAPTURED.clear();
    }

    /**
     * 换base-url即可复用：报文形状完全一致，地址与鉴权按各自配置生成
     */
    @Test
    public void onlyBaseUrlAndModelDifferBetweenClusterVendors() {
        ChatResponse openAI = call("https://api.openai.com/v1/", "sk-openai-key", "gpt-4o-mini");
        ChatResponse deepSeek = call("https://api.deepseek.com/", "sk-deepseek-key", "deepseek-chat");

        assertEquals(2, ClusterTestApi.CAPTURED.size());
        Request openAiRequest = ClusterTestApi.CAPTURED.get(0);
        Request deepSeekRequest = ClusterTestApi.CAPTURED.get(1);

        Map<String, Object> openAiBody = bodyWithoutModel(openAiRequest);
        Map<String, Object> deepSeekBody = bodyWithoutModel(deepSeekRequest);
        assertEquals("除模型标识外两次请求体完全一致", openAiBody, deepSeekBody);
        assertEquals("gpt-4o-mini", body(openAiRequest).get("model"));
        assertEquals("deepseek-chat", body(deepSeekRequest).get("model"));

        assertEquals("/chat/completions", openAiRequest.getPath());
        assertEquals("/chat/completions", deepSeekRequest.getPath());
        assertTrue(openAiRequest.getUrl(), openAiRequest.getUrl().startsWith("https://api.openai.com/v1/"));
        assertTrue(deepSeekRequest.getUrl(), deepSeekRequest.getUrl().startsWith("https://api.deepseek.com/"));
        assertEquals("Bearer sk-openai-key", authorization(openAiRequest));
        assertEquals("Bearer sk-deepseek-key", authorization(deepSeekRequest));

        assertEquals("两家的响应归一化行为一致", toolCallText(openAI), toolCallText(deepSeek));
    }

    /**
     * 兼容簇没有独立分支：源码里不存在以厂商名命名的类型，处理路径只有OpenAI方言一条
     */
    @Test
    public void noDedicatedBranchForClusterVendors() {
        File root = new File(AI_SOURCE_DIR);
        assertTrue("AI源码目录必须存在：" + root.getAbsolutePath(), root.isDirectory());
        List<String> violations = new ArrayList<>();
        collectViolations(root, violations);
        assertTrue("兼容簇厂商不该有独立分支代码：" + violations, violations.isEmpty());
    }

    private static void collectViolations(File dir, List<String> violations) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectViolations(child, violations);
                continue;
            }
            String lowerName = child.getName().toLowerCase();
            for (String vendor : CLUSTER_VENDOR_NAMES) {
                if (lowerName.contains(vendor)) {
                    violations.add(child.getPath() + " 出现了兼容簇厂商名 " + vendor);
                }
            }
        }
    }

    private ChatResponse call(String baseUrl, String apiKey, String model) {
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        AiConfiguration.create()
                .put("cluster.baseUrl", baseUrl)
                .put("cluster.apiKey", apiKey)
                .install(factory);
        ChatRequest request = AiFixtures.weatherRequest();
        request.setModel(model);
        return factory.getProxyObject(ClusterTestApi.class).chat(request);
    }

    private static Map<String, Object> body(Request request) {
        String body = request.getRequestParameter().getBody().getBodyAsString();
        return AiJsonUtils.fromJson(body, new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    private static Map<String, Object> bodyWithoutModel(Request request) {
        Map<String, Object> body = body(request);
        body.remove("model");
        return body;
    }

    private static String authorization(Request request) {
        Object value = request.getHeaderManager().getFirstHeader("Authorization").getValue();
        return String.valueOf(value);
    }

    private static String toolCallText(ChatResponse response) {
        List<ToolCall> toolCalls = response.getToolCalls();
        assertFalse("响应里没有工具调用", toolCalls.isEmpty());
        return toolCalls.get(0).getName() + toolCalls.get(0).getArguments();
    }
}
