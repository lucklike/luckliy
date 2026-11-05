package com.luckyframework.httpclient.core.meta;


import org.springframework.lang.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * 响应输入流
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2024/6/14 17:23
 */
public class ResponseInputStream extends BufferedInputStream {

    private final Closeable closeable;

    public ResponseInputStream(@Nullable InputStream in, Closeable closeable) {
        super(in == null ? new ByteArrayInputStream(new byte[0]) : in);
        this.closeable = closeable;
    }

    public ResponseInputStream(@Nullable InputStream in, int size, Closeable closeable) {
        super(in == null ? new ByteArrayInputStream(new byte[0]) : in, size);
        this.closeable = closeable;
    }

    public ResponseInputStream(@Nullable InputStream in) {
        this(in, null);
    }

    public ResponseInputStream(@Nullable InputStream in, int size) {
        this(in, size, null);
    }

    @Override
    public void close() throws IOException {
        IOException ioException = null;
        try {
            super.close();
        } catch (IOException e) {
            ioException = e;
        }
        // 无论流的关闭是否成功，都必须释放底层资源（连接等）
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                if (ioException == null) {
                    ioException = new IOException(e);
                } else {
                    ioException.addSuppressed(e);
                }
            }
        }
        if (ioException != null) {
            throw ioException;
        }
    }
}
