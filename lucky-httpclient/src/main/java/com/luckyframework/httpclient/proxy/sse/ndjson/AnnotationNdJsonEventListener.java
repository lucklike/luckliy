package com.luckyframework.httpclient.proxy.sse.ndjson;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.ClassStaticElement;
import com.luckyframework.httpclient.proxy.spel.MutableMapParamWrapper;
import com.luckyframework.httpclient.proxy.spel.ParamWrapperSetter;
import com.luckyframework.httpclient.proxy.spel.ParameterInstanceGetter;
import com.luckyframework.httpclient.proxy.sse.Event;
import com.luckyframework.httpclient.proxy.sse.OnMessage;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.AnnotationUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于application/x-ndjson格式的事件监听器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/2/12 00:38
 * T 消息泛型
 */
public abstract class AnnotationNdJsonEventListener<T> extends NdJsonEventListener<T> {

    /**
     * 对象类型的消息内容
     */
    private static final String $_DATA_$ = "$data$";

    /**
     * 用于获取当前对象的KEY
     */
    private static final String $_THIS_$ = "$this$";

    /**
     * Message方法集合
     */
    private final List<MessageMethod> messageMethods = new ArrayList<>();

    /**
     * 无参构造器
     * 初始化时收集所有的Message方法
     */
    public AnnotationNdJsonEventListener() {
        for (Method method : ASMUtil.getAllMethodOrder(getClass())) {
            OnMessage onMessageAnn = AnnotationUtils.findMergedAnnotation(method, OnMessage.class);
            if (onMessageAnn != null) {
                messageMethods.add(new MessageMethod(onMessageAnn, method));
            }
        }
    }


    /**
     * 执行所有条件符合的Message方法
     *
     * @param event 消息事件
     * @throws Exception 执行过程可能出现异常
     */
    @Override
    public void onMessage(Event<T> event) throws Exception {
        MethodContext context = event.getContext();
        ParamWrapperSetter setter = getParamWrapperSetter(event);
        for (MessageMethod mm : messageMethods) {
            OnMessage onMessageAnn = mm.getOnMessage();
            if (context.parseExpression(onMessageAnn.value(), boolean.class, setter)) {
                context.invokeMethod(this, mm.getMethod(), setter, getParameterInstanceGetter(mm, event));
                break;
            }
        }
    }

    /**
     * 构造参数实例获取器
     *
     * @param msgMethod 消息方法
     * @param event     消息事件
     * @return 参数实例获取器
     */
    private ParameterInstanceGetter getParameterInstanceGetter(MessageMethod msgMethod, Event<T> event) {
        return parameter -> {
            Class<?> parameterType = parameter.getType();
            if (Event.class.isAssignableFrom(parameterType)) {
                return event;
            }
            if (Context.class.isAssignableFrom(parameterType)) {
                return event.getContext();
            }
            if (getMessageType() == parameterType) {
                return event.getMessage();
            }
            if (MessageMethod.class.isAssignableFrom(parameterType)) {
                return msgMethod;
            }
            if (getClass() == parameterType || NdJsonEventListener.class == parameterType || AnnotationNdJsonEventListener.class == parameterType) {
                return this;
            }
            return getParameterInstance(parameter, msgMethod, event);
        };
    }

    /**
     * 构造SpEL参数设置器
     *
     * @param event 消息事件
     * @return SpEL参数设置器
     */
    private ParamWrapperSetter getParamWrapperSetter(Event<T> event) {
        T data = event.getMessage();
        return pw -> {
            Map<String, Object> varMap = new HashMap<>();
            varMap.put($_DATA_$, data);
            varMap.put($_THIS_$, this);

            pw.importPackage(getClass().getPackage().getName());
            pw.getVariables().addFirst(ClassStaticElement.create(getClass()).getAllStaticMethods());
            pw.getRootObject().addFirst(varMap);
            addMessageSpELVar(pw, event);
        };
    }

    /**
     * 扩展接口
     * 添加自定义的消息变量到SpEL上下文中
     *
     * @param paramWrapper 现有的SpEL变量Map
     * @param event        消息事件
     */
    protected void addMessageSpELVar(MutableMapParamWrapper paramWrapper, Event<T> event) {

    }

    /**
     * 扩展接口
     * 用于扩展自定义的参数获取逻辑
     *
     * @param parameter 参数实例
     * @param msgMethod 消息方法
     * @param event     消息事件
     * @return 参数对应的实例对象
     */
    protected Object getParameterInstance(Parameter parameter, MessageMethod msgMethod, Event<T> event) {
        return null;
    }


    /**
     * Message方法包装类
     */
    public static class MessageMethod {
        /**
         * {@link OnMessage}注解实例
         */
        private final OnMessage onMessage;

        /**
         * 方法实例
         */
        private final Method method;

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
}
