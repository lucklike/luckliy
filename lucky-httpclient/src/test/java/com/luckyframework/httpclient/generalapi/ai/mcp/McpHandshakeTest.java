package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.InMemoryMcpTransport;
import com.luckyframework.httpclient.generalapi.ai.support.McpTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 6.3 MCP客户端初始化握手：消息顺序必须是 initialize 请求 → initialize 响应 →
 * notifications/initialized 通知（通知无id），握手完成前不得发出任何工具请求；
 * server返回的协议版本与请求不一致时采用server的版本；server未声明tools能力时不注册远端工具。
 * 帧序列取自 {@code src/test/resources/ai/mcp} 下的录制文件，全程离线。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class McpHandshakeTest {

    private static final String TOOLS_LIST_METHOD = "tools/list";

    private InMemoryMcpTransport transport;
    private McpClient client;
    private ExecutorService executor;

    @Before
    public void setUp() {
        transport = new InMemoryMcpTransport();
        McpServerConfig config = McpServerConfig.builder("example-server")
                .timeoutMillis(2000)
                .build();
        client = new McpClient(transport, config);
        executor = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        client.close();
    }

    /**
     * 握手成功的消息顺序，以及「握手完成后才允许 tools/list」
     */
    @Test
    public void handshakeGoesInitializeRequestThenInitializedNotification() throws Exception {
        Future<Map<String, Object>> capabilities = executor.submit(() -> client.initialize());
        waitFrames(1);

        Map<String, Object> request = transport.sentFrame(0);
        assertEquals("2.0", request.get("jsonrpc"));
        assertEquals("initialize", request.get("method"));
        Map<String, Object> params = McpTestSupport.paramsOf(request);
        assertEquals(McpClient.REQUESTED_PROTOCOL_VERSION, params.get("protocolVersion"));
        assertTrue("必须上报客户端能力集合（本实现为空集合）", params.containsKey("capabilities"));
        Map<String, Object> clientInfo = McpTestSupport.asMap(params.get("clientInfo"));
        assertEquals(McpClient.CLIENT_NAME, clientInfo.get("name"));
        assertFalse("客户端信息里的版本号不得为空", String.valueOf(clientInfo.get("version")).isEmpty());

        deliverRecorded("ai/mcp/initialize-response.json");
        Map<String, Object> serverCapabilities = capabilities.get(2, TimeUnit.SECONDS);
        assertTrue(client.isInitialized());
        assertEquals("example-server", client.getServerName());
        assertEquals("1.0.0", client.getServerVersion());
        assertTrue(serverCapabilities.containsKey("tools"));

        waitFrames(2);
        Map<String, Object> notification = transport.sentFrame(1);
        assertEquals("notifications/initialized", notification.get("method"));
        assertFalse("通知不得带id", notification.containsKey("id"));
        assertFalse("通知不得带result", notification.containsKey("result"));

        // 握手之后 tools/list 才被允许发出
        Future<List<Map<String, Object>>> tools = executor.submit(() -> client.listTools());
        waitFrames(3);
        assertEquals(TOOLS_LIST_METHOD, transport.sentFrame(2).get("method"));
        deliverRecorded("ai/mcp/tools-list-response.json");
        assertEquals(2, tools.get(2, TimeUnit.SECONDS).size());
    }

    /**
     * 未握手即请求工具清单：抛「会话未初始化」，且一条请求帧都不发出
     */
    @Test
    public void toolsListIsRefusedBeforeHandshake() {
        try {
            client.listTools();
            fail("握手前必须拒绝工具请求");
        } catch (McpException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not initialized"));
            assertTrue(expected.getMessage(), expected.getMessage().contains(TOOLS_LIST_METHOD));
        }
        assertEquals("拒绝时不得发出任何请求", 0, transport.sentCount());

        try {
            client.callTool("get_weather", McpTestSupport.param("city", "北京"));
            fail("握手前必须拒绝工具调用");
        } catch (McpException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not initialized"));
        }
        assertEquals(0, transport.sentCount());

        try {
            client.ping();
            fail("握手前必须拒绝ping");
        } catch (McpException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not initialized"));
        }
        assertEquals(0, transport.sentCount());
    }

    /**
     * server返回的协议版本与请求不一致时，采用server返回的版本继续会话
     */
    @Test
    public void serverProtocolVersionWinsWhenDifferent() throws Exception {
        Future<Map<String, Object>> capabilities = executor.submit(() -> client.initialize());
        waitFrames(1);
        deliverRecorded("ai/mcp/initialize-response-version-mismatch.json");
        capabilities.get(2, TimeUnit.SECONDS);

        assertEquals("2024-11-05", client.getProtocolVersion());
        // 后续请求仍按协商结果继续，不会因为版本不一致而中断会话
        assertTrue(client.isInitialized());
        Future<List<Map<String, Object>>> tools = executor.submit(() -> client.listTools());
        waitFrames(3);
        deliverRecorded("ai/mcp/tools-list-response.json");
        assertEquals(2, tools.get(2, TimeUnit.SECONDS).size());
    }

    /**
     * server未声明tools能力时不拉取、不注册任何远端工具，只给出可诊断说明
     */
    @Test
    public void serverWithoutToolsCapabilityRegistersNothing() throws Exception {
        Future<Map<String, Object>> capabilities = executor.submit(() -> client.initialize());
        waitFrames(1);
        deliverRecorded("ai/mcp/initialize-response-no-tools.json");
        capabilities.get(2, TimeUnit.SECONDS);

        assertFalse(client.serverHasTools());
        assertFalse(client.getServerCapabilities().containsKey("tools"));
        // 握手两帧之后再无其他请求：不主动拉取清单，也就没有任何远端工具进入注册表
        waitFrames(2);
        assertEquals(2, transport.sentCount());
    }

    /**
     * server以JSON-RPC error拒绝握手时，握手直接报错且不会发出initialized通知
     */
    @Test
    public void rejectedInitializeAbortsHandshake() throws Exception {
        Future<Map<String, Object>> capabilities = executor.submit(() -> client.initialize());
        waitFrames(1);
        int id = idOf(transport.sentFrame(0));
        transport.deliver("{\"jsonrpc\":\"2.0\",\"id\":" + id
                + ",\"error\":{\"code\":-32600,\"message\":\"Invalid Request\"}}");
        try {
            capabilities.get(2, TimeUnit.SECONDS);
            fail("握手被拒绝时必须报错");
        } catch (java.util.concurrent.ExecutionException expected) {
            assertTrue(String.valueOf(expected.getCause()), expected.getCause() instanceof McpException);
            assertTrue(expected.getCause().getMessage(), expected.getCause().getMessage().contains("Invalid Request"));
        }
        assertFalse(client.isInitialized());
        assertEquals("被拒绝后不得发出initialized通知", 1, transport.sentCount());
    }

    /**
     * 取出录制帧并把id改成当前待应答请求的id后交付，模拟server的响应
     */
    private void deliverRecorded(String path) {
        Map<String, Object> frame = AiFixtures.jsonResource(path);
        frame.put("id", idOf(transport.sentFrame(transport.sentCount() - 1)));
        transport.deliver(AiJsonUtils.toJson(frame));
    }

    /**
     * 等待通道累计发出expected帧（发起请求本身是同步写入，这里只是防御性等待）
     */
    private void waitFrames(int expected) throws InterruptedException {
        for (int i = 0; i < 200 && transport.sentCount() < expected; i++) {
            Thread.sleep(10);
        }
        assertTrue("应当已发出第" + expected + "帧", transport.sentCount() >= expected);
    }

    private static int idOf(Map<String, Object> frame) {
        return ((Number) frame.get("id")).intValue();
    }
}
