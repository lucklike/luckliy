package com.luckyframework.httpclient.proxy.sse.ndjson;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.sse.Event;
import com.luckyframework.httpclient.proxy.sse.EventListener;
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
public abstract class NdJsonEventListener<T> implements EventListener {


    @Override
    @SuppressWarnings("unchecked")
    public final void onText(Event<String> event) throws Exception {
        MethodContext context = event.getContext();
        String line = event.getMessage();

        Event<T> entityEvent = new Event<>(context, (T) JSON_SCHEME.deserialization(line, getMessageType()));
        onMessage(entityEvent);
    }


    /**
     * 接收到服务器的消息时触发
     *
     * @param event 消息事件
     */
    protected void onMessage(Event<T> event) throws Exception {

    }

    /**
     * 获取消息类型
     *
     * @return 消息类型
     */
    protected Type getMessageType() {
        return ResolvableType.forClass(NdJsonEventListener.class, getClass()).getGeneric(0).getType();
    }
}
