package com.luckyframework.httpclient.core.impl;

import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.serializable.SerializationException;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Response默认实现
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 2:59 下午
 */
public class DefaultResponse implements Response {

    private final ResponseMetaData responseMetaData;
    private byte[] result;

    public DefaultResponse(ResponseMetaData metaData) {
        this.responseMetaData = metaData;
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
    public synchronized byte[] getResult() {
        if (result == null) {
            try {
                result = FileCopyUtils.copyToByteArray(responseMetaData.getInputStream());
            } catch (IOException e) {
               throw new SerializationException(e);
            }
        }
        return result;
    }

    @Override
    public InputStream getInputStream() {
        if (result == null) {
            return responseMetaData.getInputStream();
        }
        return new ByteArrayInputStream(result);
    }

    @Override
    public ResponseMetaData getResponseMetaData() {
        return this.responseMetaData;
    }

    @Override
    public void close() throws IOException {
        if (result == null) {
            responseMetaData.getInputStream().close();
        }
    }
}
