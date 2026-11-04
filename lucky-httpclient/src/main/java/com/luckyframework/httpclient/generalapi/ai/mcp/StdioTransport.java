package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.serializable.SerializationTypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * stdio传输：按配置的命令拉起子进程，以「换行分隔的JSON帧」通过标准输入输出通信。
 * <pre>
 *     一帧一个完整JSON对象，不依赖空白缩进；读循环放在后台守护线程里，
 *     收到的每一帧都交付给 {@link McpTransport.Listener}（响应与通知都走同一条流）。
 *     {@link #close()} 幂等，并以 {@code destroyForcibly()} 兜底，保证不残留已拉起的进程。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class StdioTransport implements McpTransport {

    private final List<String> commandLine;
    private final Map<String, String> environment;

    private volatile Listener listener;
    private volatile Process process;
    private volatile BufferedWriter writer;
    private volatile boolean closed;
    private volatile Throwable disconnectCause;
    private Thread readerThread;

    /**
     * @param commandLine 命令与参数（第一个元素是可执行程序），来自配置文件或编程式配置
     * @param environment 需要附加给子进程的环境变量，可为null
     */
    public StdioTransport(List<String> commandLine, Map<String, String> environment) {
        if (commandLine == null || commandLine.isEmpty()) {
            throw new McpException("The MCP stdio command is not configured（命令与参数必须来自配置）.");
        }
        this.commandLine = new ArrayList<>(commandLine);
        this.environment = environment;
    }

    public StdioTransport(List<String> commandLine) {
        this(commandLine, null);
    }

    @Override
    public void open(Listener listener) {
        if (this.listener != null) {
            return;
        }
        this.listener = listener;
        try {
            ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.redirectErrorStream(false);
            if (environment != null && !environment.isEmpty()) {
                builder.environment().putAll(environment);
            }
            this.process = builder.start();
            this.writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            this.readerThread = new Thread(this::readLoop, "mcp-stdio-reader");
            this.readerThread.setDaemon(true);
            this.readerThread.start();
        } catch (IOException e) {
            close();
            throw new McpException(e, "Failed to launch the MCP stdio server: " + commandLine);
        }
    }

    /**
     * 换行分隔的一帧JSON对象，写出后立即flush
     */
    @Override
    public void send(String jsonFrame) {
        if (closed || process == null) {
            throw McpException.disconnected(new IllegalStateException("the stdio transport is closed"));
        }
        try {
            writer.write(jsonFrame);
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
            close();
            throw McpException.disconnected(e);
        }
    }

    @Override
    public boolean isOpen() {
        return !closed && process != null && process.isAlive();
    }

    /**
     * 幂等关闭：先温和地结束输入流，再强制回收子进程，任何情况下都不留残留进程
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeQuietly(writer);
        Process current = this.process;
        if (current != null) {
            current.destroy();
            try {
                if (!current.waitFor(200, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    current.destroyForcibly();
                    current.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                current.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            closeQuietly(current.getInputStream());
            closeQuietly(current.getOutputStream());
            closeQuietly(current.getErrorStream());
        }
        Thread reader = this.readerThread;
        if (reader != null) {
            reader.interrupt();
        }
    }

    /**
     * 读循环：逐行读取子进程的标准输出，每行是一个完整JSON对象
     */
    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (closed) {
                    return;
                }
                String text = line.trim();
                // 容忍非JSON的噪声行（例如server把日志写到了stdout），空行与非对象行直接跳过
                if (text.isEmpty() || text.charAt(0) != '{') {
                    continue;
                }
                Map<String, Object> frame = AiJsonUtils.fromJson(text,
                        new SerializationTypeToken<Map<String, Object>>() {
                        });
                if (frame != null) {
                    listener.onMessage(frame);
                }
            }
        } catch (IOException e) {
            disconnectCause = e;
        } catch (RuntimeException e) {
            disconnectCause = e;
        }
        if (!closed) {
            listener.onDisconnect(disconnectCause == null
                    ? new IllegalStateException("the MCP stdio server process exited") : disconnectCause);
        }
    }

    /**
     * 子进程句柄，仅供测试断言「进程已被回收」
     */
    Process getProcess() {
        return process;
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
