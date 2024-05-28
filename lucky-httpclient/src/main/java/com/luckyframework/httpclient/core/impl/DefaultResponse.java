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

    private final ResponseMetaData responseMetaData;
    private final byte[] result;


    public DefaultResponse(ResponseMetaData metaData) throws IOException {
        this.result = FileCopyUtils.copyToByteArray(metaData.getInputStream());
        this.responseMetaData = new ResponseMetaData(
                metaData.getRequest(),
                metaData.getStatus(),
                metaData.getHeaderManager(),
                this::getInputStream
        );
    }

    @Override
    public Request getRequest() {
        return this.responseMetaData.getRequest();
    }

    @Override
    public int getStatus() {
        return this.responseMetaData.getStatus();
    }

    @Override
    public HttpHeaderManager getHeaderManager() {
        return this.responseMetaData.getHeaderManager();
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
