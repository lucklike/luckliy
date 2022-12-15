package com.luckyframework.context;

import com.luckyframework.context.event.ApplicationEvent;
import com.luckyframework.context.event.ApplicationListener;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/18 下午4:49
 */
public final class EventListenerMethodApplicationListener implements ApplicationListener, Ordered {

    private final Object bean;
    private final Method eventListenerMethod;
    private final int methodParamLength;
    private final int order;


    EventListenerMethodApplicationListener(Object bean, Method eventListenerMethod,Integer order) {
        this.bean = bean;
        this.eventListenerMethod = eventListenerMethod;
        this.methodParamLength = eventListenerMethod.getParameterCount();
        this.order = order;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        Object[] invokeParam;
        if(methodParamLength == 1){
            invokeParam = new Object[1];
            invokeParam[0] = event;
        }else{
            invokeParam = new Object[0];
        }
        MethodUtils.invoke(bean,eventListenerMethod,invokeParam);
    }

    @Override
    public int getOrder() {
        return order;
    }

    public String getMethodName(){
        StringBuilder methodName = new StringBuilder();
        methodName.append(this.eventListenerMethod.getDeclaringClass().getName())
                  .append("#")
                  .append(this.eventListenerMethod.getName()).append("(");
        Type[] genericParameterTypes = this.eventListenerMethod.getGenericParameterTypes();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            methodName.append(ResolvableType.forType(genericParameterTypes[i]));
            if(i==genericParameterTypes.length-1){
                methodName.append(")");
            }else{
                methodName.append(",");
            }
        }
        return methodName.toString();
    }
}
