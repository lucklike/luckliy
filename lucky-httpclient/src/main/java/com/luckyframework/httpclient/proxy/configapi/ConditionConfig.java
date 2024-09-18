package com.luckyframework.httpclient.proxy.configapi;

public class ConditionConfig<T> {

    private String condition;
    private T data;

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
