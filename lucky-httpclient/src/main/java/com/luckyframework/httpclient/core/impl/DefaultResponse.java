package com.luckyframework.httpclient.core.impl;

import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;

/**
 * Response默认实现
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 2:59 下午
 */
public class DefaultResponse implements Response {

    private static SaveResultResponseProcessor commonProcessor;

    private final ResponseMetaData responseMetaData;
    private final byte[] result;


    public DefaultResponse(ResponseMetaData responseMetaData) throws IOException {
        this.responseMetaData = responseMetaData;
        this.result = FileCopyUtils.copyToByteArray(responseMetaData.getInputStream());
    }

    public static SaveResultResponseProcessor getCommonProcessor() {
        return commonProcessor == null ? new SaveResultResponseProcessor() : commonProcessor;
    }

    public static void setCommonProcessor(SaveResultResponseProcessor commonProcessor) {
        DefaultResponse.commonProcessor = commonProcessor;
    }

    @Override
    public Request getRequest() {
        return this.responseMetaData.getRequest();
    }

    @Override
    public int getState() {
        return this.responseMetaData.getStatus();
    }

    @Override
    public HttpHeaderManager getHeaderManager() {
        return this.responseMetaData.getResponseHeader();
    }

    @Override
    public byte[] getResult() {
        return this.result;
    }

    @Override
    public ResponseMetaData getResponseMetaData() {
        return this.responseMetaData;
    }

}
