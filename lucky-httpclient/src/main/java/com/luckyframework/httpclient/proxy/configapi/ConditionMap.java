package com.luckyframework.httpclient.proxy.configapi;

import java.util.Map;

public class ConditionMap {

    private String condition;
    private Map<String, Object> data;

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
