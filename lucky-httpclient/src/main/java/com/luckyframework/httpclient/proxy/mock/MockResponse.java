package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.core.meta.ResponseMetaData;

import java.io.InputStream;

/**
 *
 */
public class MockResponse implements Response {
    @Override
    public Request getRequest() {
        return null;
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public HttpHeaderManager getHeaderManager() {
        return null;
    }

    @Override
    public byte[] getResult() {
        return new byte[0];
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public ResponseMetaData getResponseMetaData() {
        return null;
    }

    @Override
    public void closeResource() {

    }
}
