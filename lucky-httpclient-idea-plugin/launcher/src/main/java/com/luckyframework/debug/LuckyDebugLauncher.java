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
        watchParent();
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

    /**
     * 父进程存活检测：stdin 管道被操作系统关闭时读到 EOF，直接结束进程。
     *
     * 插件进程被强制结束时不会执行 Disposer 收尾，launcher 可能还阻塞在
     * {@link DebugRpcServer#serve()} 的 accept() 上（握手阶段 RPC 尚未连接），
     * 仅靠 socket 断开无法退出；stdin EOF 在这个窗口同样可用。
     * 正常情况下父进程持有管道写端，read 会一直阻塞，不影响原有流程。
     */
    private static void watchParent() {
        Thread watcher = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[256];
                try {
                    while (System.in.read(buffer) >= 0) {
                        // 插件不向 stdin 写数据，读到内容直接丢弃
                    }
                } catch (IOException ignored) {
                    // 读失败与 EOF 等价：写端已经关闭
                }
                // 父进程已消失且没有日志观察者，由 System.exit 触发 Spring Boot
                // 已注册的 shutdown hook 关闭容器；启动早期没有 hook 时直接回收进程。
                System.exit(0);
            }
        }, "lucky-debug-parent-watch");
        watcher.setDaemon(true);
        watcher.start();
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
