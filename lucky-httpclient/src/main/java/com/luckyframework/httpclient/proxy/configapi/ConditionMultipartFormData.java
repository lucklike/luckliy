package com.luckyframework.httpclient.proxy.configapi;

public class ConditionMultipartFormData {

    private String condition;
    private MultipartFormData data;

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public MultipartFormData getData() {
        return data;
    }

    public void setData(MultipartFormData data) {
        this.data = data;
    }
}
