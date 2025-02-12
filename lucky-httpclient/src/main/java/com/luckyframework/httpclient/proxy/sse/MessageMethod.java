package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Message方法包装类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/2/12 20:30
 */
public class MessageMethod {

    /**
     * {@link OnMessage}注解实例
     */
    private final OnMessage onMessage;

    /**
     * 方法实例
     */
    private final Method method;

    /**
     * 获取某个类中所有的消息方法
     *
     * @param clazz 目标类
     * @return 该类中所有的消息方法
     */
    public static List<MessageMethod> findMessageMethods(Class<?> clazz) {
        List<MessageMethod> messageMethods = new ArrayList<>();
        for (Method method : ASMUtil.getAllMethodOrder(clazz)) {
            OnMessage onMessageAnn = AnnotationUtils.findMergedAnnotation(method, OnMessage.class);
            if (onMessageAnn != null) {
                messageMethods.add(new MessageMethod(onMessageAnn, method));
            }
        }
        return messageMethods;
    }

    /**
     * 全参构造器
     *
     * @param onMessage {@link OnMessage}注解实例
     * @param method    方法实例
     */
    public MessageMethod(OnMessage onMessage, Method method) {
        this.onMessage = onMessage;
        this.method = method;
    }

    /**
     * 获取{@link OnMessage}注解实例
     *
     * @return {@link OnMessage}注解实例
     */
    public OnMessage getOnMessage() {
        return onMessage;
    }

    /**
     * 获取方法实例
     *
     * @return 方法实例
     */
    public Method getMethod() {
        return method;
    }
}
