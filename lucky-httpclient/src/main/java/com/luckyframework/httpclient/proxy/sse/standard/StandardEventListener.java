package com.luckyframework.httpclient.proxy.sse.standard;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.sse.ReconnectionEventListener;
import com.luckyframework.httpclient.proxy.sse.SseException;

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
     * 正常结束时触发
     * <p>
     * SSE规范中一个事件以空行作为结束标志，但服务端可能在推送完最后一帧后
     * 未补发空行就直接关闭流（EOF）。此时{@link #onText(String)}不会派发缓冲区中
     * 残留的最后一帧，导致消息丢失，因此在流正常结束时主动flush一次。
     * <p>
     * 注意：网络异常中断会走{@link #onError(Throwable)}而不会到达此处，
     * 因此这里flush的一定是正常结束时的完整尾帧。
     */
    @Override
    public final void onCompleted() {
        Properties properties = props.get(getContext());
        // 缓冲区仍有残留数据，说明最后一帧没有以空行结束，需要在此补发
        if (properties != null && !properties.isEmpty()) {
            try {
                onMessage(new Message(properties));
            } catch (Exception e) {
                throw new SseException(e);
            } finally {
                props.put(getContext(), new Properties());
            }
        }
        onCompleting();
    }


    /**
     * 接收到服务器的消息时触发
     *
     * @param message 消息
     * @throws Exception 消息处理过程中可能出现的异常
     */
    protected abstract void onMessage(Message message) throws Exception;

    /**
     * 当连接建立时触发
     *
     * @param response 响应对象
     */
    protected void onOpening(Response response) throws Exception {

    }

    /**
     * 当流正常结束时触发（在残留尾帧flush之后）
     */
    protected void onCompleting() {

    }

    /**
     * 当连接关闭时触发
     */
    protected void onClosed() {

    }

}
