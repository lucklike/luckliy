package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.InMemoryMcpTransport;
import com.luckyframework.httpclient.generalapi.ai.support.McpTestSupport;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolExecutor;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistryException;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolResult;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 6.4/6.5 MCP工具桥接：远端清单转本地声明（含分页取全、缺失schema补空object、重名冲突与前缀选项），
 * 以及 {@code tools/call} 包装成执行实现后的四类结果转换（成功文本、isError、JSON-RPC错误、多分片连接）。
 * 全部由内存假通道与录制帧驱动，不触碰网络与子进程。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class McpToolBridgeTest {

    private InMemoryMcpTransport transport;
    private McpClient client;
    private McpToolBridge bridge;
    private ToolRegistry registry;
    private ExecutorService executor;

    @Before
    public void setUp() {
        transport = new InMemoryMcpTransport();
        client = new McpClient(transport, McpServerConfig.builder("example").timeoutMillis(2000).build());
        bridge = new McpToolBridge(client);
        registry = new ToolRegistry();
        executor = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        client.close();
    }

    /**
     * 单页两工具注册：名称、描述、schema的properties与required与远端字段逐项一致
     */
    @Test
    public void singlePageToolsAreRegisteredFieldByField() throws Exception {
        handshake("ai/mcp/initialize-response.json");
        List<ToolSpec> registered = registerTools("ai/mcp/tools-list-response.json");

        assertEquals(2, registered.size());
        assertEquals("get_weather", registered.get(0).getName());
        assertEquals("get_time", registered.get(1).getName());

        ToolSpec weather = registry.getSpec("get_weather");
        assertEquals("查询城市天气", weather.getDescription());
        Map<String, Object> properties = McpTestSupport.asMap(weather.getParameters().get("properties"));
        Map<String, Object> city = McpTestSupport.asMap(properties.get("city"));
        assertEquals("object", weather.getParameters().get("type"));
        assertEquals("string", city.get("type"));
        assertEquals("城市名", city.get("description"));
        assertEquals(Collections.singletonList("city"),
                McpTestSupport.asList(weather.getParameters().get("required")));

        // 无执行实现的声明不可能出现：远端工具一律带桥接执行器
        assertTrue(registry.findExecutor("get_weather").isPresent());
    }

    /**
     * 分页取全：响应带nextCursor时携带cursor继续请求，直到取完全部工具
     */
    @Test
    public void pagedToolsAreAllFetched() throws Exception {
        handshake("ai/mcp/initialize-response.json");
        Future<List<ToolSpec>> registering = executor.submit(() -> bridge.registerAll(registry));
        waitFrames(3);
        assertEquals("tools/list", transport.sentFrame(2).get("method"));
        deliverRecorded("ai/mcp/tools-list-page1.json");

        waitFrames(4);
        Map<String, Object> secondPage = transport.sentFrame(3);
        assertEquals("tools/list", secondPage.get("method"));
        assertEquals("c1", McpTestSupport.paramsOf(secondPage).get("cursor"));
        deliverRecorded("ai/mcp/tools-list-page2.json");

        List<ToolSpec> registered = registering.get(2, TimeUnit.SECONDS);
        assertEquals(2, registered.size());
        assertEquals(2, registry.size());
        assertTrue(registry.contains("get_weather"));
        assertTrue(registry.contains("get_time"));
    }

    /**
     * 远端缺inputSchema时补成空的object schema
     */
    @Test
    public void missingInputSchemaBecomesEmptyObjectSchema() throws Exception {
        handshake("ai/mcp/initialize-response.json");
        registerTools("ai/mcp/tools-list-missing-schema.json");

        ConfigurationMap parameters = registry.getSpec("ping_server").getParameters();
        assertEquals("object", parameters.get("type"));
        assertTrue(McpTestSupport.asMap(parameters.get("properties")).isEmpty());
    }

    /**
     * 与本地工具重名时默认抛冲突异常，且第一次登记的内容保持不变
     */
    @Test
    public void duplicateNameWithLocalToolFailsByDefault() throws Exception {
        registry.register(AiFixtures.weatherToolSpec());
        handshake("ai/mcp/initialize-response.json");
        try {
            registerTools("ai/mcp/tools-list-response.json");
            fail("远端工具与本地工具重名时必须报冲突");
        } catch (ToolRegistryException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("get_weather"));
        }
        assertEquals("冲突后本地声明不变", "查询城市天气", registry.getSpec("get_weather").getDescription());
        assertFalse(registry.contains("get_time"));
    }

    /**
     * 前缀选项下注册名唯一，而远端 {@code tools/call} 仍使用原始名
     */
    @Test
    public void prefixedRegistrationKeepsRemoteOriginalName() throws Exception {
        registry.register(AiFixtures.weatherToolSpec());
        handshake("ai/mcp/initialize-response.json");
        Future<List<ToolSpec>> registering = executor.submit(() -> bridge.prefix("example").registerAll(registry));
        waitFrames(3);
        deliverRecorded("ai/mcp/tools-list-page1.json");
        waitFrames(4);
        deliverRecorded("ai/mcp/tools-list-page2.json");
        registering.get(2, TimeUnit.SECONDS);

        assertTrue(registry.contains("get_weather"));
        assertTrue(registry.contains("example_get_weather"));
        assertNotEquals(registry.getSpec("get_weather").getName(), registry.getSpec("example_get_weather").getName());

        ToolResult result = call("example_get_weather", McpTestSupport.param("city", "北京"),
                "ai/mcp/tools-call-response.json");
        Map<String, Object> request = lastRequestFrame();
        assertEquals("tools/call", request.get("method"));
        Map<String, Object> params = McpTestSupport.paramsOf(request);
        assertEquals("远端调用必须使用原始工具名", "get_weather", params.get("name"));
        assertEquals("北京", McpTestSupport.asMap(params.get("arguments")).get("city"));
        assertEquals("北京今天晴，12 度", result.getContent());
    }

    /**
     * 远端业务失败（isError=true）转成失败的工具结果，内容含远端说明文本
     */
    @Test
    public void remoteBusinessFailureBecomesFailureResult() throws Exception {
        registerRemoteTool();
        ToolResult result = call("get_weather", McpTestSupport.param("city", "北京"),
                "ai/mcp/tools-call-response-is-error.json");
        assertFalse(result.isSuccess());
        assertTrue(result.getContent(), result.getContent().contains("城市不存在"));
    }

    /**
     * JSON-RPC层error响应同样转成失败结果（含错误码与错误说明），不抛未捕获异常
     */
    @Test
    public void jsonRpcErrorResponseBecomesFailureResult() throws Exception {
        registerRemoteTool();
        int baseline = transport.sentCount() + 1;
        Future<ToolResult> calling = executor.submit(
                () -> callExecutor(registry.findExecutor("get_weather").get(), McpTestSupport.param("city", "北京")));
        waitFrames(baseline);
        int id = idOf(transport.sentFrame(baseline - 1));
        transport.deliver("{\"jsonrpc\":\"2.0\",\"id\":" + id
                + ",\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}");

        ToolResult result = calling.get(2, TimeUnit.SECONDS);
        assertFalse(result.isSuccess());
        assertTrue(result.getContent(), result.getContent().contains("-32601"));
        assertTrue(result.getContent(), result.getContent().contains("Method not found"));
    }

    /**
     * 多个text分片按顺序以换行连接
     */
    @Test
    public void multipleTextPartsAreJoinedByNewline() throws Exception {
        registerRemoteTool();
        ToolResult result = call("get_weather", McpTestSupport.param("city", "北京"),
                "ai/mcp/tools-call-response-two-texts.json");
        assertTrue(result.isSuccess());
        assertEquals("北京今天晴\n，12 度。", result.getContent());
    }

    /**
     * 非text分片不被静默丢弃，以可识别的占位说明保留
     */
    @Test
    public void nonTextPartKeepsAnIdentifiablePlaceholder() throws Exception {
        registerRemoteTool();
        ToolResult result = call("get_weather", McpTestSupport.param("city", "北京"),
                "ai/mcp/tools-call-response-mixed-parts.json");
        assertTrue(result.getContent(), result.getContent().contains("天气图已生成"));
        assertTrue(result.getContent(), result.getContent().contains("image"));
        assertEquals(2, result.getContent().split("\n", -1).length);
    }

    /**
     * server未声明tools能力时桥接不登记任何远端工具
     */
    @Test
    public void bridgeRegistersNothingWhenServerHasNoTools() throws Exception {
        handshake("ai/mcp/initialize-response-no-tools.json");
        assertTrue(bridge.registerAll(registry).isEmpty());
        assertEquals(0, registry.size());
        assertEquals("握手两帧之后再无请求", 2, transport.sentCount());
    }

    /**
     * 完成握手并登记一个只含get_weather的远端工具，供调用桥接断言使用
     */
    private void registerRemoteTool() throws Exception {
        handshake("ai/mcp/initialize-response.json");
        registerTools("ai/mcp/tools-list-response.json");
    }

    private List<ToolSpec> registerTools(String toolsListResource) throws Exception {
        Future<List<ToolSpec>> registering = executor.submit(() -> bridge.registerAll(registry));
        waitFrames(3);
        deliverRecorded(toolsListResource);
        try {
            return registering.get(2, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof ToolRegistryException) {
                throw (ToolRegistryException) e.getCause();
            }
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new AssertionError(e.getCause());
        }
    }

    /**
     * 通过注册表里的桥接执行器调用远端工具，并回放录制的响应帧
     */
    private ToolResult call(String localName, Map<String, Object> args, String responseResource) throws Exception {
        int baseline = transport.sentCount();
        Future<ToolResult> calling = executor.submit(
                () -> callExecutor(registry.findExecutor(localName).get(), args));
        waitFrames(baseline + 1);
        deliverRecorded(responseResource);
        return calling.get(2, TimeUnit.SECONDS);
    }

    /**
     * 最近一次发出的请求帧
     */
    private Map<String, Object> lastRequestFrame() {
        return transport.sentFrame(transport.sentCount() - 1);
    }

    /**
     * 执行器只能返回成/败两态结果，异常已经由桥接层收敛
     */
    private static ToolResult callExecutor(ToolExecutor executor, Map<String, Object> args) {
        try {
            return (ToolResult) executor.execute(args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new AssertionError("桥接执行器不得把远端失败抛成异常: " + t, t);
        }
    }

    private void handshake(String initializeResource) throws Exception {
        Future<Map<String, Object>> capabilities = executor.submit(() -> client.initialize());
        waitFrames(1);
        deliverRecorded(initializeResource);
        capabilities.get(2, TimeUnit.SECONDS);
    }

    /**
     * 取出录制帧并把id改成当前待应答请求的id后交付，模拟server的响应
     */
    private void deliverRecorded(String path) {
        Map<String, Object> frame = AiFixtures.jsonResource(path);
        frame.put("id", idOf(transport.sentFrame(transport.sentCount() - 1)));
        transport.deliver(AiJsonUtils.toJson(frame));
    }

    private void waitFrames(int expected) throws InterruptedException {
        for (int i = 0; i < 200 && transport.sentCount() < expected; i++) {
            Thread.sleep(10);
        }
        assertTrue("应当已发出第" + expected + "帧，实际只有" + transport.sentCount() + "帧",
                transport.sentCount() >= expected);
    }

    private static int idOf(Map<String, Object> frame) {
        return ((Number) frame.get("id")).intValue();
    }
}
