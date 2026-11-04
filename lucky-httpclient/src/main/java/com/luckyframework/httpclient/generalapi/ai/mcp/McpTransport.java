package com.luckyframework.httpclient.generalapi.ai.mcp;

import java.io.Closeable;
import java.util.Map;

/**
 * MCP消息的收发通道。两种实现：{@link StdioTransport}（拉起子进程走标准输入输出）与
 * {@link StreamableHttpTransport}（POST到配置的HTTP地址）。
 * <pre>
 *     契约是「一帧一个完整JSON对象」：
 *     1. {@link #send} 把一帧文本发给对端，不保证同步拿到响应；
 *     2. 对端发来的消息（响应与通知都算）一律通过 {@link Listener} 交付，
 *        实现可以在任意线程交付（stdio是实现自己的后台读线程，HTTP是发送线程）；
 *     3. 请求-响应配对、超时与通知容忍都由 {@link JsonRpcChannel} 负责，Transport只管字节。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 */
public interface McpTransport extends Closeable {

    /**
     * 一帧完整JSON对象到达时的回调
     */
    interface Listener {

        /**
         * 收到一帧已解析的JSON对象
         *
         * @param frame 帧内容
         */
        void onMessage(Map<String, Object> frame);

        /**
         * 连接断开（子进程退出、HTTP通道不可用等），此后不会再有消息到达
         *
         * @param cause 断开原因，正常关闭时为null
         */
        void onDisconnect(Throwable cause);
    }

    /**
     * 开始接收对端消息，实现应保证多次调用只生效一次
     *
     * @param listener 消息回调
     * @throws McpException 通道建立失败时抛出
     */
    void open(Listener listener);

    /**
     * 发送一帧JSON文本（不带换行，由实现决定帧分隔方式）
     *
     * @param jsonFrame 完整的一个JSON对象
     * @throws McpException 发送失败（连接已断开）时抛出
     */
    void send(String jsonFrame);

    /**
     * 通道当前是否可用
     *
     * @return 已打开且未断开时为true
     */
    boolean isOpen();

    /**
     * 关闭通道并回收资源，重复调用无副作用
     */
    @Override
    void close();
}
