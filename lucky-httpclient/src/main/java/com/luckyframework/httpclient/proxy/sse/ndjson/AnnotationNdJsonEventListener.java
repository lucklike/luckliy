package com.luckyframework.httpclient.proxy.sse.ndjson;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.ClassStaticElement;
import com.luckyframework.httpclient.proxy.spel.MutableMapParamWrapper;
import com.luckyframework.httpclient.proxy.spel.ParamWrapperSetter;
import com.luckyframework.httpclient.proxy.spel.ParameterInfo;
import com.luckyframework.httpclient.proxy.spel.ParameterInstanceGetter;
import com.luckyframework.httpclient.proxy.sse.MessageMethod;
import com.luckyframework.httpclient.proxy.sse.OnMessage;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
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
    private final List<MessageMethod> messageMethods;

    /**
     * 无参构造器
     * 初始化时收集所有的Message方法
     */
    public AnnotationNdJsonEventListener() {
        this.messageMethods = MessageMethod.findMessageMethods(getClass());
    }


    /**
     * 执行所有条件符合的Message方法
     *
     * @param data 消息数据
     * @throws Exception 执行过程可能出现异常
     */
    @Override
    public void onMessage(T data) throws Exception {
        MethodContext context = getContext();
        ParamWrapperSetter setter = getParamWrapperSetter(data);
        for (MessageMethod mm : messageMethods) {
            OnMessage onMessageAnn = mm.getOnMessage();
            if (context.parseExpression(onMessageAnn.value(), boolean.class, setter)) {
                context.autoInjectParamExecuteMethod(this, mm.getMethod(), setter, getParameterInstanceGetter(mm, data));
                break;
            }
        }
    }

    /**
     * 构造参数实例获取器
     *
     * @param msgMethod 消息方法
     * @param data      消息数据
     * @return 参数实例获取器
     */
    private ParameterInstanceGetter getParameterInstanceGetter(MessageMethod msgMethod, T data) {
        return parameter -> {
            Class<?> parameterType = parameter.getTargetClass();
            if (Context.class.isAssignableFrom(parameterType)) {
                return getContext();
            }
            if (getMessageType() == parameterType) {
                return data;
            }
            if (MessageMethod.class.isAssignableFrom(parameterType)) {
                return msgMethod;
            }
            if (getClass() == parameterType || NdJsonEventListener.class == parameterType || AnnotationNdJsonEventListener.class == parameterType) {
                return this;
            }
            return getParameterInstance(parameter, msgMethod, data);
        };
    }

    /**
     * 构造SpEL参数设置器
     *
     * @param data 消息数据
     * @return SpEL参数设置器
     */
    private ParamWrapperSetter getParamWrapperSetter(T data) {
        return pw -> {
            Map<String, Object> varMap = new HashMap<>();
            varMap.put($_DATA_$, data);
            varMap.put($_THIS_$, this);

            pw.importPackage(getClass().getPackage().getName());
            pw.getVariables().addFirst(ClassStaticElement.create(getClass()).getAllStaticMethods());
            pw.getRootObject().addFirst(varMap);
            addMessageSpELVar(pw, data);
        };
    }

    /**
     * 扩展接口
     * 添加自定义的消息变量到SpEL上下文中
     *
     * @param paramWrapper 现有的SpEL变量Map
     * @param data         消息数据
     */
    protected void addMessageSpELVar(MutableMapParamWrapper paramWrapper, T data) {

    }

    /**
     * 扩展接口
     * 用于扩展自定义的参数获取逻辑
     *
     * @param parameterInfo 参数信息
     * @param msgMethod     消息方法
     * @param data          消息数据
     * @return 参数对应的实例对象
     */
    protected Object getParameterInstance(ParameterInfo parameterInfo, MessageMethod msgMethod, T data) {
        return null;
    }

    @Override
    protected final Type getMessageType() {
        return ResolvableType.forClass(AnnotationNdJsonEventListener.class, getClass()).getGeneric(0).getType();
    }
}
