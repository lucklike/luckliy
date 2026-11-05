package com.luckyframework.httpclient.proxy.typeparser.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
 *     5.请求记录（方法/路径/请求头/请求体）与按队列回放的响应体，
 *       用于验证发出的报文内容与多轮请求分别拿到了哪个样本
 * </pre>
 */
public class TestHttpServer implements Closeable {

    /**
     * AI会话接口使用的路径
     */
    public static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

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

    /**
     * 各路径收到的请求记录
     */
    private final Map<String, List<RecordedRequest>> requestRecords = new ConcurrentHashMap<>();

    /**
     * 各路径待回放的响应体队列，队列耗尽后重复使用最后一个
     */
    private final Map<String, Deque<String>> responseQueues = new ConcurrentHashMap<>();
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

    /**
     * 注册一个「记录请求 + 按队列回放JSON响应」的路径，队列未设置时返回空JSON对象
     *
     * @param path 路径
     */
    public void registerJson(String path) {
        register(path, exchange -> respond(exchange, "application/json;charset=UTF-8", nextResponseBody(path)));
    }

    /**
     * 注册一个自定义处理器（命中计数与请求记录照旧），
     * 供需要自定义状态码、响应头、SSE事件流等形态的场景使用
     *
     * @param path    路径
     * @param handler 处理器
     */
    public void registerHandler(String path, ExchangeHandler handler) {
        register(path, handler);
    }

    /**
     * 取指定路径下一个待回应的响应体（按队列顺序，队列耗尽时重复使用最后一个）
     *
     * @param path 路径
     * @return 响应体文本
     */
    public String nextResponseBodyOf(String path) {
        return nextResponseBody(path);
    }

    /**
     * 为指定路径追加一个待回放的响应体
     *
     * @param path 路径
     * @param body 响应体
     */
    public void queueResponse(String path, String body) {
        responseQueues.computeIfAbsent(path, key -> new ConcurrentLinkedDeque<>()).add(body);
    }

    /**
     * 指定路径收到的全部请求记录
     *
     * @param path 路径
     * @return 请求记录列表，按到达顺序
     */
    public List<RecordedRequest> requests(String path) {
        return Collections.unmodifiableList(new ArrayList<>(recordList(path)));
    }

    /**
     * 指定路径最近一次收到的请求记录
     *
     * @param path 路径
     * @return 请求记录，一次都没收到时返回null
     */
    public RecordedRequest lastRequest(String path) {
        List<RecordedRequest> records = recordList(path);
        return records.isEmpty() ? null : records.get(records.size() - 1);
    }

    private List<RecordedRequest> recordList(String path) {
        List<RecordedRequest> records = requestRecords.get(path);
        return records == null ? Collections.emptyList() : new ArrayList<>(records);
    }

    private String nextResponseBody(String path) {
        Deque<String> queue = responseQueues.get(path);
        if (queue == null || queue.isEmpty()) {
            return "{}";
        }
        // 只剩一个响应体时重复使用它，多个时按顺序取用，取到最后一个后停在那里
        return queue.size() == 1 ? queue.peek() : queue.poll();
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
        requestRecords.put(path, Collections.synchronizedList(new ArrayList<>()));
        server.createContext(path, exchange -> {
            hitCounters.get(path).incrementAndGet();
            recordRequest(path, exchange);
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
     * 记录请求的方法、路径、请求头与请求体，供测试断言发出的报文
     * <pre>
     *     请求体在这里被整体读走，处理器只需要负责写响应；
     *     已有的文本/SSE用例不读取请求体，因此不受影响。
     * </pre>
     */
    private void recordRequest(String path, HttpExchange exchange) {
        try {
            byte[] body = readAll(exchange.getRequestBody());
            requestRecords.get(path).add(new RecordedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    new HashMap<>(exchange.getRequestHeaders()),
                    new String(body, StandardCharsets.UTF_8)));
        } catch (Throwable ignored) {
            // 记录失败不影响响应
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * 普通响应：固定长度
     */
    private void respond(HttpExchange exchange, String contentType, String body) throws IOException {
        respond(exchange, 200, contentType, Collections.emptyMap(), body);
    }

    /**
     * 指定状态码与额外响应头的普通响应，供自定义处理器使用
     *
     * @param exchange    本次交换
     * @param status      HTTP状态码
     * @param contentType 响应Content-Type
     * @param headers     额外响应头，可为空
     * @param body        响应体文本
     */
    public void respond(HttpExchange exchange, int status, String contentType,
                        Map<String, String> headers, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                exchange.getResponseHeaders().set(entry.getKey(), entry.getValue());
            }
        }
        if (data.length == 0) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
            out.flush();
        }
    }

    /**
     * 以SSE事件流的形式回应若干条data帧（发送完即关闭连接）
     *
     * @param exchange 本次交换
     * @param headers  额外响应头，可为空
     * @param frames   每条一个JSON帧
     */
    public void respondEventStream(HttpExchange exchange, Map<String, String> headers,
                                   List<String> frames) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                exchange.getResponseHeaders().set(entry.getKey(), entry.getValue());
            }
        }
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        for (String frame : frames) {
            out.write(("data: " + frame + "\n\n").getBytes(StandardCharsets.UTF_8));
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

    public interface ExchangeHandler {
        void handle(HttpExchange exchange) throws Exception;
    }

    /**
     * 一次请求的记录，用于断言客户端实际发出的报文
     */
    public static class RecordedRequest {

        private final String method;
        private final String path;
        private final Map<String, List<String>> headers;
        private final String body;

        RecordedRequest(String method, String path, Map<String, List<String>> headers, String body) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.body = body;
        }

        /**
         * HTTP方法，如POST
         */
        public String method() {
            return method;
        }

        /**
         * 请求路径（不含查询串）
         */
        public String path() {
            return path;
        }

        /**
         * 请求体文本
         */
        public String body() {
            return body;
        }

        /**
         * 取指定请求头的第一个值，头名不区分大小写
         *
         * @param name 头名
         * @return 头值，不存在时返回null
         */
        public String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    List<String> values = entry.getValue();
                    return values == null || values.isEmpty() ? null : values.get(0);
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return method + " " + path + " body=" + body;
        }
    }
}
