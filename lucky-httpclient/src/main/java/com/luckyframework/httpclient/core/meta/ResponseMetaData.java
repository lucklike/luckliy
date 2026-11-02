package com.luckyframework.httpclient.core.meta;

import com.luckyframework.exception.LuckyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.Nullable;

import java.io.Closeable;
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
public final class ResponseMetaData extends HeaderMataData implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ResponseMetaData.class);

    private final InputStreamSource inputStreamSource;

    /**
     * 底层传输资源（HttpResponse、HttpURLConnection 等），可为空
     */
    private final Closeable transport;

    public ResponseMetaData(Request request,
                            int status,
                            HttpHeaderManager responseHeader,
                            InputStreamSource inputStreamSource
    ) {
        this(request, status, responseHeader, inputStreamSource, null);
    }

    public ResponseMetaData(Request request,
                            int status,
                            HttpHeaderManager responseHeader,
                            InputStreamSource inputStreamSource,
                            @Nullable Closeable transport
    ) {
        super(request, status, responseHeader);
        this.inputStreamSource = inputStreamSource;
        this.transport = transport;
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

    /**
     * 基于当前响应元数据创建一个新的响应元数据，仅替换响应流的来源，
     * 底层传输资源保持不变，确保解压等场景下资源仍能被正确释放。
     *
     * @param inputStreamSource 新的响应流来源
     * @return 新的响应元数据
     */
    public ResponseMetaData withInputStreamSource(InputStreamSource inputStreamSource) {
        return new ResponseMetaData(getRequest(), getStatus(), getHeaderManager(), inputStreamSource, transport);
    }

    /**
     * 释放底层传输资源
     * <p>
     * 优先关闭执行器传入的传输对象；对于未持有传输对象的历史构造方式则退化为关闭响应流。
     * 该方法实现为幂等的，重复调用不会产生副作用。
     */
    @Override
    public void close() {
        if (transport != null) {
            try {
                transport.close();
            } catch (Exception e) {
                log.debug("Failed to release the response transport resource: [{} {}]", getStatus(), getRequestUrl(), e);
            }
            return;
        }
        try {
            getInputStream().close();
        } catch (Exception e) {
            log.debug("Failed to close the response InputStream: [{} {}]", getStatus(), getRequestUrl(), e);
        }
    }

}
