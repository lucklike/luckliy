package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.support.FakeMcpServer;
import com.luckyframework.httpclient.generalapi.ai.support.McpTestSupport;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 6.2 stdio传输：按配置的命令拉起子进程、以换行分隔的JSON帧收发、读循环在后台线程，
 * 超时与进程退出都必须报错并回收子进程（不残留进程）。
 * <p>
 * 假server由本测试用 {@code java -cp <测试类路径>} 拉起，全程不访问网络。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class StdioTransportTest {

    private StdioTransport transport;
    private JsonRpcChannel channel;
    private ExecutorService executor;

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (channel != null) {
            channel.close();
        }
        if (transport != null) {
            transport.close();
        }
    }

    /**
     * 一帧一个JSON对象：initialize请求能拿到与自身id对应的响应，
     * 且server混在stdout里的非JSON日志行被忽略
     */
    @Test
    public void framesAreExchangedOneJsonObjectPerLine() {
        start("--noisy");
        Map<String, Object> response = channel.request("initialize", McpTestSupport.param("protocolVersion", "2025-06-18"));

        JsonRpcMessage<Object> message = JsonRpcMessage.fromFrame(response);
        assertNotNull(message);
        assertTrue("响应的id必须与请求对应", message.isResponse());
        assertEquals("2025-06-18", message.getResultMap().get("protocolVersion"));
        assertEquals("fake-server", McpTestSupport.asMap(message.getResultMap().get("serverInfo")).get("name"));
    }

    /**
     * tools/list 与 tools/call 走同一条通道，帧形状与spec样例一致
     */
    @Test
    public void toolListAndCallGoThroughTheSamePipe() {
        start();
        channel.request("initialize", null);

        List<String> names = McpTestSupport.toolNames(McpTestSupport.result(channel.request("tools/list", null)));
        assertEquals(java.util.Arrays.asList("get_weather", "echo"), names);

        Map<String, Object> callResult = McpTestSupport.result(
                channel.request("tools/call", McpTestSupport.param("name", "get_weather")));
        assertEquals("北京今天晴，12 度", McpTestSupport.firstText(callResult));
    }

    /**
     * server在超时时间内不回包时得到超时异常，且子进程被回收
     */
    @Test
    public void timeoutReclaimsTheChildProcess() {
        start();
        channel.timeout(600).closeOnTimeout(true);
        Process process = transport.getProcess();
        assertTrue("子进程应当已经拉起", process.isAlive());

        try {
            channel.request(FakeMcpServer.HANG_METHOD, null);
            fail("无响应时必须走超时异常");
        } catch (McpException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("timed out"));
        }

        assertTrue("超时后子进程必须被回收", waitUntilDead(process));
        assertFalse(transport.isOpen());
    }

    /**
     * 子进程意外退出时，未完成的请求立即得到「连接已断开」错误而不再等待
     */
    @Test
    public void processExitFailsPendingRequests() throws Exception {
        start();
        executor = Executors.newSingleThreadExecutor();
        Future<Map<String, Object>> call = executor.submit(
                () -> channel.request(FakeMcpServer.EXIT_METHOD, null));

        try {
            call.get(5, TimeUnit.SECONDS);
            fail("进程退出后必须报错");
        } catch (java.util.concurrent.ExecutionException e) {
            assertTrue(String.valueOf(e.getCause()), e.getCause() instanceof McpException);
            assertTrue(e.getCause().getMessage(), e.getCause().getMessage().contains("连接已断开"));
        }
        assertFalse(transport.getProcess().isAlive());
    }

    /**
     * close()幂等：重复关闭不抛异常，进程不残留
     */
    @Test
    public void closeIsIdempotent() {
        start();
        Process process = transport.getProcess();
        transport.close();
        transport.close();
        assertTrue(waitUntilDead(process));
    }

    /**
     * 未配置命令时直接报错，不会悄悄拉起一个空命令
     */
    @Test
    public void missingCommandIsRejected() {
        try {
            new StdioTransport(new java.util.ArrayList<String>());
            fail("命令为空时必须报错");
        } catch (McpException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not configured"));
        }
    }

    private void start(String... extraArgs) {
        transport = new StdioTransport(FakeMcpServer.command(extraArgs));
        channel = new JsonRpcChannel(transport).timeout(5000);
    }

    /**
     * 等待子进程真正退出，最多2秒
     */
    private static boolean waitUntilDead(Process process) {
        for (int i = 0; i < 100 && process.isAlive(); i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return !process.isAlive();
    }
}
