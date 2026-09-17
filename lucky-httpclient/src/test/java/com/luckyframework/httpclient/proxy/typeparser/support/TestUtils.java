package com.luckyframework.httpclient.proxy.typeparser.support;

import com.luckyframework.httpclient.core.meta.DefaultRequest;
import com.luckyframework.httpclient.core.meta.RequestMethod;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.function.BooleanSupplier;

/**
 * 测试工具类
 */
public final class TestUtils {

    private TestUtils() {
    }

    /**
     * 轮询等待条件成立，超时后抛出断言错误
     */
    public static void awaitTrue(String message, long timeoutMillis, BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            if (condition.getAsBoolean()) {
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new AssertionError("Condition not satisfied within " + timeoutMillis + "ms: " + message);
            }
            sleepQuietly(15);
        }
    }

    /**
     * 判断异常的原因链中是否包含指定类型的异常
     */
    public static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return false;
    }

    /**
     * 睡眠，被中断时直接抛出异常终止测试
     */
    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sleeping", e);
        }
    }

    /**
     * 获取一个当前未被占用的本地端口
     */
    public static int unusedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to find an unused port", e);
        }
    }

    /**
     * 构造一个指向测试服务器的GET请求
     */
    public static DefaultRequest request(TestHttpServer server, String path) {
        return new DefaultRequest(server.baseUrl(), path, RequestMethod.GET);
    }

    /**
     * 构造一个指向未监听端口的请求（用于模拟连接失败）
     */
    public static DefaultRequest badRequest() {
        DefaultRequest request = new DefaultRequest("http://127.0.0.1:" + unusedPort(), "/bad", RequestMethod.GET);
        request.setConnectTimeout(2000);
        request.setReadTimeout(2000);
        return request;
    }
}
