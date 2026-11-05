package com.luckyframework.httpclient.generalapi.ai.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * 测试用的假MCP server：以 {@code main} 方式被 {@code java -cp target/test-classes} 拉起，
 * 从标准输入按行读「一个完整JSON对象」，按同样格式把响应写到标准输出。
 * <pre>
 *     只实现本能力用到的四个方法，另有两个用于验证异常路径的开关式方法：
 *     {@code hang} 永不回包（验证超时与进程回收），{@code exit} 直接退出进程（验证进程意外退出）。
 *     启动参数 {@code --noisy} 让每一帧前先往stdout写一行非JSON日志，用于验证客户端只认对象帧。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 */
public final class FakeMcpServer {

    /**
     * 该方法名请求会让server一直不回包
     */
    public static final String HANG_METHOD = "hang";

    /**
     * 该方法名请求会让server立即退出进程
     */
    public static final String EXIT_METHOD = "exit";

    private FakeMcpServer() {
    }

    /**
     * 拉起本假server的完整命令（当前JDK的java可执行文件 + 测试类路径），
     * 命令与参数全部由调用方（测试）给出，验证时不访问任何网络
     *
     * @param extraArgs 额外的启动参数
     * @return 命令行
     */
    public static java.util.List<String> command(String... extraArgs) {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(System.getProperty("java.home") + java.io.File.separator + "bin"
                + java.io.File.separator + "java");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(FakeMcpServer.class.getName());
        if (extraArgs != null) {
            command.addAll(java.util.Arrays.asList(extraArgs));
        }
        return command;
    }

    public static void main(String[] args) throws IOException {
        boolean noisy = args.length > 0 && "--noisy".equals(args[0]);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintStream out = new PrintStream(System.out, false, StandardCharsets.UTF_8.name());
        String line;
        while ((line = reader.readLine()) != null) {
            String text = line.trim();
            if (text.isEmpty() || text.charAt(0) != '{') {
                continue;
            }
            if (noisy) {
                // 混入非JSON的日志行，验证客户端只认「一帧一个JSON对象」
                out.println("[fake-mcp-server] handling a frame");
            }
            respond(text, out);
        }
    }

    /**
     * 不解析完整JSON，只用字符串定位id与method，避免假server依赖任何第三方库
     */
    private static void respond(String frame, PrintStream out) {
        String id = extractId(frame);
        String method = extractString(frame, "method");
        if (id == null) {
            // 通知不回包
            return;
        }
        if (HANG_METHOD.equals(method)) {
            return;
        }
        if (EXIT_METHOD.equals(method)) {
            out.flush();
            Runtime.getRuntime().halt(0);
            return;
        }
        String result;
        switch (method == null ? "" : method) {
            case "initialize":
                result = "{\"protocolVersion\":\"2025-06-18\","
                        + "\"capabilities\":{\"tools\":{\"listChanged\":true}},"
                        + "\"serverInfo\":{\"name\":\"fake-server\",\"version\":\"1.0.0\"}}";
                break;
            case "tools/list":
                result = "{\"tools\":["
                        + "{\"name\":\"get_weather\",\"description\":\"查询城市天气\","
                        + "\"inputSchema\":{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\","
                        + "\"description\":\"城市名\"}},\"required\":[\"city\"]}},"
                        + "{\"name\":\"echo\",\"description\":\"回显输入\"}]}";
                break;
            case "tools/call":
                result = "{\"content\":[{\"type\":\"text\",\"text\":\"北京今天晴，12 度\"}],\"isError\":false}";
                break;
            case "ping":
                result = "{}";
                break;
            default:
                write("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"error\":"
                        + "{\"code\":-32601,\"message\":\"Method not found\"}}", out);
                return;
        }
        write("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":" + result + "}", out);
    }

    private static void write(String frame, PrintStream out) {
        out.print(frame);
        out.print('\n');
        out.flush();
    }

    /**
     * 取 {@code "id": 12} 里的数字，取不到返回null（表达这是通知）
     */
    private static String extractId(String frame) {
        int index = frame.indexOf("\"id\"");
        if (index < 0) {
            return null;
        }
        int colon = frame.indexOf(':', index);
        int cursor = colon + 1;
        while (cursor < frame.length() && Character.isWhitespace(frame.charAt(cursor))) {
            cursor++;
        }
        int end = cursor;
        while (end < frame.length() && Character.isDigit(frame.charAt(end))) {
            end++;
        }
        return end > cursor ? frame.substring(cursor, end) : null;
    }

    /**
     * 取 {@code "key":"value"} 形式的字符串值
     */
    private static String extractString(String frame, String key) {
        int index = frame.indexOf("\"" + key + "\"");
        if (index < 0) {
            return null;
        }
        int colon = frame.indexOf(':', index);
        int firstQuote = frame.indexOf('"', colon + 1);
        int secondQuote = frame.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) {
            return null;
        }
        return frame.substring(firstQuote + 1, secondQuote);
    }
}
