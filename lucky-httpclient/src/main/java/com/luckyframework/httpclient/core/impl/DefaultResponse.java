package com.luckyframework.httpclient.core.impl;

import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Response;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 2:59 下午
 */
public class DefaultResponse implements Response {

    private static SaveResultResponseProcessor commonProcessor;

    private int state;
    private HttpHeaderManager headerManager;
    private byte[] result;

    public DefaultResponse() {
    }

    public DefaultResponse(int state, HttpHeaderManager header, byte[] result) {
        this.state = state;
        this.headerManager = header;
        this.result = result;
    }

    public static SaveResultResponseProcessor getCommonProcessor() {
        return commonProcessor == null ? new SaveResultResponseProcessor() : commonProcessor;
    }

    public static void setCommonProcessor(SaveResultResponseProcessor commonProcessor) {
        DefaultResponse.commonProcessor = commonProcessor;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setHeaderManager(HttpHeaderManager headerManager) {
        this.headerManager = headerManager;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public HttpHeaderManager getHeaderManager() {
        return headerManager;
    }

    @Override
    public byte[] getResult() {
        return result;
    }

}
