package com.luckyframework.httpclient.proxy.sse.standard;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.sse.Event;
import com.luckyframework.httpclient.proxy.sse.EventListener;

import java.util.Properties;

/**
 * SSE标准数据格式的事件监听器（text/event-stream）
 */
public abstract class StandardEventListener implements EventListener {

    /**
     * 消息集合
     */
    private final ThreadLocal<Properties> props = new ThreadLocal<>();

    @Override
    public void onOpen(Event<Response> event) {
        props.set(new Properties());
    }

    @Override
    public void onClose(Event<Void> event) {
        props.remove();
    }

    /**
     * 接收到服务器的消息时触发
     *
     * @param event 消息事件
     */
    @Override
    public final void onText(Event<String> event) throws Exception {
        MethodContext context = event.getContext();
        String line = event.getMessage();
        Properties properties = props.get();

        // 消息处理，遇到空行之前收集消息，遇到空行时处理消息
        if (!StringUtils.hasText(line)) {
            onMessage(new Event<>(context, new Message(properties)));
            props.set(new Properties());
        } else {
            int index = line.indexOf(":");
            if (index != -1) {
                properties.put(line.substring(0, index), line.substring(index + 1));
            }
        }
    }


    /**
     * 接收到服务器的消息时触发
     *
     * @param event 消息事件
     * @throws Exception 消息处理过程中可能出现的异常
     */
    protected void onMessage(Event<Message> event) throws Exception {

    }

}
