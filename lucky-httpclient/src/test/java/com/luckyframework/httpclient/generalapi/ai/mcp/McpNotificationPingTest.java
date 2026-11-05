package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.InMemoryMcpTransport;
import com.luckyframework.httpclient.generalapi.ai.support.McpTestSupport;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
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

/**
 * 6.7 通知容忍与ping：server主动发来的通知（含未知method、{@code notifications/tools/list_changed}）
 * 与配不上对的响应一律忽略，不得报错也不得打断请求-响应配对；
 * ping收到 {@code result: {}} 即成功，无响应时在超时后得到失败结果。
 * 帧序列全部来自 {@code src/test/resources/ai/mcp} 下的录制文件与手工构造帧，全程离线。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class McpNotificationPingTest {

    private InMemoryMcpTransport transport;
    private McpClient client;
    private ExecutorService executor;

    @Before
    public void setUp() {
        transport = new InMemoryMcpTransport();
        McpServerConfig config = McpServerConfig.builder("example-server").timeoutMillis(2000).build();
        client = new McpClient(transport, config);
        executor = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        client.close();
    }

    /**
     * 结果前先收到通知：未知method的通知与另一id的响应都不报错、不打断配对，
     * tools/call仍拿到与自身id匹配的结果，客户端也不会去回复任何通知
     */
    @Test
    public void notificationBeforeResultDoesNotBreakPairing() throws Exception {
        handshake();

        Future<Map<String, Object>> call = executor.submit(
                () -> client.callTool("get_weather", McpTestSupport.param("city", "北京")));
        waitFrames(3);
        int callFrameIndex = transport.sentCount() - 1;
        assertEquals("tools/call", transport.sentFrame(callFrameIndex).get("method"));

        // 结果之前先后来三条「不该影响配对」的帧：已知通知、未知method通知、别的id的响应
        transport.deliver("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/message\","
                + "\"params\":{\"level\":\"info\",\"data\":\"server is warming up\"}}");
        transport.deliver("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/progress\",\"params\":{\"token\":1}}");
        transport.deliver("{\"jsonrpc\":\"2.0\",\"id\":9527,\"result\":{\"tools\":[]}}");

        deliverRecorded("ai/mcp/tools-call-response.json");

        Map<String, Object> result = call.get(2, TimeUnit.SECONDS);
        assertEquals("北京今天晴，12 度", McpTestSupport.firstText(result));
        assertEquals("通知与迟到响应都不该触发任何外发帧", 3, transport.sentCount());
    }

    /**
     * {@code notifications/tools/list_changed} 本轮只容忍：不报错、也不自动重新拉取清单
     */
    @Test
    public void listChangedNotificationIsToleratedWithoutAutoRefresh() throws Exception {
        handshake();

        ToolRegistry registry = new ToolRegistry();
        McpToolBridge bridge = new McpToolBridge(client);
        Future<List<ToolSpec>> registering = executor.submit(() -> bridge.registerAll(registry));
        waitFrames(3);
        deliverRecorded("ai/mcp/tools-list-response.json");
        assertEquals(2, registering.get(2, TimeUnit.SECONDS).size());
        int framesAfterRegister = transport.sentCount();

        transport.deliver("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/tools/list_changed\"}");

        assertEquals("收到清单变更通知时不得自动重新发tools/list", framesAfterRegister, transport.sentCount());
        assertEquals("注册表内容保持不变", 2, registry.size());
        // 会话仍然可用：后续请求照旧能拿到配对结果
        Future<Map<String, Object>> call = executor.submit(
                () -> client.callTool("get_weather", McpTestSupport.param("city", "北京")));
        waitFrames(framesAfterRegister + 1);
        deliverRecorded("ai/mcp/tools-call-response.json");
        assertEquals("北京今天晴，12 度", McpTestSupport.firstText(call.get(2, TimeUnit.SECONDS)));
    }

    /**
     * ping收到空result即视为成功
     */
    @Test
    public void pingSucceedsOnEmptyResult() throws Exception {
        handshake();

        Future<Boolean> ping = executor.submit(() -> client.ping());
        waitFrames(3);
        assertEquals("ping", transport.sentFrame(2).get("method"));

        int id = idOf(transport.sentFrame(2));
        transport.deliver("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{}}");

        assertTrue(ping.get(2, TimeUnit.SECONDS));
    }

    /**
     * 无响应的server：ping在超时后给出失败结果而不是把异常抛给调用方
     */
    @Test(timeout = 10_000L)
    public void pingFailsAfterTimeoutWhenServerIsSilent() throws Exception {
        handshake();

        Future<Boolean> ping = executor.submit(() -> client.ping());
        waitFrames(3);
        // 一条响应都不回，让配置里的超时生效
        assertFalse("超时无响应时ping应给出失败结果", ping.get(6, TimeUnit.SECONDS));
    }

    /**
     * 完成握手：initialize请求 → 响应 → initialized通知
     */
    private void handshake() throws Exception {
        Future<Map<String, Object>> capabilities = executor.submit(() -> client.initialize());
        waitFrames(1);
        deliverRecorded("ai/mcp/initialize-response.json");
        capabilities.get(2, TimeUnit.SECONDS);
        waitFrames(2);
        assertTrue(client.isInitialized());
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
