package com.luckyframework.httpclient.core.meta;

import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.core.io.InputStreamSource;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static com.luckyframework.httpclient.core.executor.HttpExecutor.EMPTY_INPUT_STREAM;

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
        }
        // EOFException 表示响应体为空
        catch (EOFException e) {
            return EMPTY_INPUT_STREAM;
        }
        // 其他异常则需要手动抛出
        catch (IOException e) {
            throw new LuckyRuntimeException("An exception occurred while obtaining the response InputStream", e);
        }

    }

}
