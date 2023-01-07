package com.luckyframework.context.event;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/18 下午2:41
 */
public class DefaultApplicationEventMulticaster implements ApplicationEventMulticaster {

    private final BeanFactory beanFactory;
    private final Set<EventTypeApplicationListenerMapping> listenerMappings = new HashSet<>();

    public DefaultApplicationEventMulticaster(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }


    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        Class<?> itselfType = listener.getClass();
        ResolvableType eventType = ResolvableType.forClass(ApplicationListener.class, itselfType).getGeneric(0);
        addApplicationListener(listener, eventType);
    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener, ResolvableType eventType) {
        for (EventTypeApplicationListenerMapping mapping : listenerMappings) {
            if(mapping.isEquals(eventType)){
                mapping.addApplicationListener(listener);
                return;
            }
        }
        listenerMappings.add(new EventTypeApplicationListenerMapping(listener, eventType));
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

    public void removeApplicationListenerByType(String listenerId, ResolvableType eventType){
        for (EventTypeApplicationListenerMapping mapping : listenerMappings) {
            if(mapping.isMatches(eventType)){
                mapping.removeApplicationListener(listenerId);
                break;
            }
        }
    }

    @Override
    public void removeAllListeners() {
        this.listenerMappings.clear();
    }

    @Override
    public void multicastEvent(ApplicationEvent event) {
        multicastEvent(event, null);
    }

    @Override
    @SuppressWarnings("all")
    public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
        ResolvableType type = eventType;
        if(type == null){
            type = ResolvableType.forInstance(event);
            // type 存在泛型的情况
            if(type.hasGenerics() && Objects.nonNull(event.getSource())){
                type = ResolvableType.forClassWithGenerics(event.getClass(), ResolvableType.forInstance(event.getSource()));
            }
        }

        List<ApplicationListener<?>> matchingListenerList = new ArrayList<>();
        for (EventTypeApplicationListenerMapping mapping : listenerMappings) {
            if(mapping.isMatches(type)){
                matchingListenerList.addAll(mapping.getListeners());
            }
            if(event instanceof PayloadApplicationEvent){
                ResolvableType payloadType = type.as(PayloadApplicationEvent.class).getGeneric();
                if(mapping.isMatches(payloadType)){
                    matchingListenerList.addAll(mapping.getListeners());
                }
            }
        }

        // 条件过滤，移除掉那些不满足条件的EventListenerMethodApplicationListener
        matchingListenerList.removeIf((listener) -> {
            if(listener instanceof EventListenerMethodApplicationListener){
                EventListenerMethodApplicationListener methodListener = (EventListenerMethodApplicationListener) listener;
                return ! methodListener.conditionalJudgment(event);
            }
            return false;
        });


        AnnotationAwareOrderComparator.sort(matchingListenerList);
        for (ApplicationListener applicationListener : matchingListenerList) {
            invokeListener(applicationListener, event);
        }
    }

    @SuppressWarnings("all")
    protected void invokeListener(ApplicationListener listener, ApplicationEvent event){
        listener.onApplicationEvent(event);
    }

    private String getApplicationListenerId(ApplicationListener<?> listener){
        if(listener instanceof EventListenerMethodApplicationListener){
            return ((EventListenerMethodApplicationListener)listener).getMethodName();
        }
        return listener.getClass().getName();
    }

    class EventTypeApplicationListenerMapping {

        private final ResolvableType eventType;
        private final Map<String, ApplicationListener<?>> listeners = new ConcurrentHashMap<>();

        EventTypeApplicationListenerMapping(@Nullable ApplicationListener<?> listener, @Nullable ResolvableType eventType) {
            this.eventType = eventType;
            this.addApplicationListener(listener);
        }

        public boolean isMatches(ResolvableType inspectedType){
            return ClassUtils.compatibleOrNot(this.eventType, inspectedType);
        }

        public boolean isEquals(ResolvableType inspectedType){
            return this.eventType.toString().equals(inspectedType.toString());
        }

        public void addApplicationListener(ApplicationListener<?> listener){
            listeners.put(getApplicationListenerId(listener), listener);
        }

        public void removeApplicationListener(String listenerId){
            listeners.remove(listenerId);
        }

        public Collection<ApplicationListener<?>> getListeners(){
            return listeners.values();
        }



    }
}
