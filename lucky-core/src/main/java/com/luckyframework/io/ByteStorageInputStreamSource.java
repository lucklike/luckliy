package com.luckyframework.io;

import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.NonNull;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于byte数组实现的有存储介质的InputStreamSource
 */
public class ByteStorageInputStreamSource implements StorageMediumStream, InputStreamSource {

    private byte[] buffer;
    private final long length;
    private boolean isDel;

    public ByteStorageInputStreamSource(byte[] buffer) {
        this.buffer = buffer;
        this.length = buffer.length;
    }

    public ByteStorageInputStreamSource(@NonNull InputStream inputStream) throws IOException {
        this(FileCopyUtils.copyToByteArray(inputStream));
    }

    @Override
    public boolean deleteStorageMedium() {
        buffer = null;
        isDel = true;
        return true;
    }

    @Override
    public long length() {
        return length;
    }

    @NonNull
    @Override
    public InputStream getInputStream() throws IOException {
        if (isDel) {
            throw new IOException("Storage media has been deleted");
        }
        return new ByteArrayInputStream(buffer);
    }


}
