package com.luckyframework.httpclient.proxy.sse;

/**
 * SSE事件监听器
 */
public interface EventListener {

    /**
     * 接收到服务器的消息时触发
     *
     * @param event 服务器发送的消息
     */
    default void onMessage(Event<Message> event) {

    }

    /**
     * 当发生错误时触发
     *
     * @param event 异常
     */
    default void onError(Event<Throwable> event) {
        throw new SseException(event.getMessage());
    }

    /**
     * 当连接关闭时触发
     */
    default void onClose(Event<Void> event) {

    }
}
