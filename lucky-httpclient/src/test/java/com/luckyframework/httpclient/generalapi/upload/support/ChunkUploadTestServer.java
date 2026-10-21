package com.luckyframework.httpclient.generalapi.upload.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分片上传注解端到端测试用的本地假服务端。
 * <pre>
 *     基于JDK内置HttpServer，实现{@code ChunkUploadApi}约定的三个动作：
 *     1.GET  /uploadChunks?fileId=x   → {@code {"code":200,"data":[1,3]}}，data是已收到的分片索引数组
 *     2.POST /chunk?fileId=x&amp;chunkNumber=n → 请求体是该分片的原始字节，返回{@code {"code":200,"data":true}}
 *     3.POST /merge?fileId=x&amp;fileName=y&amp;totalChunks=n → 按索引拼接收到的分片，同样返回JSON包装结果
 *     4.POST /chunkMultipart → 与/chunk功能相同，只是分片信息与内容放在multipart/form-data里，
 *       用于验证{@code @MultiData}/{@code @MultiFile}这类表单协议写法
 *     所有接口统一返回{@code {"code":..,"data":..}}结构的application/json：
 *     框架对text/plain一律按字符串返回，无法自动转成{@code boolean}，
 *     因此返回布尔值的接口需要客户端用{@code @RespConvert("#{$body$.data}")}从包装结果里取值，
 *     查询接口也用这一写法来演示取data字段。
 *
 *     可注入的故障：
 *     1.{@link #failChunk(long, int)}：指定分片前N次尝试失败；
 *     2.{@link #alwaysFailChunk(long)}：指定分片始终失败；
 *     3.{@link #setFailMode(FailMode)}：失败方式是返回false还是抛HTTP 500；
 *     4.{@link #setFailMerge(boolean)}：合并请求始终失败。
 *
 *     分片接口的URL端口在测试运行时才确定，因此地址通过{@link #currentBaseUrl()}静态暴露，
 *     由测试API接口的{@code @ServerAddress(urlFunc=...)}函数取用。
 * </pre>
 */
public class ChunkUploadTestServer implements Closeable {

    public static final String UPLOAD_CHUNKS_PATH = "/uploadChunks";
    public static final String CHUNK_PATH = "/chunk";
    public static final String CHUNK_MULTIPART_PATH = "/chunkMultipart";
    public static final String MERGE_PATH = "/merge";

    /**
     * JSON响应的媒体类型，只有JSON才能被框架自动转成boolean与集合类型
     */
    private static final String APPLICATION_JSON = "application/json;charset=UTF-8";

    /**
     * 当前正在运行的假服务端地址，供测试API接口拼接URL
     */
    private static volatile String currentBaseUrl;

    private final HttpServer server;
    private final ExecutorService executor;

    /**
     * 每个文件已收到的分片，内层按分片索引排序
     */
    private final Map<String, TreeMap<Long, byte[]>> chunkStore = new ConcurrentHashMap<>();

    /**
     * 合并后的文件内容
     */
    private final Map<String, byte[]> mergedFiles = new ConcurrentHashMap<>();

    /**
     * 各路径的命中次数
     */
    private final Map<String, AtomicInteger> hitCounters = new ConcurrentHashMap<>();

    /**
     * 每个分片索引已尝试的次数
     */
    private final Map<Long, AtomicInteger> chunkAttempts = new ConcurrentHashMap<>();

    private final Map<Long, Integer> failTimes = new ConcurrentHashMap<>();

    private final Set<Long> alwaysFailChunks = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    /**
     * 合并收到的分片总数参数，用于校验元数据传递是否正确
     */
    private volatile int mergedTotalChunks = -1;

    private volatile String mergedFileName;

    private volatile FailMode failMode = FailMode.RETURN_FALSE;

    private volatile boolean failMerge;

    /**
     * 分片失败的表现形式
     */
    public enum FailMode {
        /**
         * 返回false，由内核判定为业务失败
         */
        RETURN_FALSE,
        /**
         * 返回HTTP 500，让协议方法抛出异常
         */
        THROW_EXCEPTION
    }

    public ChunkUploadTestServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "chunk-upload-test-server");
            thread.setDaemon(true);
            return thread;
        });
        this.server.setExecutor(executor);
        registerHandlers();
        this.server.start();
        currentBaseUrl = baseUrl();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    /**
     * 获取当前运行的假服务端地址
     *
     * @return 地址，服务器未启动时返回null
     */
    public static String currentBaseUrl() {
        return currentBaseUrl;
    }

    /**
     * 让指定分片的前{@code times}次尝试失败
     *
     * @param index 分片索引
     * @param times 失败次数
     */
    public void failChunk(long index, int times) {
        failTimes.put(index, times);
    }

    /**
     * 让指定分片始终失败
     *
     * @param index 分片索引
     */
    public void alwaysFailChunk(long index) {
        alwaysFailChunks.add(index);
    }

    public void setFailMode(FailMode failMode) {
        this.failMode = failMode;
    }

    public void setFailMerge(boolean failMerge) {
        this.failMerge = failMerge;
    }

    /**
     * 预置服务端已收到的分片，用于断点续传用例
     *
     * @param fileId  文件唯一标识
     * @param index   分片索引
     * @param content 分片内容
     */
    public void putChunk(String fileId, long index, byte[] content) {
        store(fileId).put(index, content);
    }

    /**
     * 指定路径的命中次数
     *
     * @param path 路径
     * @return 命中次数
     */
    public int hit(String path) {
        AtomicInteger counter = hitCounters.get(path);
        return counter == null ? 0 : counter.get();
    }

    /**
     * 指定分片的实际尝试次数
     *
     * @param index 分片索引
     * @return 尝试次数
     */
    public int attemptsOf(long index) {
        AtomicInteger counter = chunkAttempts.get(index);
        return counter == null ? 0 : counter.get();
    }

    /**
     * 服务端已收到的分片索引
     *
     * @param fileId 文件唯一标识
     * @return 升序的分片索引集合
     */
    public List<Long> receivedIndexes(String fileId) {
        TreeMap<Long, byte[]> chunks = chunkStore.get(fileId);
        return chunks == null ? Collections.emptyList() : new ArrayList<>(chunks.keySet());
    }

    /**
     * 合并后的文件内容
     *
     * @param fileId 文件唯一标识
     * @return 内容，未合并时返回null
     */
    public byte[] mergedContent(String fileId) {
        return mergedFiles.get(fileId);
    }

    public int getMergedTotalChunks() {
        return mergedTotalChunks;
    }

    public String getMergedFileName() {
        return mergedFileName;
    }

    @Override
    public void close() {
        if (currentBaseUrl != null && currentBaseUrl.equals(baseUrl())) {
            currentBaseUrl = null;
        }
        try {
            server.stop(0);
        } catch (Throwable ignored) {
            // ignore
        }
        executor.shutdownNow();
    }

    private void registerHandlers() {
        for (String path : Arrays.asList(UPLOAD_CHUNKS_PATH, CHUNK_PATH, CHUNK_MULTIPART_PATH, MERGE_PATH)) {
            hitCounters.put(path, new AtomicInteger());
        }
        server.createContext(UPLOAD_CHUNKS_PATH, this::handleUploadedChunks);
        server.createContext(CHUNK_PATH, this::handleChunk);
        server.createContext(CHUNK_MULTIPART_PATH, this::handleChunkMultipart);
        server.createContext(MERGE_PATH, this::handleMerge);
    }

    /**
     * 返回指定文件已收到的分片索引
     */
    private void handleUploadedChunks(HttpExchange exchange) throws IOException {
        hitCounters.get(UPLOAD_CHUNKS_PATH).incrementAndGet();
        respond(exchange, APPLICATION_JSON, uploadedIndexesJson(queryParam(exchange, "fileId")));
    }

    /**
     * 生成已上传分片索引的JSON包装结果，如{@code {"code":200,"data":[1,3]}}
     */
    private String uploadedIndexesJson(String fileId) {
        List<Long> indexes = receivedIndexes(fileId);
        StringBuilder text = new StringBuilder(64).append("{\"code\":200,\"data\":[");
        for (int i = 0; i < indexes.size(); i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append(indexes.get(i));
        }
        return text.append("]}").toString();
    }

    private void handleChunk(HttpExchange exchange) throws IOException {
        hitCounters.get(CHUNK_PATH).incrementAndGet();
        String fileId = queryParam(exchange, "fileId");
        long index = Long.parseLong(queryParam(exchange, "chunkNumber"));
        acceptChunk(exchange, fileId, index, readBody(exchange));
    }

    /**
     * 处理multipart/form-data形式的分片上传：普通表单项取fileId与chunkNumber，文件项name=file的部分是分片内容
     */
    private void handleChunkMultipart(HttpExchange exchange) throws IOException {
        hitCounters.get(CHUNK_MULTIPART_PATH).incrementAndGet();
        Map<String, String> fields = new HashMap<>();
        Map<String, byte[]> files = new HashMap<>();
        parseMultipart(boundaryOf(exchange), readBody(exchange), fields, files);
        long index = Long.parseLong(fields.get("chunkNumber"));
        acceptChunk(exchange, fields.get("fileId"), index, files.get("file"));
    }

    /**
     * 按已注入的故障规则接收一个分片
     *
     * @param exchange 当前请求
     * @param fileId   文件唯一标识
     * @param index    分片索引
     * @param content  分片内容
     */
    private void acceptChunk(HttpExchange exchange, String fileId, long index, byte[] content) throws IOException {
        int attempt = chunkAttempts.computeIfAbsent(index, key -> new AtomicInteger()).incrementAndGet();
        Integer limitedFailTimes = failTimes.get(index);
        boolean shouldFail = alwaysFailChunks.contains(index) || (limitedFailTimes != null && attempt <= limitedFailTimes);
        if (shouldFail) {
            if (failMode == FailMode.THROW_EXCEPTION) {
                respondStatus(exchange, 500, "chunk-" + index + "-failed");
                return;
            }
            respondBooleanResult(exchange, false);
            return;
        }
        store(fileId).put(index, content);
        respondBooleanResult(exchange, true);
    }

    private void handleMerge(HttpExchange exchange) throws IOException {
        hitCounters.get(MERGE_PATH).incrementAndGet();
        String fileId = queryParam(exchange, "fileId");
        mergedFileName = queryParam(exchange, "fileName");
        mergedTotalChunks = Integer.parseInt(queryParam(exchange, "totalChunks"));
        if (failMerge) {
            respondStatus(exchange, 500, "merge-failed");
            return;
        }
        TreeMap<Long, byte[]> chunks = store(fileId);
        Set<Long> missing = new LinkedHashSet<>();
        for (long i = 1; i <= mergedTotalChunks; i++) {
            if (!chunks.containsKey(i)) {
                missing.add(i);
            }
        }
        if (!missing.isEmpty()) {
            // 分片不完整还收到合并通知，说明客户端的止损/重试逻辑有问题
            respondBooleanResult(exchange, false);
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (long i = 1; i <= mergedTotalChunks; i++) {
            byte[] content = chunks.get(i);
            out.write(content, 0, content.length);
        }
        mergedFiles.put(fileId, out.toByteArray());
        respondBooleanResult(exchange, true);
    }

    /**
     * 返回{@code {"code":200,"data":true}}形式的布尔结果
     *
     * @param exchange 当前请求
     * @param success  业务是否成功
     */
    private static void respondBooleanResult(HttpExchange exchange, boolean success) throws IOException {
        respond(exchange, APPLICATION_JSON, "{\"code\":200,\"data\":" + success + "}");
    }

    private TreeMap<Long, byte[]> store(String fileId) {
        return chunkStore.computeIfAbsent(fileId, key -> new TreeMap<>());
    }

    private static String queryParam(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int split = pair.indexOf('=');
            if (split > 0 && name.equals(pair.substring(0, split))) {
                try {
                    return URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8.name());
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to decode query param " + name, e);
                }
            }
        }
        return null;
    }

    private static byte[] readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    /**
     * 从Content-Type中取出multipart的分隔符
     */
    private static String boundaryOf(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        int start = contentType.indexOf("boundary=");
        if (start < 0) {
            throw new IllegalStateException("multipart request without boundary: " + contentType);
        }
        String boundary = contentType.substring(start + "boundary=".length());
        int semicolon = boundary.indexOf(';');
        if (semicolon >= 0) {
            boundary = boundary.substring(0, semicolon);
        }
        return boundary.replace("\"", "").trim();
    }

    /**
     * 最小可用的multipart解析：按分隔符拆部分，每部分取头部里的name，带filename的归为文件项，其余归为普通表单项
     * <pre>
     *     只用于本测试，不处理嵌套multipart与转义字符；文件内容按字节截取，避免二进制被转码损坏。
     * </pre>
     */
    private static void parseMultipart(String boundary, byte[] body, Map<String, String> fields, Map<String, byte[]> files) {
        byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] headEnd = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        int position = indexOf(body, delimiter, 0);
        while (position >= 0) {
            int partStart = position + delimiter.length;
            // 结束分隔符后面紧跟"--"，此时已处理完所有部分
            if (partStart + 1 >= body.length || (body[partStart] == '-' && body[partStart + 1] == '-')) {
                break;
            }
            int next = indexOf(body, delimiter, partStart);
            if (next < 0) {
                break;
            }
            int headEndAt = indexOf(body, headEnd, partStart);
            if (headEndAt >= 0 && headEndAt < next) {
                String headers = new String(body, partStart, headEndAt - partStart, StandardCharsets.UTF_8);
                String name = attributeName(headers, "name");
                int valueStart = headEndAt + headEnd.length;
                int valueEnd = next - 2;
                if (name != null && valueEnd > valueStart) {
                    if (attributeName(headers, "filename") != null) {
                        files.put(name, Arrays.copyOfRange(body, valueStart, valueEnd));
                    } else {
                        fields.put(name, new String(body, valueStart, valueEnd - valueStart, StandardCharsets.UTF_8));
                    }
                }
            }
            position = next;
        }
    }

    /**
     * 从Content-Disposition头部文本中取指定属性的值
     */
    private static String attributeName(String headers, String attribute) {
        String prefix = " " + attribute + "=\"";
        int start = headers.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = headers.indexOf('"', start);
        return end < 0 ? null : headers.substring(start, end);
    }

    /**
     * 在字节数组中查找子串
     */
    private static int indexOf(byte[] source, byte[] target, int from) {
        outer:
        for (int i = Math.max(from, 0); i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static void respond(HttpExchange exchange, String contentType, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
            out.flush();
        }
    }

    private static void respondStatus(HttpExchange exchange, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        // 失败响应也给出JSON媒体类型，避免失败原因变成无关的响应体转换异常
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
            out.flush();
        }
    }
}
