package com.luckyframework.httpclient.proxy.configapi;

/**
 * 条件转换配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:20
 */
public class Condition {

    /**
     * 断言条件
     */
    private String assertion;

    /**
     * 响应结果
     */
    private String result;

    /**
     * 异常结果
     */
    private String exception;

    /**
     * 获取断言条件
     *
     * @return 断言条件
     */
    public String getAssertion() {
        return assertion;
    }

    /**
     * 设置断言条件
     *
     * @param assertion 断言条件
     */
    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    /**
     * 获取响应结果
     *
     * @return 响应结果
     */
    public String getResult() {
        return result;
    }

    /**
     * 设置响应结果
     *
     * @param result 响应结果
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * 获取异常结果
     *
     * @return 异常结果
     */
    public String getException() {
        return exception;
    }

    /**
     * 设置异常结果
     *
     * @param exception 异常结果
     */
    public void setException(String exception) {
        this.exception = exception;
    }
}
