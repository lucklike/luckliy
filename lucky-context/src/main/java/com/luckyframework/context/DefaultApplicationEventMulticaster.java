package com.luckyframework.context;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.event.ApplicationEvent;
import com.luckyframework.context.event.ApplicationEventMulticaster;
import com.luckyframework.context.event.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/18 下午2:41
 */
public class DefaultApplicationEventMulticaster implements ApplicationEventMulticaster {

    private final static String APPLICATION_EVENT = ApplicationEvent.class.getName();
    private final BeanFactory beanFactory;
    private final Map<String,Map<String, ApplicationListener<?>>> listenerMap = new LinkedHashMap<>();

    public DefaultApplicationEventMulticaster(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }


    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        Class<?> itselfType = listener.getClass();
        ResolvableType eventType = ResolvableType.forClass(ApplicationListener.class,itselfType).getGeneric(0);
        addApplicationListener(listener,eventType);
    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener, ResolvableType eventType) {
        String eventTypeString = eventType.toString();
        eventTypeString = "?".equals(eventTypeString)?APPLICATION_EVENT:eventTypeString;
        Map<String, ApplicationListener<?>> eventTypeListenerMap = listenerMap.get(eventTypeString);
        if(eventTypeListenerMap == null){
            eventTypeListenerMap = new ConcurrentHashMap<>();
            eventTypeListenerMap.put(getApplicationListenerId(listener),listener);
            listenerMap.put(eventTypeString,eventTypeListenerMap);
        }else{
            eventTypeListenerMap.put(getApplicationListenerId(listener),listener);
        }
    }

    @Override
    public void addApplicationListenerBean(String listenerBeanName) {
        addApplicationListener((ApplicationListener<?>) beanFactory.getBean(listenerBeanName));
    }

    @Override
    public void removeApplicationListener(ApplicationListener<?> listener) {
        ResolvableType listenerType = ResolvableType.forClass(listener.getClass());
        ResolvableType eventType = ResolvableType.forClass(ApplicationListener.class,listenerType.getRawClass()).getGeneric(0);
        removeApplicationListenerByType(getApplicationListenerId(listener),eventType);
    }

    @Override
    public void removeApplicationListenerBean(String listenerBeanName) {
        ResolvableType listenerType = this.beanFactory.getResolvableType(listenerBeanName);
        ResolvableType eventType = ResolvableType.forClass(ApplicationListener.class,listenerType.getRawClass()).getGeneric(0);
        removeApplicationListenerByType(listenerType.toString(),eventType);
    }

    @Override
    public void removeApplicationListener(ApplicationListener<?> listener, ResolvableType eventType) {
        removeApplicationListenerByType(getApplicationListenerId(listener),eventType);
    }

    public void removeApplicationListenerByType(String listenerId,ResolvableType eventType){
        String eventTypeKey = eventType.toString();
        eventTypeKey = "?".equals(eventTypeKey)?APPLICATION_EVENT:eventTypeKey;
        Map<String, ApplicationListener<?>> theSameGenericListenerMap
                = listenerMap.get(eventTypeKey);
        if(theSameGenericListenerMap != null){
            theSameGenericListenerMap.remove(listenerId);
        }
    }

    @Override
    public void removeAllListeners() {
        this.listenerMap.clear();
    }

    @Override
    public void multicastEvent(ApplicationEvent event) {
        multicastEvent(event,ResolvableType.forClass(event.getClass()));
    }

    @Override
    public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
        List<ApplicationListener<?>> matchingListenerList = new ArrayList<>();
        Map<String, ApplicationListener<?>> genericVariableListenerMap = listenerMap.get(APPLICATION_EVENT);
        Map<String, ApplicationListener<?>> genericMatchingListenerMap = listenerMap.get(eventType.getRawClass().getName());
        if(!ContainerUtils.isEmptyMap(genericVariableListenerMap)){
            matchingListenerList.addAll(genericVariableListenerMap.values());
        }
        if(!ContainerUtils.isEmptyMap(genericMatchingListenerMap)){
            matchingListenerList.addAll(genericMatchingListenerMap.values());
        }
        AnnotationAwareOrderComparator.sort(matchingListenerList);
        for (ApplicationListener applicationListener : matchingListenerList) {
            applicationListener.onApplicationEvent(event);
        }
    }

    private String getApplicationListenerId(ApplicationListener<?> listener){
        if(listener instanceof EventListenerMethodApplicationListener){
            return ((EventListenerMethodApplicationListener)listener).getMethodName();
        }
        return listener.getClass().getName();
    }
}
