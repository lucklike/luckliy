package com.luckyframework.httpclient.core;


import org.springframework.lang.NonNull;

import java.io.BufferedInputStream;
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

    public ResponseInputStream(@NonNull InputStream in, Closeable closeable) {
        super(in);
        this.closeable = closeable;
    }

    public ResponseInputStream(@NonNull InputStream in, int size, Closeable closeable) {
        super(in, size);
        this.closeable = closeable;
    }

    public ResponseInputStream(@NonNull InputStream in) {
        this(in, null);
    }

    public ResponseInputStream(@NonNull InputStream in, int size) {
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
