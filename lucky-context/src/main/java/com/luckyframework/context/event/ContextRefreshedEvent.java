package com.luckyframework.context.event;

import com.luckyframework.context.ApplicationContext;

/**
 * 容器成功刷新事件
 * @author fk7075
 * @version 1.0.0
 * @date 2022/10/5 01:08
 */
public class ContextRefreshedEvent extends ApplicationContextEvent{
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param applicationContext the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public ContextRefreshedEvent(ApplicationContext applicationContext) {
        super(applicationContext);
    }
}
