package com.luckyframework.httpclient.proxy.sse.standard;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.sse.ReconnectionEventListener;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE标准数据格式的事件监听器（text/event-stream）
 */
public abstract class StandardEventListener extends ReconnectionEventListener {

    /**
     * 消息集合
     */
    private final Map<MethodContext, Properties> props = new ConcurrentHashMap<>();

    @Override
    public final void onOpen(Response response) throws Exception {
        props.put(getContext(), new Properties());
        onOpening(response);
    }

    @Override
    public final void onClose() {
        props.remove(getContext());
        onClosed();
    }

    /**
     * 接收到服务器的消息时触发
     *
     * @param message 消息事件
     */
    @Override
    public final void onText(String message) throws Exception {
        Properties properties = props.get(getContext());
        // 消息处理，遇到空行之前收集消息，遇到空行时处理消息
        if (!StringUtils.hasText(message)) {
            onMessage(new Message(properties));
            props.put(getContext(), new Properties());
        } else {
            int index = message.indexOf(":");
            if (index != -1) {
                properties.put(message.substring(0, index), message.substring(index + 1));
            }
        }
    }


    /**
     * 接收到服务器的消息时触发
     *
     * @param message 消息
     * @throws Exception 消息处理过程中可能出现的异常
     */
    protected void onMessage(Message message) throws Exception {

    }

    /**
     * 当连接建立时触发
     *
     * @param response 响应对象
     */
    protected void onOpening(Response response) throws Exception {

    }

    /**
     * 当连接关闭时触发
     */
    protected void onClosed() {

    }

}
