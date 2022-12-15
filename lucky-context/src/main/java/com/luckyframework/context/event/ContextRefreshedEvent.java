package com.luckyframework.context.event;

/**
 * 容器成功刷新事件
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/5 01:08
 */
public class ContextRefreshedEvent extends ApplicationEvent{
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public ContextRefreshedEvent(Object source) {
        super(source);
    }
}
