package com.luckyframework.httpclient.core.impl;

import com.luckyframework.httpclient.core.*;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 05:34
 */
public class SaveResultResponseProcessor implements ResponseProcessor {

    private Request request;
    private Response response;

    public SaveResultResponseProcessor() {
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public final void process(int status, HttpHeaderManager header, InputStreamFactory factory) {
        response.setState(status);
        response.setHeaderManager(header);
        try {
            response.setResult(FileCopyUtils.copyToByteArray(factory.getInputStream()));
            responseProcess(response);
        } catch (IOException e) {
            result2ByteExceptionHandler(e);
        }
    }

    protected void responseProcess(Response response) {

    }

    protected void result2ByteExceptionHandler(IOException e) {
        throw new HttpExecutorException("An exception occurred while processing the response result of the HTTP request:" + request, e);
    }
}
