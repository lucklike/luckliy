package com.luckyframework.context.event;

import com.luckyframework.context.ApplicationContext;

/**
 * 容器即将关闭时发布的事件
 *
 * @author fk7075
 * @since 12.08.2003
 * @see ContextRefreshedEvent
 */
@SuppressWarnings("serial")
public class ContextClosedEvent extends ApplicationContextEvent {

    /**
     * Creates a new ContextClosedEvent.
     * @param source the {@code ApplicationContext} that has been closed
     * (must not be {@code null})
     */
    public ContextClosedEvent(ApplicationContext source) {
        super(source);
    }

}
