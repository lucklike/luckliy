package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.chat.FinishReason;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.CHAT_COMPLETIONS_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * 内置OpenAI兼容会话接口 {@link OpenAIChatApi} 的端到端测试。
 * <pre>
 *     验证点：
 *     1.请求方法为POST、路径为/chat/completions，目标地址由配置项提供；
 *     2.Authorization头的值取自配置文件键（不是代码里的常量）；
 *     3.请求体就是统一模型的OpenAI形状，且不含回环内部控制字段；
 *     4.响应经转换器归一化为 {@link ChatResponse}。
 *
 *     测试全程打本地假服务端，不访问任何外部地址。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class OpenAIApiTest {

    /**
     * 连接配置的来源文件，注解里的 #{ai.openai.*} 表达式即取本文件的键
     */
    private static final String CONFIG_LOCATION = "classpath:ai/openai/api-config.yml";

    /**
     * 配置文件中声明的密钥，用于断言头取值确实来自文件
     */
    private static final String CONFIGURED_API_KEY = "sk-config-file-key";

    private TestHttpServer server;
    private AiConfiguration configuration;
    private OpenAIChatApi api;

    @Before
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.registerJson(CHAT_COMPLETIONS_PATH);
        server.queueResponse(CHAT_COMPLETIONS_PATH, AiFixtures.resource("ai/openai/response-text.json"));

        configuration = AiConfiguration.load(CONFIG_LOCATION)
                .put("openai.baseUrl", server.baseUrl() + "/");
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        configuration.install(factory);
        api = factory.getProxyObject(OpenAIChatApi.class);
    }

    @After
    public void tearDown() {
        server.close();
    }

    /**
     * 方法、路径、鉴权头与返回类型四项契约
     */
    @Test
    public void requestUsesConfiguredAddressAndCredential() {
        ChatResponse response = api.chat(AiFixtures.minimalRequest());

        assertEquals("应当只发出一次请求", 1, server.hit(CHAT_COMPLETIONS_PATH));
        TestHttpServer.RecordedRequest sent = server.lastRequest(CHAT_COMPLETIONS_PATH);
        assertNotNull("假服务端没有收到请求", sent);
        assertEquals("POST", sent.method());
        assertEquals(CHAT_COMPLETIONS_PATH, sent.path());
        assertEquals("Bearer " + CONFIGURED_API_KEY, sent.header("Authorization"));

        Map<String, Object> body = AiJsonUtils.fromJson(sent.body(), new SerializationTypeToken<Map<String, Object>>() {
        });
        assertEquals("deepseek-chat", body.get("model"));
        assertEquals(2, ((List<?>) body.get("messages")).size());
        assertFalse("回环内部控制字段不得泄漏进请求体", body.containsKey("loopRound"));

        assertEquals("北京今天晴，12 度。", response.getContent());
        assertEquals(FinishReason.STOP, response.getFinishReason());
        assertEquals(Integer.valueOf(39), response.getUsage().getTotalTokens());
    }

    /**
     * 换一个base-url配置即可复用到兼容簇厂商，报文结构不变
     */
    @Test
    public void onlyBaseUrlDiffersBetweenCompatibleVendors() {
        String configuredBaseUrl = configuration.getString("openai.baseUrl");
        assertEquals(server.baseUrl() + "/", configuredBaseUrl);
        assertEquals(CONFIGURED_API_KEY, configuration.getString("openai.apiKey"));
    }
}
