package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.support.FakeHttpMcpServer;
import com.luckyframework.httpclient.generalapi.ai.vendor.AiMaps;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * MCP Streamable HTTP传输的离线验证：本地假server扮演一个带会话标识的MCP端点，
 * 覆盖「会话头保持、SSE响应取消息、404重连一次」三条契约（全程不访问外部网络）。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class StreamableHttpTransportTest {

    /**
     * 假MCP端点的路径
     */
    private static final String MCP_PATH = "/mcp";

    private static final String SERVER_NAME = "example-server";

    private static final String CONFIGURED_TOKEN = "Bearer mcp-token-from-config";

    private TestHttpServer server;
    private FakeHttpMcpServer fake;
    private McpClient client;

    @Before
    public void setUp() throws Exception {
        server = new TestHttpServer();
        fake = new FakeHttpMcpServer(server, MCP_PATH);
        McpServerConfig config = McpServerConfig.builder(SERVER_NAME)
                .url(server.baseUrl() + MCP_PATH)
                .headers(Collections.singletonMap("Authorization", CONFIGURED_TOKEN))
                .timeoutMillis(3000L)
                .build();
        client = new McpClient(config.createTransport(), config);
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
        server.close();
    }

    /**
     * 会话标识保持：握手响应颁发的 Mcp-Session-Id 出现在后续每个请求头里，
     * 且请求头满足 application/json + text/event-stream 的媒体类型要求，凭据头来自配置
     */
    @Test
    public void sessionIdIsCarriedByEveryRequestAfterHandshake() {
        client.initialize();
        assertEquals(2, client.listTools().size());

        List<TestHttpServer.RecordedRequest> requests = server.requests(MCP_PATH);
        assertEquals("initialize请求 + initialized通知 + tools/list", 3, requests.size());
        for (TestHttpServer.RecordedRequest sent : requests) {
            assertEquals(StreamableHttpTransport.ACCEPT_HEADER_VALUE, sent.header("Accept"));
            assertTrue("请求体必须是JSON: " + sent.header("Content-Type"),
                    sent.header("Content-Type").contains("application/json"));
            assertEquals("配置里的凭据头要原样带上", CONFIGURED_TOKEN, sent.header("Authorization"));
        }
        // 握手前手里没有会话标识，握手后每一帧都要带上它
        assertNull(requests.get(0).header(StreamableHttpTransport.SESSION_ID_HEADER));
        for (int i = 1; i < requests.size(); i++) {
            assertEquals("s1", requests.get(i).header(StreamableHttpTransport.SESSION_ID_HEADER));
        }
    }

    /**
     * SSE形式的响应：结果藏在 data: 帧里，按帧解析取出与本请求id匹配的消息，
     * 同一条流里别的id的消息不参与本次结果
     */
    @Test
    @SuppressWarnings("unchecked")
    public void resultIsExtractedFromEventStreamFrames() {
        client.initialize();
        fake.setReplyToolsCallWithEventStream(true);

        Map<String, Object> result = client.callTool("get_weather", arguments("北京"));

        assertFalse("取到的不能是那条别的id的消息", result.containsKey("tools"));
        List<Object> content = AiMaps.getList(result, "content");
        assertEquals(1, content.size());
        assertEquals("北京今天晴，12 度", ((Map<String, Object>) content.get(0)).get("text"));
    }

    /**
     * 会话过期重连一次：404 → 重新握手（拿到新会话标识）→ 重发该请求 → 成功；
     * 该请求共到服务端2次，说明确实只重连一次
     */
    @Test
    public void expiredSessionTriggersOneReHandshakeAndResend() {
        client.initialize();
        fake.expireSession();

        assertEquals(2, client.listTools().size());

        assertEquals("tools/list共发了两次：一次撞404、一次重连后成功", 2, fake.countMethod("tools/list"));
        assertEquals("initialize共发了两次：原始握手 + 一次重连", 2, fake.countMethod("initialize"));
        assertEquals("只回过一次404", 1, fake.countStatus(404));
        assertEquals("两轮握手各两帧 + 两帧tools/list", 6, server.hit(MCP_PATH));
    }

    /**
     * 会话始终无效时必须尽快报错，不得无限重握手
     */
    @Test
    public void reHandshakeIsBoundedWhenSessionIsNeverAccepted() {
        fake.setRejectEverything(true);

        try {
            client.initialize();
            fail("会话始终无效时必须报错");
        } catch (McpException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("404"));
        }
        assertEquals("最多握手两次（原始 + 一次重连），绝不无限重试", 2, server.hit(MCP_PATH));
    }

    /**
     * 通道关闭后不再收发
     */
    @Test
    public void closedTransportRefusesToSend() {
        client.initialize();
        client.close();

        assertFalse(client.getChannel().isOpen());
        try {
            client.listTools();
            fail("通道已关闭时应当报错");
        } catch (McpException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("closed"));
        }
    }

    private static Map<String, Object> arguments(String city) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("city", city);
        return arguments;
    }
}
