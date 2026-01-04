package com.luckyframework.httpclient.proxy.sse.ndjson;

import com.luckyframework.httpclient.proxy.sse.ReconnectionEventListener;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * 基于application/x-ndjson格式的事件监听器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/2/12 00:38
 * T 消息泛型
 */
public abstract class NdJsonEventListener<T> extends ReconnectionEventListener {


    @Override
    @SuppressWarnings("unchecked")
    public final void onText(String message) throws Exception {
        onMessage((T) JSON_SCHEME.deserialization(message, getMessageType()));
    }

    /**
     * 获取消息类型
     *
     * @return 消息类型
     */
    protected Type getMessageType() {
        return ResolvableType.forClass(NdJsonEventListener.class, getClass()).getGeneric(0).getType();
    }

    /**
     * 接收到服务器的消息时触发
     *
     * @param data 消息
     */
    protected abstract void onMessage(T data) throws Exception;
}
