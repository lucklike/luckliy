package com.luckyframework.context.event;

import java.util.EventListener;

/**
 * 事件监听器，当使用{@link ApplicationEventPublisher}发布个事件时，
 * 对应泛型类型的监听器便可监听到该事件
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/15 下午11:57
 */
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

    /**
     * Handle an application event.
     * @param event the event to respond to
     */
    void onApplicationEvent(E event);

}