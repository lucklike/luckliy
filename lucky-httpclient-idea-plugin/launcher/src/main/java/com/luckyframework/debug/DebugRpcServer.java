package com.luckyframework.debug;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NDJSON over TCP 的调试 RPC 服务。
 *
 * 读循环只做解析与分派，耗时的 invoke 交给工作线程，
 * 保证 cancel 请求不会被正在执行的调用阻塞。
 */
final class DebugRpcServer {

    private final DebugContainer container;
    private final DebugInvoker invoker;
    private final ExecutorService workers;
    private final Map<String, Future<?>> running = new ConcurrentHashMap<String, Future<?>>();

    private ServerSocket serverSocket;
    private volatile Socket client;
    private volatile BufferedWriter writer;

    DebugRpcServer(DebugContainer container) {
        this.container = container;
        this.invoker = new DebugInvoker(container);
        final AtomicInteger counter = new AtomicInteger();
        this.workers = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "lucky-debug-rpc-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    void bind() throws IOException {
        serverSocket = new ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"));
    }

    int port() {
        return serverSocket.getLocalPort();
    }

    void serve() {
        try {
            client = serverSocket.accept();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(
                    new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                dispatchLine(line);
            }
        } catch (IOException ignored) {
            // 客户端断开或进程退出，交给 finally 统一收尾
        }
    }

    void close() {
        Socket socket = client;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        workers.shutdownNow();
    }

    private void dispatchLine(String line) {
        JsonNode request;
        try {
            request = DebugJson.MAPPER.readTree(line);
        } catch (Exception e) {
            return;
        }
        if (request == null || !request.isObject()) {
            return;
        }
        final String id = DebugJson.text(request, "id", "");
        final String op = DebugJson.text(request, "op", "");
        if (id.isEmpty() || op.isEmpty()) {
            return;
        }
        // 关闭请求不走线程池：需要先回包，再关容器、退出进程
        if (DebugProtocol.OP_SHUTDOWN.equals(op)) {
            handleShutdown(id);
            return;
        }

        final JsonNode payload = request;
        Future<?> future = workers.submit(new Runnable() {
            @Override
            public void run() {
                long started = System.currentTimeMillis();
                ObjectNode response;
                try {
                    response = handle(id, op, payload);
                } catch (Throwable e) {
                    response = error(id, op, DebugProtocol.ERROR_INTERNAL,
                            DebugJson.messageOf(e), DebugJson.stackOf(e));
                }
                response.put("elapsedMs", System.currentTimeMillis() - started);
                send(response);
                running.remove(id);
            }
        });
        running.put(id, future);
    }

    private ObjectNode handle(String id, String op, JsonNode request) {
        try {
            if (DebugProtocol.OP_PING.equals(op)) {
                ObjectNode payload = DebugJson.object();
                payload.put("pong", true);
                return ok(id, op, payload);
            }
            if (DebugProtocol.OP_HELLO.equals(op)) {
                return hello(id);
            }
            if (DebugProtocol.OP_STATUS.equals(op)) {
                return status(id);
            }
            if (DebugProtocol.OP_CANCEL.equals(op)) {
                return cancel(id, request);
            }
            if (DebugProtocol.OP_INVOKE.equals(op)) {
                return invoke(id, request);
            }
            return error(id, op, DebugProtocol.ERROR_INTERNAL, "未知操作: " + op, null);
        } catch (DebugException e) {
            return error(id, op, e.getCode(), e.getMessage(), e.getStack());
        } catch (Throwable e) {
            return error(id, op, DebugProtocol.ERROR_INTERNAL,
                    DebugJson.messageOf(e), DebugJson.stackOf(e));
        }
    }

    private ObjectNode hello(String id) {
        ObjectNode payload = DebugJson.object();
        payload.put("launcherVersion", "1.0.0");
        payload.put("protocolVersion", DebugProtocol.VERSION);
        payload.put("containerState", container.state());
        payload.put("mainClass", container.mainClass());
        payload.put("beanPresent", container.beanPresent());
        payload.put("jdkVersion", System.getProperty("java.version"));
        return ok(id, DebugProtocol.OP_HELLO, payload);
    }

    private ObjectNode status(String id) {
        ObjectNode payload = DebugJson.object();
        payload.put("containerState", container.state());
        payload.put("beanPresent", container.beanPresent());
        payload.put("activeRequests", running.size());
        payload.put("lastError", container.failureMessage());
        return ok(id, DebugProtocol.OP_STATUS, payload);
    }

    private ObjectNode invoke(String id, JsonNode request) throws DebugException {
        if (!DebugProtocol.STATE_READY.equals(container.state())) {
            String code = DebugProtocol.STATE_FAILED.equals(container.state())
                    ? DebugProtocol.ERROR_CONTAINER_FAILED
                    : DebugProtocol.ERROR_CONTAINER_STARTING;
            String message = container.failureMessage() == null
                    ? "容器状态: " + container.state()
                    : container.failureMessage();
            return error(id, DebugProtocol.OP_INVOKE, code, message, container.failureStack());
        }
        return ok(id, DebugProtocol.OP_INVOKE, invoker.invoke(request));
    }

    private ObjectNode cancel(String id, JsonNode request) {
        String targetId = DebugJson.text(request, "targetId", "");
        Future<?> future = running.get(targetId);
        boolean cancelled = future != null && future.cancel(true);
        ObjectNode payload = DebugJson.object();
        payload.put("cancelled", cancelled);
        payload.put("alreadyFinished", future == null);
        return ok(id, DebugProtocol.OP_CANCEL, payload);
    }

    private void handleShutdown(String id) {
        ObjectNode response = ok(id, DebugProtocol.OP_SHUTDOWN, DebugJson.object());
        response.put("elapsedMs", 0L);
        send(response);
        container.stop();
        workers.shutdownNow();
        close();
        // 给 socket 一点时间把最后一帧刷出去，再结束进程
        Thread exit = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(150L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                System.exit(0);
            }
        }, "lucky-debug-exit");
        exit.setDaemon(false);
        exit.start();
    }

    private void send(ObjectNode response) {
        BufferedWriter out = writer;
        if (out == null) {
            return;
        }
        synchronized (this) {
            try {
                out.write(DebugJson.MAPPER.writeValueAsString(response));
                out.write('\n');
                out.flush();
            } catch (IOException ignored) {
                // 对端已断开，响应直接丢弃
            }
        }
    }

    private ObjectNode ok(String id, String op, JsonNode payload) {
        ObjectNode response = base(id, op);
        response.put("ok", true);
        response.set("payload", payload);
        return response;
    }

    private ObjectNode error(String id, String op, String code, String message, String stack) {
        ObjectNode response = base(id, op);
        response.put("ok", false);
        response.put("errorCode", code == null ? DebugProtocol.ERROR_INTERNAL : code);
        response.put("errorMessage", message == null ? "" : message);
        if (stack != null) {
            response.put("stackTrace", stack);
        }
        response.set("payload", DebugJson.object());
        return response;
    }

    private ObjectNode base(String id, String op) {
        ObjectNode response = DebugJson.object();
        response.put("id", id);
        response.put("op", op);
        response.put("protocolVersion", DebugProtocol.VERSION);
        return response;
    }
}
