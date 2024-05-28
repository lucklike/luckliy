package com.luckyframework.httpclient.core;

import com.luckyframework.web.ContentTypeUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 响应原数据
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/5 13:09
 */
public final class ResponseMetaData extends HeaderMataData {

    private final InputStreamFactory inputStreamFactory;

    public ResponseMetaData(Request request,
                            int status,
                            HttpHeaderManager responseHeader,
                            InputStreamFactory inputStreamFactory
    ) {
        super(request, status, responseHeader);
        this.inputStreamFactory = inputStreamFactory;
    }


    /**
     * 获取响应体内容对应的InputStream
     *
     * @return 响应体内容对应的InputStream
     * @throws IOException 获取失败时会抛出该异常
     */
    public InputStream getInputStream() throws IOException {
        return inputStreamFactory.getInputStream();
    }

    /**
     * 获取响应的Content-Type
     *
     * @return 响应Content-Type
     */
    public ContentType getContentType() {
        ContentType contentType = super.getContentType();
        if (contentType != ContentType.NON) {
            return contentType;
        }
        try {
            String mimeType = ContentTypeUtils.getMimeType(getInputStream());
            return mimeType == null ? ContentType.NON : ContentType.create(mimeType, "");
        } catch (IOException e) {
            return ContentType.NON;
        }
    }

}
