package com.luckyframework.httpclient.proxy.sse;

/**
 * SSE事件监听器
 */
public interface EventListener {

    /**
     * 当连接建立时触发
     *
     * @param event 连接建立事件
     */
    default void onOpen(Event<Void> event) throws Exception {

    }

    /**
     * 接收到服务器的消息时触发
     *
     * @param event 消息事件
     */
    default void onMessage(Event<Message> event) throws Exception {

    }

    /**
     * 当发生错误时触发
     *
     * @param event 异常事件
     */
    default void onError(Event<Throwable> event) {
        throw new SseException(event.getMessage());
    }

    /**
     * 当连接关闭时触发
     *
     * @param event 连接关闭事件
     */
    default void onClose(Event<Void> event) {

    }
}
