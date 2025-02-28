package com.luckyframework.httpclient.proxy.sse.standard;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.ClassStaticElement;
import com.luckyframework.httpclient.proxy.spel.MutableMapParamWrapper;
import com.luckyframework.httpclient.proxy.spel.ParamWrapperSetter;
import com.luckyframework.httpclient.proxy.spel.ParameterInstanceGetter;
import com.luckyframework.httpclient.proxy.sse.Event;
import com.luckyframework.httpclient.proxy.sse.MessageMethod;
import com.luckyframework.httpclient.proxy.sse.OnMessage;
import com.luckyframework.spel.LazyValue;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持使用注解来绑定特定事件的监听器（text/event-stream）
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/19 00:31
 */
public class AnnotationStandardEventListener extends StandardEventListener {

    /**
     * 用于获取整个消息体的KEY
     */
    private static final String $_MSG_$ = "$msg$";

    /**
     * 用于获取文本格式的消息体中的【data:】部分
     */
    private static final String $_DATA_$ = "$data$";

    /**
     * 消息体中所有的消息集合
     */
    private static final String $_MSG_MAP_$ = "$msgMap$";

    /**
     * JSON格式的data数据转化为对象之后的数据
     */
    private static final String $_JSON_DATA_$ = "$jdata$";

    /**
     * 用于获取文本格式的消息体中的【id:】部分
     */
    private static final String $_ID_$ = "$id$";

    /**
     * 用于获取文本格式的消息体中的【event:】部分
     */
    private static final String $_EVENT_$ = "$event$";

    /**
     * 用于获取文本格式的消息体中的【retry:】部分
     */
    private static final String $_RETRY_$ = "$retry$";

    /**
     * 用于获取文本格式的消息体中的注释部分
     */
    private static final String $_COMMENT_$ = "$comment$";

    /**
     * 用于获取当前对象的KEY
     */
    private static final String $_THIS_$ = "$this$";

    /**
     * Message方法集合
     */
    private final List<MessageMethod> messageMethods;

    /**
     * 无参构造器
     * 初始化时收集所有的Message方法
     */
    public AnnotationStandardEventListener() {
        this.messageMethods = MessageMethod.findMessageMethods(getClass());
    }


    /**
     * 执行所有条件符合的Message方法
     *
     * @param event 消息事件
     * @throws Exception 执行过程可能出现异常
     */
    @Override
    public void onMessage(Event<Message> event) throws Exception {
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
    private ParameterInstanceGetter getParameterInstanceGetter(MessageMethod msgMethod, Event<Message> event) {
        return parameter -> {
            Class<?> parameterType = parameter.getType();
            if (Event.class.isAssignableFrom(parameterType)) {
                return event;
            }
            if (Context.class.isAssignableFrom(parameterType)) {
                return event.getContext();
            }
            if (Message.class.isAssignableFrom(parameterType)) {
                return event.getMessage();
            }
            if (MessageMethod.class.isAssignableFrom(parameterType)) {
                return msgMethod;
            }
            if (getClass() == parameterType || AnnotationStandardEventListener.class == parameterType || StandardEventListener.class == parameterType) {
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
    private ParamWrapperSetter getParamWrapperSetter(Event<Message> event) {
        Message message = event.getMessage();
        return pw -> {
            Map<String, Object> varMap = new HashMap<>();
            varMap.put($_MSG_$, message);
            varMap.put($_THIS_$, this);
            varMap.put($_DATA_$, LazyValue.of(message::getData));
            varMap.put($_ID_$, LazyValue.of(message::getId));
            varMap.put($_EVENT_$, LazyValue.of(message::getEvent));
            varMap.put($_RETRY_$, LazyValue.of(message::getRetry));
            varMap.put($_COMMENT_$, LazyValue.of(message::getComment));
            varMap.put($_MSG_MAP_$, LazyValue.of(message::getMsgProperties));
            varMap.put($_JSON_DATA_$, LazyValue.of(() -> message.fromJsonData(Object.class)));

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
    protected void addMessageSpELVar(MutableMapParamWrapper paramWrapper, Event<Message> event) {

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
    protected Object getParameterInstance(Parameter parameter, MessageMethod msgMethod, Event<Message> event) {
        return null;
    }

}
