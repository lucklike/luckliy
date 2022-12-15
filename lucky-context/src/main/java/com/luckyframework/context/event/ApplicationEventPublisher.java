package com.luckyframework.context.event;

/**
 * 事件发布者，可以使用该组件发布一个事件，发布之后与之对应的监听器的监听方法会被按照顺序执行
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/15 下午11:59
 */
@FunctionalInterface
public interface ApplicationEventPublisher {

    default void publishEvent(ApplicationEvent event) {
        publishEvent((Object) event);
    }

    void publishEvent(Object event);

}
