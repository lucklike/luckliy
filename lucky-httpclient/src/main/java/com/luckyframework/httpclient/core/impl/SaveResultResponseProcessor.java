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
import java.util.Collection;
import java.util.Map;

/**
 * 将响应结果以byte[]保存的响应处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 05:34
 */
public class SaveResultResponseProcessor implements ResponseProcessor {


    private static final Map<String, ContentEncodingConvertor> CEC_MAP = new LinkedCaseInsensitiveMap<>();

    private Response response;

    static {
        addContentEncodingConvertor(new GzipContentEncodingConvertor());
        addContentEncodingConvertor(new InflaterContentEncodingConvertor());
    }

    public static void addContentEncodingConvertor(ContentEncodingConvertor convertor) {
        SaveResultResponseProcessor.CEC_MAP.put(convertor.contentEncoding(), convertor);
    }

    public static ContentEncodingConvertor getContentEncodingConvertor(String name) {
        return CEC_MAP.get(name);
    }

    public Collection<ContentEncodingConvertor> getContentEncodingConvertors() {
        return CEC_MAP.values();
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
            ContentEncodingConvertor encodingConvertor = getContentEncodingConvertor(String.valueOf(contentEncodingHeader.getValue()));
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
