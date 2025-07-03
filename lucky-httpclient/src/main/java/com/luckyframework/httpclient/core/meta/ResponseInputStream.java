package com.luckyframework.httpclient.core.meta;


import org.springframework.lang.NonNull;
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
        if (closeable != null) {
            closeable.close();
        } else {
            super.close();
        }
    }
}
