package com.luckyframework.httpclient.proxy.configapi;

import java.util.List;
import java.util.Map;

public class ConditionMapList {

    private String condition;
    private Map<String, List<Object>> data;


    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Map<String, List<Object>> getData() {
        return data;
    }

    public void setData(Map<String, List<Object>> data) {
        this.data = data;
    }
}
