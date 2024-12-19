package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.ParamWrapperSetter;
import com.luckyframework.httpclient.proxy.spel.ParameterInstanceGetter;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.spel.LazyValue;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持使用注解来绑定特定事件的监听器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/19 00:31
 */
public class AnnotationEventListener implements EventListener {

    private static final String $_MSG_$ = "$msg$";
    private static final String $_DATA_$ = "$txtData$";
    private static final String $_PROPERTIES_$ = "$property$";
    private static final String $_JSON_DATA_$ = "$data$";

    /**
     * Message方法集合
     */
    private final List<MessageMethod> messageMethods = new ArrayList<>();

    /**
     * 无参构造器
     * 初始化时收集所有的Message方法
     */
    public AnnotationEventListener() {
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
     * @throws Exception 支持过程可能出现异常
     */
    @Override
    public void onMessage(Event<Message> event) throws Exception {
        MethodContext context = event.getContext();
        ParamWrapperSetter wrapperSetter = getParamWrapperSetter(event);
        ParameterInstanceGetter parameterInstanceGetter = getParameterInstanceGetter(event);
        for (MessageMethod mm : messageMethods) {
            OnMessage onMessageAnn = mm.getOnMessage();
            if (context.parseExpression(onMessageAnn.value(), boolean.class, wrapperSetter)) {
                Method method = mm.getMethod();
                Object[] methodArgs = context.getMethodParamObject(method, wrapperSetter, parameterInstanceGetter);
                if (ClassUtils.isStaticMethod(method)) {
                    MethodUtils.invoke(null, method, methodArgs);
                } else {
                    MethodUtils.invoke(this, method, methodArgs);
                }
                break;
            }
        }
    }

    /**
     * 构造参数实例获取器
     *
     * @param event 消息事件
     * @return 参数实例获取器
     */
    private ParameterInstanceGetter getParameterInstanceGetter(Event<Message> event) {
        return parameter -> {
            Class<?> parameterType = parameter.getType();
            if (Event.class.isAssignableFrom(parameterType)) {
                return event;
            }
            if (Message.class.isAssignableFrom(parameterType)) {
                return event.getMessage();
            }
            return parameterInstanceGetter(parameter, event);
        };
    }

    /**
     * 构造SpEL参数设置器
     *
     * @param event 消息事件
     * @return SpEL参数设置器
     */
    private ParamWrapperSetter getParamWrapperSetter(Event<Message> event) {
        Message message = event.getMessage();
        return pw -> {
            Map<String, Object> varMap = new HashMap<>();
            varMap.put($_MSG_$, message);
            varMap.put($_DATA_$, LazyValue.of(message::getData));
            varMap.put($_PROPERTIES_$, LazyValue.of(message::getMsgProperties));
            varMap.put($_JSON_DATA_$, LazyValue.of(() -> message.jsonDataToEntity(Object.class)));
            addMessageSpELVar(varMap, event);
            pw.getRootObject().addFirst(varMap);
        };
    }

    /**
     * 扩展接口
     * 添加自定义的消息变量到SpEL上下文中
     *
     * @param varMap 现有的SpEL变量Map
     * @param event  消息事件
     */
    protected void addMessageSpELVar(Map<String, Object> varMap, Event<Message> event) {

    }

    /**
     * 扩展接口
     * 用于扩展自定义的参数获取逻辑
     *
     * @param parameter 参数实例
     * @param event     消息事件
     * @return 参数对应的实例对象
     */
    protected Object parameterInstanceGetter(Parameter parameter, Event<Message> event) {
        return null;
    }


    /**
     * Message方法包装类
     */
    static class MessageMethod {
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
