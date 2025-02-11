package com.luckyframework.httpclient.proxy.stream.sse;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationTypeToken;

import java.lang.reflect.Type;
import java.util.Properties;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.XML_SCHEME;
import static com.luckyframework.httpclient.proxy.stream.sse.SseConstant.COMMENT;
import static com.luckyframework.httpclient.proxy.stream.sse.SseConstant.DATA;
import static com.luckyframework.httpclient.proxy.stream.sse.SseConstant.EVENT;
import static com.luckyframework.httpclient.proxy.stream.sse.SseConstant.ID;
import static com.luckyframework.httpclient.proxy.stream.sse.SseConstant.RETRY;

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
     * 消息类型
     */
    private final String event;

    /**
     * 消息数据
     */
    private final String data;

    /**
     * 重试相关的设置
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

    /**
     * 获取消息ID
     *
     * @return 消息ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取消息类型
     *
     * @return 消息类型
     */
    public String getEvent() {
        return event;
    }

    /**
     * 获取消息数据
     *
     * @return 消息数据
     */
    public String getData() {
        return data;
    }

    /**
     * 获取重试信息
     *
     * @return 重试信息
     */
    public String getRetry() {
        return retry;
    }

    /**
     * 获取注释说明
     *
     * @return 注释说明
     */
    public String getComment() {
        return comment;
    }

    /**
     * 获取所有消息数据
     *
     * @return 所有消息数据
     */
    public Properties getMsgProperties() {
        return msgProperties;
    }

    /**
     * 根据指定的key或者对应的消息
     *
     * @param key Key名称
     * @return 对应的消息数据
     */
    public String getProperty(String key) {
        return msgProperties.getProperty(key);
    }

    /**
     * 根据指定的key或者对应的消息，如果获取不到则返回默认值
     *
     * @param key          Key名称
     * @param defaultValue 默认值
     * @return 对应的消息数据
     */
    public String getPropertyOrDefault(String key, String defaultValue) {
        return hasProperty(key) ? getProperty(key) : defaultValue;
    }

    /**
     * 判断名称为Key的消息是否存在
     *
     * @param key Key名称
     * @return 名称为Key的消息是否存在
     */
    public boolean hasProperty(String key) {
        return msgProperties.containsKey(key);
    }

    /**
     * 是否存在消息ID
     *
     * @return 是否存在消息ID
     */
    public boolean hasId() {
        return hasProperty(ID);
    }

    /**
     * 是否存在消息类型
     *
     * @return 是否存在消息类型
     */
    public boolean hasEvent() {
        return hasProperty(EVENT);
    }

    /**
     * 是否存在消息数据
     *
     * @return 是否存在消息数据
     */
    public boolean hasData() {
        return hasProperty(DATA);
    }

    /**
     * 是否存在重试相关信息
     *
     * @return 是否存在重试相关信息
     */
    public boolean hasRetry() {
        return hasProperty(RETRY);
    }

    /**
     * 是否存在消息的注释信息
     *
     * @return 是否存在消息的注释信息
     */
    public boolean hasComment() {
        return hasProperty(COMMENT);
    }

    /**
     * 将JSON格式的消息数据转化为Java对象
     *
     * @param objectType Java对象类型
     * @param <T>        Java对象类型
     * @return Java对象
     */
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

    /**
     * 将JSON格式的消息数据转化为复杂的Java对象
     *
     * @param token Java对象类型token
     * @param <T>   Java对象类型
     * @return Java对象
     */
    public <T> T jsonDataToEntity(SerializationTypeToken<T> token) {
        return jsonDataToEntity(token.getType());
    }

    /**
     * 将JSON格式的消息数据转化为{@link ConfigurationMap}对象
     * @return ConfigurationMap对象
     */
    public ConfigurationMap jsonDataToMap() {
        return jsonDataToEntity(ConfigurationMap.class);
    }

    /**
     * 将XML格式的消息数据转化为Java对象
     *
     * @param objectType Java对象类型
     * @param <T>        Java对象类型
     * @return Java对象
     */
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

    /**
     * 将XML格式的消息数据转化为复杂的Java对象
     *
     * @param token Java对象类型token
     * @param <T>   Java对象类型
     * @return Java对象
     */
    public <T> T xmlDataToEntity(SerializationTypeToken<T> token) {
        return xmlDataToEntity(token.getType());
    }

    /**
     * 将JDK序列化后的消息数据转化为Java对象
     *
     * @param objectType Java对象类型
     * @param <T>        Java对象类型
     * @return Java对象
     */
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

    /**
     * 将JDK序列化后的消息数据转化为复杂Java对象
     *
     * @param objectType Java对象类型token
     * @param <T>        Java对象类型
     * @return Java对象
     */
    public <T> T javaDataToEntity(SerializationTypeToken<T> token) {
        return javaDataToEntity(token.getType());
    }

}
