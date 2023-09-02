package com.luckyframework.httpclient.core.impl;

import com.luckyframework.httpclient.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 05:34
 */
public class SaveResultResponseProcessor implements ResponseProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SaveResultResponseProcessor.class);

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
        InputStream inputStream;
        try {
            inputStream = factory.getInputStream();
        } catch (IOException e) {
            throw new HttpExecutorException(e, "An exception occurred while obtaining the response flow, status is '{}', request is {}", status, request);
        }
        try {
            response.setResult(FileCopyUtils.copyToByteArray(inputStream));
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
