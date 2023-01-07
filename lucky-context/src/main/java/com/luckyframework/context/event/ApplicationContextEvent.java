package com.luckyframework.context.event;

import com.luckyframework.context.ApplicationContext;

/**
 * 基本的lucky容器事件
 *
 * @author fukang
 * @since 2.5
 */
@SuppressWarnings("serial")
public abstract class ApplicationContextEvent extends ApplicationEvent {

    /**
     * Create a new ContextStartedEvent.
     * @param source the {@code ApplicationContext} that the event is raised for
     * (must not be {@code null})
     */
    public ApplicationContextEvent(ApplicationContext source) {
        super(source);
    }

    /**
     * Get the {@code ApplicationContext} that the event was raised for.
     */
    public final ApplicationContext getApplicationContext() {
        return (ApplicationContext) getSource();
    }

}
