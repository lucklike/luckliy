package com.luckyframework.httpclient.core.meta;

import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.core.io.InputStreamSource;

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

    private final InputStreamSource inputStreamSource;

    public ResponseMetaData(Request request,
                            int status,
                            HttpHeaderManager responseHeader,
                            InputStreamSource inputStreamSource
    ) {
        super(request, status, responseHeader);
        this.inputStreamSource = inputStreamSource;
    }

    /**
     * 获取响应{@link InputStream}
     *
     * @return 响应InputStream
     */
    public InputStream getInputStream() {
        try {
            return inputStreamSource.getInputStream();
        }catch (IOException e) {
            throw new LuckyRuntimeException("An exception occurred while obtaining the response InputStream", e);
        }

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
