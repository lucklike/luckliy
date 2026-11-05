package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.support.InMemoryMcpTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
 * 6.1 JSON-RPC 2.0消息面与请求-响应配对：响应乱序到达时各自命中自己的请求，
 * 无id的通知既不参与配对也不会被当成某请求的响应，无响应时按超时报错。
 * 全部由内存假通道驱动，不触碰任何网络与子进程。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class JsonRpcPairingTest {

    private InMemoryMcpTransport transport;
    private JsonRpcChannel channel;
    private ExecutorService executor;

    @Before
    public void setUp() {
        transport = new InMemoryMcpTransport();
        channel = new JsonRpcChannel(transport).timeout(2000);
        executor = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        channel.close();
    }

    /**
     * 请求帧自带jsonrpc版本、id与方法名，通知帧不带id
     */
    @Test
    public void outgoingFramesFollowJsonRpcShape() throws Exception {
        startRequest("tools/list");
        waitFrames(1);
        Map<String, Object> request = transport.sentFrame(0);
        assertEquals("2.0", request.get("jsonrpc"));
        assertEquals("tools/list", request.get("method"));
        assertTrue("请求必须带id用于配对", request.containsKey("id"));
        assertTrue("无参请求也要给出空的params", request.containsKey("params"));

        channel.notify("notifications/initialized", null);
        Map<String, Object> notification = transport.sentFrame(1);
        assertEquals("notifications/initialized", notification.get("method"));
        assertFalse("通知不得带id", notification.containsKey("id"));
    }

    /**
     * server乱序返回两个响应时，两个调用分别拿到与自身id对应的结果
     */
    @Test
    public void outOfOrderResponsesAreMatchedById() throws Exception {
        Future<Map<String, Object>> first = executor.submit(() -> channel.request("initialize", null));
        waitFrames(1);
        Future<Map<String, Object>> second = executor.submit(() -> channel.request("tools/list", null));
        waitFrames(2);

        int idOfFirst = idOf(transport.sentFrame(0));
        int idOfSecond = idOf(transport.sentFrame(1));

        // 故意先回第二个请求的响应
        transport.deliver("{\"jsonrpc\":\"2.0\",\"id\":" + idOfSecond
                + ",\"result\":{\"marker\":\"tools-list\"}}");
        assertEquals("tools-list", result(second.get(1, TimeUnit.SECONDS)).get("marker"));

        transport.deliver("{\"jsonrpc\":\"2.0\",\"id\":" + idOfFirst
                + ",\"result\":{\"protocolVersion\":\"2025-06-18\"}}");
        assertEquals("2025-06-18", result(first.get(1, TimeUnit.SECONDS)).get("protocolVersion"));
    }

    /**
     * 无id的通知不参与配对，也不会被当成某请求的响应：请求仍在等待，直到真正的响应到来
     */
    @Test
    public void notificationIsNotTreatedAsAResponse() throws Exception {
        Future<Map<String, Object>> call = executor.submit(() -> channel.request("tools/call", null));
        waitFrames(1);
        int id = idOf(transport.sentFrame(0));

        transport.deliver("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/message\","
                + "\"params\":{\"level\":\"info\",\"data\":\"progress 50%\"}}");
        try {
            call.get(300, TimeUnit.MILLISECONDS);
            fail("通知不得唤醒等待响应的请求");
        } catch (java.util.concurrent.TimeoutException expected) {
            // 仍在等待，符合契约
        }

        transport.deliver("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"content\":[]}}");
        assertTrue(JsonRpcMessage.fromFrame(call.get(1, TimeUnit.SECONDS)).isResponse());
    }

    /**
     * 超时时间内没有任何匹配响应时报超时错误，且不留悬挂的等待槽
     */
    @Test
    public void requestWithoutResponseTimesOut() throws Exception {
        Future<Map<String, Object>> call = executor.submit(() -> channel.request("ping", null));
        waitFrames(1);

        try {
            call.get(3, TimeUnit.SECONDS);
            fail("无响应时必须走超时异常而不是永久等待");
        } catch (java.util.concurrent.ExecutionException e) {
            assertTrue(String.valueOf(e.getCause()), e.getCause() instanceof McpException);
            assertTrue(e.getCause().getMessage(), e.getCause().getMessage().contains("timed out"));
        }
    }

    /**
     * 连接断开时未完成的请求立即得到「连接已断开」错误，不再等待
     */
    @Test
    public void disconnectFailsPendingRequests() throws Exception {
        Future<Map<String, Object>> call = executor.submit(() -> channel.request("tools/list", null));
        waitFrames(1);

        transport.disconnect(new IllegalStateException("process exited"));

        try {
            call.get(2, TimeUnit.SECONDS);
            fail("断开后必须立即报错");
        } catch (java.util.concurrent.ExecutionException e) {
            assertTrue(String.valueOf(e.getCause()), e.getCause() instanceof McpException);
            assertTrue(e.getCause().getMessage(), e.getCause().getMessage().contains("连接已断开"));
        }
    }

    /**
     * 错误响应按响应交付给调用方（由上层转成失败结果），不在配对层报错
     */
    @Test
    public void errorResponseIsDeliveredToItsCaller() throws Exception {
        Future<Map<String, Object>> call = executor.submit(() -> channel.request("tools/call", null));
        waitFrames(1);
        int id = idOf(transport.sentFrame(0));

        transport.deliver("{\"jsonrpc\":\"2.0\",\"id\":" + id
                + ",\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}");

        JsonRpcMessage<Object> message = JsonRpcMessage.fromFrame(call.get(1, TimeUnit.SECONDS));
        assertTrue(message.isError());
        assertEquals(-32601, message.getError().getCode());
        assertEquals("Method not found", message.getError().getMessage());
    }

    /**
     * 通道关闭后不得再发出请求
     */
    @Test
    public void closedChannelRefusesRequests() {
        channel.close();
        try {
            channel.request("ping", null);
            fail("通道已关闭时应当报错");
        } catch (McpException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("连接已断开"));
        }
    }

    private Future<Map<String, Object>> startRequest(String method) {
        return executor.submit(() -> channel.request(method, null));
    }

    /**
     * 等待通道累计发出expected帧（发起请求本身是同步写入，这里只是防御性等待）
     */
    private void waitFrames(int expected) throws InterruptedException {
        for (int i = 0; i < 100 && transport.sentCount() < expected; i++) {
            Thread.sleep(10);
        }
        assertTrue("应当已发出第" + expected + "帧", transport.sentCount() >= expected);
    }

    private static int idOf(Map<String, Object> frame) {
        return ((Number) frame.get("id")).intValue();
    }

    /**
     * 从响应帧里取result对象
     */
    private static Map<String, Object> result(Map<String, Object> frame) {
        return JsonRpcMessage.fromFrame(frame).getResultMap();
    }
}
