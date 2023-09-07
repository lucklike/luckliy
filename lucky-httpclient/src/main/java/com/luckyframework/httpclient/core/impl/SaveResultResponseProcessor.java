package com.luckyframework.httpclient.core.impl;

import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;

import java.io.IOException;

/**
 * 将响应结果以byte[]保存的响应处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 05:34
 */
public class SaveResultResponseProcessor implements ResponseProcessor {

    private Response response;


    @Override
    public final void process(ResponseMetaData responseMetaData) {

        try {
            initializeResponse(responseMetaData);
            responseProcess(response);
        } catch (IOException e) {
            result2ByteExceptionHandler(responseMetaData.getRequest(), e);
        }
    }

    protected void responseProcess(Response response) {

    }

    protected void result2ByteExceptionHandler(Request request, IOException e) {
        throw new HttpExecutorException("An exception occurred while processing the response result of the HTTP request:" + request, e);
    }

    private void initializeResponse(ResponseMetaData responseMetaData) throws IOException {
        response = new DefaultResponse(responseMetaData);
    }

    public Response getResponse() {
        return response;
    }
}
