package com.luckyframework.io;

import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于byte数组实现的支持重复读取的输入流
 */
public class RepeatableReadByteInputStream extends InputStream implements StorageMediumStream {

    /**
     * 流的大小
     */
    private final long length;

    /**
     * 存储原始输入流数据的字节数组
     */
    private byte[] buffer;

    /**
     * 真正使用的字节数组输入流
     */
    private ByteArrayInputStream inStream;

    public RepeatableReadByteInputStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileCopyUtils.copy(in, out);
        buffer = out.toByteArray();
        length = buffer.length;
        inStream = new ByteArrayInputStream(buffer);
    }


    @Override
    public boolean deleteStorageMedium() {
        buffer = null;
        inStream = null;
        return true;
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public int read() throws IOException {
        check();
        return inStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        check();
        return inStream.read(b, off, len);
    }

    @Override
    public void reset() throws IOException {
        check();
        inStream = new ByteArrayInputStream(buffer);
    }


    @Override
    public void close() throws IOException {
        super.close();
        reset();
    }

    /**
     * 检测存储媒介是否已经被删除
     *
     * @throws IOException 删除的话会抛出IO异常
     */
    private void check() throws IOException {
        if (inStream == null || buffer == null) {
            throw new IOException("Storage media has been deleted");
        }
    }
}
