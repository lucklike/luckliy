package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.FakeHttpMcpServer;
import com.luckyframework.httpclient.generalapi.ai.support.FakeMcpServer;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolExecutor;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolResult;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 6.8 离线验证：本能力的全部行为（握手、清单拉取、调用桥接、两条传输的分支）
 * 都由「本地子进程」与「录制帧」完成断言，测试过程不发起任何外部网络请求。
 * <pre>
 *     stdio——命令是当前JDK的java可执行文件 + 本地测试类路径拉起的假server；
 *     HTTP——目标地址是127.0.0.1上的本地假端点，每个请求的Host头都逐条核对；
 *     帧内容——全部取自 src/test/resources/ai/mcp，逐个文件核对不含外网地址。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class McpOfflineTest {

    private static final String MCP_PATH = "/mcp";

    /**
     * 本能力用到的录制帧文件，逐个核对「没有外网地址」
     */
    private static final List<String> RECORDED_FRAMES = java.util.Arrays.asList(
            "ai/mcp/initialize-response.json",
            "ai/mcp/tools-list-response.json",
            "ai/mcp/tools-call-response.json",
            "ai/mcp/tools-call-response-is-error.json",
            "ai/mcp/tools-call-response-two-texts.json",
            "ai/mcp/tools-call-response-mixed-parts.json",
            "ai/mcp/tools-list-page1.json",
            "ai/mcp/tools-list-page2.json",
            "ai/mcp/tools-list-missing-schema.json");

    private McpClient stdioClient;
    private TestHttpServer server;
    private McpClient httpClient;

    @After
    public void tearDown() {
        if (stdioClient != null) {
            stdioClient.close();
        }
        if (httpClient != null) {
            httpClient.close();
        }
        if (server != null) {
            server.close();
        }
    }

    /**
     * stdio：本地子进程即可完成握手、清单拉取、调用桥接与ping，配置里根本没有地址
     */
    @Test
    public void stdioSessionRunsOnALocalChildProcess() throws Throwable {
        McpServerConfig config = McpServerConfig.builder("weather")
                .command(FakeMcpServer.command())
                .timeoutMillis(5000L)
                .build();
        assertNull("stdio配置不带地址", config.getUrl());
        assertTrue("命令由当前JDK的java与本地类路径组成", config.getCommand().get(0).endsWith("java"));

        stdioClient = new McpClient(config.createTransport(), config);
        stdioClient.initialize();
        assertTrue(stdioClient.serverHasTools());
        assertEquals(2, stdioClient.listTools().size());

        ToolRegistry registry = new ToolRegistry();
        new McpToolBridge(stdioClient).registerAll(registry);
        Optional<ToolExecutor> executor = registry.findExecutor("get_weather");
        assertTrue(executor.isPresent());
        ToolResult result = ToolResult.success(executor.get().execute(arguments("北京")));
        assertTrue(result.getContent(), result.isSuccess());
        assertEquals("北京今天晴，12 度", result.getContent());
        assertTrue("ping在本地子进程上同样可用", stdioClient.ping());
    }

    /**
     * HTTP：每一次请求的目标都是回环地址，没有外网域名
     */
    @Test
    public void httpSessionOnlyTalksToTheLoopbackAddress() throws Exception {
        server = new TestHttpServer();
        FakeHttpMcpServer fake = new FakeHttpMcpServer(server, MCP_PATH);
        String url = server.baseUrl() + MCP_PATH;
        McpServerConfig config = McpServerConfig.builder("remote")
                .url(url)
                .headers(Collections.singletonMap("Authorization", "Bearer offline-token"))
                .timeoutMillis(3000L)
                .build();

        httpClient = new McpClient(config.createTransport(), config);
        httpClient.initialize();
        assertEquals(2, httpClient.listTools().size());
        assertEquals("北京今天晴，12 度",
                McpToolBridge.toToolResult("get_weather", httpClient.callTool("get_weather", arguments("北京")))
                        .getContent());
        fake.setReplyToolsCallWithEventStream(true);
        assertEquals("SSE形式的响应也走同一条回环链路", "北京今天晴，12 度",
                McpToolBridge.toToolResult("get_weather", httpClient.callTool("get_weather", arguments("北京")))
                        .getContent());
        assertTrue("ping在回环链路上可用", httpClient.ping());

        List<TestHttpServer.RecordedRequest> requests = server.requests(MCP_PATH);
        assertTrue("至少发出过若干请求", requests.size() >= 3);
        for (TestHttpServer.RecordedRequest sent : requests) {
            assertTrue("目标必须是回环地址: " + sent.header("Host"),
                    String.valueOf(sent.header("Host")).startsWith("127.0.0.1:"));
        }
    }

    /**
     * 录制帧全部取自测试资源目录，内容里不含任何外网地址
     */
    @Test
    public void recordedFramesContainNoExternalAddresses() {
        for (String frame : RECORDED_FRAMES) {
            String text = AiFixtures.resource(frame);
            int index = text.indexOf("http://");
            while (index >= 0) {
                assertTrue("录制帧里只允许回环地址: " + frame,
                        text.startsWith("http://127.0.0.1", index));
                index = text.indexOf("http://", index + 1);
            }
            assertTrue("录制帧里不允许出现https域名: " + frame, !text.contains("https://"));
        }
    }

    private static Map<String, Object> arguments(String city) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("city", city);
        return arguments;
    }
}
