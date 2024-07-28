package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.common.StringUtils;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationTypeToken;

import java.lang.reflect.Type;
import java.util.Properties;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.*;
import static com.luckyframework.httpclient.proxy.sse.SseConstant.*;

/**
 * SSE消息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 02:46
 */
@SuppressWarnings("all")
public class Message {

    /**
     * 消息ID
     */
    private final String id;

    /**
     * 事件类型
     */
    private final String event;

    /**
     * 消息数据
     */
    private final String data;

    /**
     * 重试相关的额设置
     */
    private final String retry;

    /**
     * 注释
     */
    private final String comment;

    /**
     * 消息属性
     */
    private final Properties msgProperties;

    public Message(Properties msgProperties) {
        this.msgProperties = msgProperties;
        this.id = msgProperties.getProperty(ID);
        this.event = msgProperties.getProperty(EVENT);
        this.data = msgProperties.getProperty(DATA);
        this.retry = msgProperties.getProperty(RETRY);
        this.comment = msgProperties.getProperty(COMMENT);
    }

    public String getId() {
        return id;
    }

    public String getEvent() {
        return event;
    }

    public String getData() {
        return data;
    }

    public String getRetry() {
        return retry;
    }

    public String getComment() {
        return comment;
    }

    public Properties getMsgProperties() {
        return msgProperties;
    }

    public String getProperty(String key) {
        return msgProperties.getProperty(key);
    }

    public String getPropertyOrDefault(String key, String defaultValue) {
        return hasProperty(key) ? getProperty(key) : defaultValue;
    }

    public boolean hasProperty(String key) {
        return msgProperties.containsKey(key);
    }

    public boolean hasId() {
        return hasProperty(ID);
    }

    public boolean hasEvent() {
        return hasProperty(EVENT);
    }

    public boolean hasData() {
        return hasProperty(DATA);
    }

    public boolean hasRetry() {
        return hasProperty(RETRY);
    }

    public boolean hasComment() {
        return hasProperty(COMMENT);
    }

    public <T> T jsonDataToEntity(Type objectType) {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        try {
            return (T) JSON_SCHEME.deserialization(data, objectType);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    public <T> T jsonDataToEntity(SerializationTypeToken<T> token) {
        return jsonDataToEntity(token.getType());
    }

    public <T> T xmlDataToEntity(Type objectType) {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        try {
            return (T) XML_SCHEME.deserialization(data, objectType);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    public <T> T xmlDataToEntity(SerializationTypeToken<T> token) {
        return xmlDataToEntity(token.getType());
    }

    public <T> T javaDataToEntity(Type objectType) {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        try {
            return (T) JDK_SCHEME.deserialization(data, objectType);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    public <T> T javaDataToEntity(SerializationTypeToken<T> token) {
        return javaDataToEntity(token.getType());
    }

}
