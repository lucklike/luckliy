package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.support.FakeHttpMcpServer;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.logging.BeautifulLoggerPrintHandler;
import com.luckyframework.httpclient.proxy.logging.DataMasker;
import com.luckyframework.httpclient.proxy.logging.support.LogMemoryAppender;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 6.8 MCP配置来源与凭据保护：
 * <pre>
 *     1.命令与参数、HTTP地址与请求头全部读自配置文件（或编程式配置），代码里没有硬编码常量；
 *     2.配置里的凭据走框架既有的按名脱敏链路——{@code McpHttpApi} 上声明了「哪些头名属于凭据」，
 *       真实调试日志里该头的值被整体替换为占位符，而路径与JSON-RPC方法名等请求信息仍可读。
 * </pre>
 * 日志收集方式与 {@code DataMaskingLoggerTest} 一致：把内存Appender挂到 BeautifulLoggerPrintHandler
 * 所绑定的LoggerConfig上，断言的是脱敏后的最终日志文本。全程只访问127.0.0.1的本地假server。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class McpConfigMaskingTest {

    /**
     * 配置文件形式的MCP连接配置
     */
    private static final String CONFIG_LOCATION = "classpath:ai/mcp/mcp-config.yml";

    private static final String MCP_PATH = "/mcp";

    /**
     * 配置文件里声明的凭据值，断言它不以明文进日志
     */
    private static final String FILE_TOKEN = "Bearer token-from-config-file";

    private static final String STDIO_TOKEN = "stdio-token-from-config-file";

    private static LogMemoryAppender appender;
    private static org.apache.logging.log4j.core.Logger coreLogger;
    private static LoggerConfig targetLoggerConfig;

    private TestHttpServer server;
    private McpClient client;

    @BeforeClass
    public static void setUpClass() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        coreLogger = context.getLogger(BeautifulLoggerPrintHandler.class.getName());
        coreLogger.setLevel(Level.INFO);
        // 挂在该logger当前绑定的LoggerConfig上，日志既照常输出到控制台，又能被收集用于断言
        targetLoggerConfig = configuration.getLoggerConfig(BeautifulLoggerPrintHandler.class.getName());
        appender = new LogMemoryAppender("McpConfigMaskingAppender");
        appender.start();
        targetLoggerConfig.addAppender(appender, null, null);
    }

    @AfterClass
    public static void tearDownClass() {
        targetLoggerConfig.removeAppender(appender.getName());
        appender.stop();
        coreLogger.setLevel(null);
    }

    @Before
    public void setUp() {
        appender.clear();
        // 全局脱敏配置隔离：本用例断言的正是「按头名脱敏」这一条链路，不启用任何裸值兜底
        DataMasker.clearMaskers();
        DataMasker.clearValueMaskers();
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close();
        }
        DataMasker.clearMaskers();
        DataMasker.clearValueMaskers();
    }

    /**
     * 配置来源：stdio的命令/参数/环境变量与HTTP的地址/请求头/超时全部读自配置文件键
     */
    @Test
    public void connectionSettingsComeFromConfigurationFile() {
        AiConfiguration configuration = AiConfiguration.load(CONFIG_LOCATION);

        McpServerConfig stdio = McpServerConfig.from(configuration, "weather");
        assertTrue("有命令即为stdio配置", stdio.isStdio());
        assertEquals("java", stdio.getCommand().get(0));
        assertEquals("-Dfile.encoding=UTF-8", stdio.getCommand().get(1));
        assertEquals(STDIO_TOKEN, stdio.getEnvironment().get("MCP_TOKEN"));
        assertEquals(5000L, stdio.getTimeoutMillis());

        McpServerConfig http = McpServerConfig.from(configuration, "remote");
        assertFalse("有地址没有命令即为HTTP配置", http.isStdio());
        assertEquals("http://127.0.0.1:18080/mcp", http.getUrl());
        assertEquals(FILE_TOKEN, http.getHeaders().get("Authorization"));
        assertEquals(2000L, http.getTimeoutMillis());
    }

    /**
     * 配置对象的字符串形式不带凭据：日志里直接打印配置对象也不会泄漏
     */
    @Test
    public void configurationToStringOmitsCredentials() {
        AiConfiguration configuration = AiConfiguration.load(CONFIG_LOCATION);

        assertFalse(McpServerConfig.from(configuration, "remote").toString().contains("token-from-config-file"));
        assertFalse(McpServerConfig.from(configuration, "weather").toString().contains(STDIO_TOKEN));
    }

    /**
     * 凭据按名脱敏：地址来自运行期探测的本地端口、请求头来自配置文件，
     * 完成一次真实工具调用后，调试日志里 Authorization 的值是占位符而非令牌明文，
     * 而请求路径与JSON-RPC方法名等排障需要的信息依旧可读
     */
    @Test
    public void credentialHeaderIsMaskedInDebugLog() throws Exception {
        server = new TestHttpServer();
        new FakeHttpMcpServer(server, MCP_PATH);
        McpServerConfig fromFile = McpServerConfig.from(AiConfiguration.load(CONFIG_LOCATION), "remote");
        McpServerConfig config = McpServerConfig.builder(fromFile.getServerName())
                .url(server.baseUrl() + MCP_PATH)
                .headers(fromFile.getHeaders())
                .timeoutMillis(fromFile.getTimeoutMillis())
                .build();

        client = new McpClient(loggingTransport(config), config);
        client.initialize();
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("city", "北京");
        client.callTool("get_weather", arguments);

        String requestLog = stripAnsi(appender.textContaining("REQUEST"));
        assertTrue("请求头名称仍应可读: " + requestLog, requestLog.contains("Authorization"));
        assertTrue("凭据值应被整体脱敏: " + requestLog,
                Pattern.compile("Authorization\\s*:\\s*\\*{8}").matcher(requestLog).find());
        assertFalse("日志中不得出现令牌明文", requestLog.contains("token-from-config-file"));

        assertTrue("请求路径应可读", requestLog.contains(MCP_PATH));
        assertTrue("请求体里的方法名应可读", requestLog.contains("tools/call"));
        assertTrue("请求体里的工具名应可读", requestLog.contains("get_weather"));
    }

    /**
     * 剥离ANSI控制码（请求头名带着色、按列对齐，断言前需先去掉色码）
     */
    private static String stripAnsi(String text) {
        return text.replaceAll("\u001B\\[[;\\d]*[A-Za-z]", "");
    }

    /**
     * 模拟「使用方打开了调试日志」：地址依旧来自配置，只是给承载 {@link McpHttpApi} 的工厂装上了日志处理器
     */
    private static StreamableHttpTransport loggingTransport(McpServerConfig config) {
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.setLoggerHandler(new BeautifulLoggerPrintHandler());
        AiConfiguration.create().put(McpHttpApi.URL_CONFIG_PATH, config.getUrl()).install(factory);
        return new StreamableHttpTransport(config, factory.getProxyObject(McpHttpApi.class));
    }
}
