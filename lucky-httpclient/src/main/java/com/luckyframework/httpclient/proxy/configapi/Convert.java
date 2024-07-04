package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:19
 */
public class Convert {

    private String result;

    private String exception;

    @TargetField("meta-type")
    private Class<?> metaType = Object.class;

    private List<Condition> condition = new ArrayList<>();

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public List<Condition> getCondition() {
        return condition;
    }

    public void setCondition(List<Condition> condition) {
        this.condition = condition;
    }

    public Class<?> getMetaType() {
        return metaType;
    }

    public void setMetaType(Class<?> metaType) {
        this.metaType = metaType;
    }
}
