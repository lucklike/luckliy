package com.luckyframework.httpclient.proxy.configapi;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:20
 */
public class Condition {

    private String assertion;
    private String result;
    private String exception;

    public String getAssertion() {
        return assertion;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

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
}
