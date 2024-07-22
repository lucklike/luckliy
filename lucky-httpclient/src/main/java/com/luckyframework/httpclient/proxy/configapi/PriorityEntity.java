package com.luckyframework.httpclient.proxy.configapi;

public class PriorityEntity<T> {

    private final Integer priority;
    private final T entity;


    private PriorityEntity(Integer priority, T entity) {
        this.priority = priority;
        this.entity = entity;
    }

    public static <T> PriorityEntity<T> of(Integer priority, T entity) {
        return new PriorityEntity<>(priority, entity);
    }

    public Integer getPriority() {
        return priority;
    }

    public T getEntity() {
        return entity;
    }
}
