package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.mcp.McpTransport;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 内存假MCP通道：把发出的帧记录下来，由测试决定何时、以什么顺序回什么帧，
 * 用于离线断言JSON-RPC的请求-响应配对与通知容忍。
 *
 * @author fukang
 * @version 3.0.3
 */
public class InMemoryMcpTransport implements McpTransport {

    /**
     * 已经发出的每一帧JSON文本
     */
    private final List<String> sentFrames = Collections.synchronizedList(new ArrayList<String>());

    private Listener listener;
    private boolean open;
    private boolean closed;

    @Override
    public void open(Listener listener) {
        if (open) {
            return;
        }
        this.listener = listener;
        this.open = true;
    }

    @Override
    public void send(String jsonFrame) {
        if (closed) {
            throw new IllegalStateException("the transport has been closed");
        }
        sentFrames.add(jsonFrame);
    }

    @Override
    public boolean isOpen() {
        return open && !closed;
    }

    @Override
    public void close() {
        closed = true;
    }

    /**
     * 模拟server回一帧（乱序、通知、错误响应都由测试自己构造）
     */
    public void deliver(String jsonFrame) {
        listener.onMessage(AiJsonUtils.fromJson(jsonFrame, new SerializationTypeToken<Map<String, Object>>() {
        }));
    }

    /**
     * 模拟server断开
     */
    public void disconnect(Throwable cause) {
        open = false;
        listener.onDisconnect(cause);
    }

    /**
     * 已发出的帧，按发送顺序
     */
    public List<String> sentFrames() {
        return new ArrayList<>(sentFrames);
    }

    /**
     * 第index帧（从0开始）解析出的JSON对象
     */
    public Map<String, Object> sentFrame(int index) {
        return AiJsonUtils.fromJson(sentFrames.get(index), new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    /**
     * 已发出的帧数
     */
    public int sentCount() {
        return sentFrames.size();
    }
}
