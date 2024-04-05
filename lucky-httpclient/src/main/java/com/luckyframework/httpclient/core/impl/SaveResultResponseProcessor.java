package com.luckyframework.httpclient.core.impl;

import com.luckyframework.httpclient.core.Header;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.HttpHeaders;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * 将响应结果以byte[]保存的响应处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 05:34
 */
public class SaveResultResponseProcessor implements ResponseProcessor {

    private Response response;
    private final Map<String, ContentEncodingConvertor> cecMap = new LinkedCaseInsensitiveMap<>();

    {
        cecMap.put("gzip", new GzipContentEncodingConvertor());
        cecMap.put("deflate", new InflaterContentEncodingConvertor());
        cecMap.put("br", new BrotliContentEncodingConvertor());
    }

    public void addContentEncodingConvertor(String name, ContentEncodingConvertor convertor) {
        this.cecMap.put(name, convertor);
    }

    public ContentEncodingConvertor getContentEncodingConvertor(String name) {
        return cecMap.get(name);
    }

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
        Header contentEncodingHeader = responseMetaData.getHeaderManager().getFirstHeader(HttpHeaders.CONTENT_ENCODING);
        if (contentEncodingHeader != null) {
            ContentEncodingConvertor encodingConvertor = this.cecMap.get(String.valueOf(contentEncodingHeader.getValue()));
            if (encodingConvertor != null) {
                final ResponseMetaData frmd = responseMetaData;
                responseMetaData = new ResponseMetaData(
                        frmd.getRequest(),
                        frmd.getStatus(),
                        frmd.getHeaderManager(),
                        () -> new ByteArrayInputStream(encodingConvertor.byteConvert(FileCopyUtils.copyToByteArray(frmd.getInputStream())))
                );
            }
        }
        response = new DefaultResponse(responseMetaData);
    }

    public Response getResponse() {
        return response;
    }
}
