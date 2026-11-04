package com.luckyframework.httpclient.generalapi.ai.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JSON-RPC 2.0 的请求-响应配对通道：包一条 {@link McpTransport}，
 * 把「发出请求」与「拿到同id的响应」在异步消息流里对上号。
 * <pre>
 *     规则：
 *     1. 请求带id，登记一个等待槽后发送，用超时等待取回同id的响应；
 *     2. 无id的通知不参与配对，也不会被当成某个请求的响应（直接忽略，不打断配对）；
 *     3. 响应可以乱序到达，每个调用只会被自己那一条 {@code id} 的响应唤醒。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @apiNote 内部API，由 {@link McpClient} 持有
 */
public class JsonRpcChannel implements McpTransport.Listener {

    /**
     * 单个请求的等待槽：响应到达或连接断开时唤醒等待方
     */
    private static final class Pending {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<Map<String, Object>> response = new AtomicReference<>();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        void complete(Map<String, Object> frame) {
            response.set(frame);
            latch.countDown();
        }

        void fail(Throwable cause) {
            failure.compareAndSet(null, cause);
            latch.countDown();
        }

        /**
         * 等待响应；超时返回null（由调用方决定报错与是否回收通道），断开则抛出失败原因
         */
        Map<String, Object> await(String method, int id, long timeoutMillis) {
            try {
                if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                    return null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new McpException(e, "The MCP request '" + method + "' (id=" + id + ") was interrupted.");
            }
            Throwable cause = failure.get();
            if (cause != null) {
                if (cause instanceof McpException) {
                    throw (McpException) cause;
                }
                throw McpException.disconnected(cause);
            }
            return response.get();
        }
    }

    private final McpTransport transport;
    private final JsonRpcIdGenerator idGenerator = new JsonRpcIdGenerator();
    private final Map<Integer, Pending> pendings = new ConcurrentHashMap<>();
    private volatile long timeoutMillis = 30_000L;

    /**
     * 超时时是否顺手关闭通道：stdio场景下子进程已经不响应了，回收比留着更符合契约
     */
    private volatile boolean closeOnTimeout;

    public JsonRpcChannel(McpTransport transport) {
        this.transport = transport;
        transport.open(this);
    }

    /**
     * 设置单次请求的等待超时
     *
     * @param timeoutMillis 超时毫秒数，非正数时保持原值
     * @return 自身
     */
    public JsonRpcChannel timeout(long timeoutMillis) {
        if (timeoutMillis > 0) {
            this.timeoutMillis = timeoutMillis;
        }
        return this;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * 请求超时时是否同时关闭通道（stdio传输必须开启，避免残留子进程）
     *
     * @param closeOnTimeout true表示超时即回收
     * @return 自身
     */
    public JsonRpcChannel closeOnTimeout(boolean closeOnTimeout) {
        this.closeOnTimeout = closeOnTimeout;
        return this;
    }

    public boolean isOpen() {
        return transport.isOpen();
    }

    /**
     * 发送一个请求并等待同id的响应
     *
     * @param method 方法名
     * @param params 参数对象，可为null
     * @return 响应帧（含result或error节点）
     * @throws McpException 超时、连接断开、或对端回了配不上对的响应
     */
    public Map<String, Object> request(String method, Object params) {
        if (!transport.isOpen()) {
            throw McpException.disconnected(new IllegalStateException("the MCP transport is already closed"));
        }
        int id = idGenerator.next();
        Pending pending = new Pending();
        pendings.put(id, pending);
        try {
            transport.send(JsonRpcMessage.request(id, method, paramsOf(params)).toJson());
            Map<String, Object> response = pending.await(method, id, timeoutMillis);
            if (response == null) {
                if (closeOnTimeout) {
                    close();
                }
                throw McpException.timeout(method, id, timeoutMillis);
            }
            return response;
        } finally {
            pendings.remove(id);
        }
    }

    /**
     * 发送一个通知（无id，不等响应）
     *
     * @param method 方法名
     * @param params 参数对象，可为null
     */
    public void notify(String method, Object params) {
        transport.send(JsonRpcMessage.notification(method, paramsOf(params)).toJson());
    }

    @Override
    public void onMessage(Map<String, Object> frame) {
        JsonRpcMessage<Object> message = JsonRpcMessage.fromFrame(frame);
        if (message == null) {
            return;
        }
        // 通知（无id）与请求（有id有method，本端不会收到server发来的请求）一律忽略，不打断配对
        if (message.isNotification() || message.isRequest()) {
            return;
        }
        Integer id = message.getId();
        if (id == null) {
            return;
        }
        Pending pending = pendings.get(id);
        if (pending == null) {
            // 迟到的响应（调用方已超时退出）直接丢弃，不影响其它请求
            return;
        }
        pending.complete(frame);
    }

    @Override
    public void onDisconnect(Throwable cause) {
        for (Map.Entry<Integer, Pending> entry : pendings.entrySet()) {
            entry.getValue().fail(cause == null ? new IllegalStateException("MCP transport closed") : cause);
        }
    }

    public void close() {
        transport.close();
        onDisconnect(null);
    }

    /**
     * 参数为null时给空对象，保证 {@code tools/list} 这类无参请求的报文里出现 {@code "params": {}}
     */
    private static Object paramsOf(Object params) {
        return params == null ? new LinkedHashMap<String, Object>() : params;
    }
}
