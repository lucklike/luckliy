package com.luckyframework.httpclient.proxy.configapi;

/**
 * SSE响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/09 18:40
 */
public class SseConvert {

    private SseListenerConf listener = new SseListenerConf();

    public SseListenerConf getListener() {
        return listener;
    }

    public void setListener(SseListenerConf listener) {
        this.listener = listener;
    }
}
