package com.luckyframework.context.event;

import java.util.EventObject;

/**
 * 事件的基类
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/15 下午11:47
 */
public abstract class ApplicationEvent extends EventObject {

    /** use serialVersionUID from Lucky  for interoperability. */
    private static final long serialVersionUID = 88647738163871937L;

    /** System time when the event happened. */
    private final long timestamp;


    /**
     * Create a new {@code ApplicationEvent}.
     * @param source the object on which the event initially occurred or with
     * which the event is associated (never {@code null})
     */
    public ApplicationEvent(Object source) {
        super(source);
        this.timestamp = System.currentTimeMillis();
    }


    /**
     * Return the system time in milliseconds when the event occurred.
     */
    public final long getTimestamp() {
        return this.timestamp;
    }
}
