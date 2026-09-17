package com.luckyframework.httpclient.proxy.typeparser.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于JDK内置HttpServer的本地测试HTTP服务器，支持：
 * <pre>
 *     1.普通响应（文本/JSON/空响应）
 *     2.慢响应（延迟返回，用于构造"任务尚未完成"的中间态）
 *     3.SSE流式响应（分块逐行发送、发送后挂起不关闭连接、截断断开）
 *     4.每个路径的请求计数（命中数），用于验证"是否发出了HTTP请求"以及"请求次数"
 * </pre>
 */
public class TestHttpServer implements Closeable {

    public static final String MONO_HELLO_PATH = "/mono/hello";
    public static final String MONO_EMPTY_PATH = "/mono/empty";
    public static final String MONO_SLOW_PATH = "/mono/slow";
    public static final String FLUX_LIST_PATH = "/flux/list";
    public static final String FLUX_SLOW_PATH = "/flux/slow";
    public static final String FLUX_BAD_JSON_PATH = "/flux/badjson";
    public static final String SSE_BASIC_PATH = "/sse/basic";
    public static final String SSE_STALL_PATH = "/sse/stall";
    public static final String SSE_BROKEN_PATH = "/sse/broken";

    public static final String MONO_HELLO_BODY = "hello-mono";
    public static final String MONO_SLOW_BODY = "slow-mono";
    public static final String FLUX_LIST_BODY = "[{\"id\":1,\"name\":\"alice\"},{\"id\":2,\"name\":\"bob\"},{\"id\":3,\"name\":\"carol\"}]";
    public static final String FLUX_BAD_JSON_BODY = "not-a-json-object";
    public static final List<String> SSE_LINES = Arrays.asList("line-1", "line-2", "line-3");
    public static final long SLOW_DELAY_MILLIS = 2000L;

    private final HttpServer server;
    private final ExecutorService executor;
    private final Map<String, AtomicInteger> hitCounters = new ConcurrentHashMap<>();
    private volatile boolean stopped;

    public TestHttpServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "test-http-server");
            thread.setDaemon(true);
            return thread;
        });
        this.server.setExecutor(executor);
        registerHandlers();
        this.server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /**
     * 获取指定路径的请求命中次数
     */
    public int hit(String path) {
        AtomicInteger counter = hitCounters.get(path);
        return counter == null ? 0 : counter.get();
    }

    @Override
    public void close() {
        stopped = true;
        try {
            server.stop(0);
        } catch (Throwable ignored) {
            // ignore
        }
        executor.shutdownNow();
    }

    private void registerHandlers() {
        register(MONO_HELLO_PATH, exchange -> respond(exchange, "text/plain;charset=UTF-8", MONO_HELLO_BODY));
        register(MONO_EMPTY_PATH, exchange -> respond(exchange, "text/plain;charset=UTF-8", ""));
        register(MONO_SLOW_PATH, exchange -> {
            sleepQuietly(SLOW_DELAY_MILLIS);
            respond(exchange, "text/plain;charset=UTF-8", MONO_SLOW_BODY);
        });
        register(FLUX_LIST_PATH, exchange -> respond(exchange, "application/json;charset=UTF-8", FLUX_LIST_BODY));
        register(FLUX_SLOW_PATH, exchange -> {
            sleepQuietly(SLOW_DELAY_MILLIS);
            respond(exchange, "application/json;charset=UTF-8", FLUX_LIST_BODY);
        });
        register(FLUX_BAD_JSON_PATH, exchange -> respond(exchange, "application/json;charset=UTF-8", FLUX_BAD_JSON_BODY));
        register(SSE_BASIC_PATH, this::handleSseBasic);
        register(SSE_STALL_PATH, this::handleSseStall);
        register(SSE_BROKEN_PATH, this::handleSseBroken);
    }

    private void register(String path, ExchangeHandler handler) {
        hitCounters.put(path, new AtomicInteger());
        server.createContext(path, exchange -> {
            hitCounters.get(path).incrementAndGet();
            try {
                handler.handle(exchange);
            } catch (Throwable ignored) {
                // 客户端提前断开等场景下服务端写数据会失败，忽略即可
            } finally {
                closeQuietly(exchange);
            }
        });
    }

    /**
     * 普通响应：固定长度
     */
    private void respond(HttpExchange exchange, String contentType, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (data.length == 0) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
            out.flush();
        }
    }

    /**
     * SSE基础流：逐行发送N行后正常关闭连接
     */
    private void handleSseBasic(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        for (String line : SSE_LINES) {
            out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            sleepQuietly(30);
        }
    }

    /**
     * SSE挂起流：发送1行后保持连接且不再发送数据（用于验证取消订阅能否及时中断阻塞中的读取）
     */
    private void handleSseStall(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        out.write("stall-1\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
        long deadline = System.currentTimeMillis() + 20000;
        while (!stopped && System.currentTimeMillis() < deadline) {
            sleepQuietly(100);
        }
    }

    /**
     * SSE截断响应：声明Content-Length为100但只写入部分数据后断开连接
     */
    private void handleSseBroken(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
        exchange.sendResponseHeaders(200, 100);
        OutputStream out = exchange.getResponseBody();
        out.write("partial".getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(HttpExchange exchange) {
        try {
            exchange.close();
        } catch (Throwable ignored) {
            // ignore
        }
    }

    interface ExchangeHandler {
        void handle(HttpExchange exchange) throws Exception;
    }
}
