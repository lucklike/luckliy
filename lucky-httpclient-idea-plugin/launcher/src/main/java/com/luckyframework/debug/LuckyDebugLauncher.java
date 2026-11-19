package com.luckyframework.debug;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * 独立调试容器的进程入口。
 *
 * 启动顺序：
 * 1. 绑定 127.0.0.1 随机端口，向 stdout 输出 {@link DebugProtocol#PORT_PREFIX}；
 * 2. 后台线程通过 SpringApplicationBuilder 启动用户容器；
 * 3. 主线程进入 RPC 循环。
 *
 * 容器启动失败不退出进程，而是把状态置为 FAILED，
 * 让插件能拉到失败原因与完整堆栈。
 */
public final class LuckyDebugLauncher {

    public static void main(String[] args) {
        configureUtf8Streams();
        String mainClass = System.getProperty(DebugProtocol.MAIN_CLASS_PROPERTY);
        if (mainClass == null || mainClass.trim().isEmpty()) {
            System.err.println("missing -D" + DebugProtocol.MAIN_CLASS_PROPERTY + "=<main class>");
            System.exit(2);
            return;
        }

        DebugContainer container = new DebugContainer();
        DebugRpcServer server = new DebugRpcServer(container);
        try {
            server.bind();
        } catch (IOException e) {
            System.err.println("failed to bind debug socket");
            e.printStackTrace();
            System.exit(3);
            return;
        }

        // 必须先于容器启动输出，插件依赖这一行完成连接
        System.out.println(DebugProtocol.PORT_PREFIX + server.port());
        System.out.flush();

        Thread starter = new Thread(new Runnable() {
            @Override
            public void run() {
                container.start(mainClass, args);
            }
        }, "lucky-debug-container");
        starter.setDaemon(true);
        starter.start();

        try {
            server.serve();
        } finally {
            server.close();
            container.stop();
        }
    }

    private LuckyDebugLauncher() {
    }

    /**
     * Java 8 在 Windows 上默认按 GBK 输出 System.out/err，
     * 插件侧固定按 UTF-8 读取，中文堆栈会变成乱码。这里显式统一编码。
     */
    private static void configureUtf8Streams() {
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {
            // UTF-8 是 JVM 必须支持的编码
        }
    }
}
