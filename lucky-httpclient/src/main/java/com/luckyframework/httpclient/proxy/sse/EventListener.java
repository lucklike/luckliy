package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.httpclient.core.meta.Response;

/**
 * SSE事件监听器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/2/11 22:51
 */
public interface EventListener {

    /**
     * 当连接建立时触发
     *
     * @param response 响应对象
     */
    default void onOpen(Response response) throws Throwable {

    }

    /**
     * 接收到服务器的消息时触发
     *
     * @param message 消息
     */
    default void onText(String message) throws Exception {

    }

    /**
     * 当发生错误时触发
     *
     * @param event 异常
     */
    default void onError(Throwable event) {
        throw new SseException(event.getMessage());
    }

    /**
     * 正常结束时触发
     *
     */
    default void onCompleted() {

    }

    /**
     * 当连接关闭时触发
     *
     */
    default void onClose() {

    }
}
