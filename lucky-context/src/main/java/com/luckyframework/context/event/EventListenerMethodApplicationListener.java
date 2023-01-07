package com.luckyframework.context.event;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.expression.SpELRuntimeBeanFactoryPostProcessor.SPEL_RUNTIME_BEAN;

/**
 * 使用EventListener注解方法构造的事件监听器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/18 下午4:49
 */
@SuppressWarnings("all")
public final class EventListenerMethodApplicationListener implements ApplicationListener, Ordered {

    private final SpELRuntime spELRuntime;
    private final Object bean;
    private final Method eventListenerMethod;
    private final int methodParamLength;
    private final String invokeCondition;
    private final int order;


    public EventListenerMethodApplicationListener(BeanFactory beanFactory, Object bean, Method eventListenerMethod, String invokeCondition, Integer order) {
        this.spELRuntime = beanFactory.getBean(SPEL_RUNTIME_BEAN, SpELRuntime.class);
        this.bean = bean;
        this.eventListenerMethod = eventListenerMethod;
        this.methodParamLength = eventListenerMethod.getParameterCount();
        this.invokeCondition = invokeCondition;
        this.order = order;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        invokeEventListenerMethod(getEventListenerMethodParameters(event));
    }

    // @EventListener条件判断
    public boolean conditionalJudgment(ApplicationEvent event){
        if(!StringUtils.hasLength(invokeCondition))
            return true;
        Object[] invokeParam = getEventListenerMethodParameters(event);
        Map<String, Object> rootObject = new ConcurrentHashMap<>();
        rootObject.put("args", invokeParam);
        rootObject.put("event", event);
        Object result =  spELRuntime.getValueForType(new ParamWrapper(invokeCondition).setRootObject(rootObject).addVariables(eventListenerMethod, invokeParam));
        if(result instanceof Boolean)
            return (Boolean)result;
        String strResult = String.valueOf(result);
        return "true".equalsIgnoreCase(strResult) || "on".equalsIgnoreCase(strResult) || "yes".equalsIgnoreCase(strResult) || "1".equalsIgnoreCase(strResult);
    }


    // 获取事件监听方法的执行参数
    private Object[] getEventListenerMethodParameters(ApplicationEvent event){
        if(methodParamLength == 1){
            Class<?> methodType = eventListenerMethod.getParameters()[0].getType();
            if((ApplicationEvent.class.isAssignableFrom(methodType))){
                return new Object[]{event};
            }
            return new Object[]{((PayloadApplicationEvent<?>)event).getPayload()};

        }
        return new Object[0];
    }


    // 执行事件监听方法
    private void invokeEventListenerMethod(Object[] invokeParam){
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
