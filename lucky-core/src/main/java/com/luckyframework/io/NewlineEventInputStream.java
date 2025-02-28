package com.luckyframework.io;

import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * 遇到换行符（\n）时执行特殊处理的输入流
 */
public class NewlineEventInputStream extends InputStream {

    private final InputStream in;
    private final Charset charset;
    private final LineEventListener listener;
    private final ByteArrayOutputStream currentLineBuffer = new ByteArrayOutputStream();
    private int currentLineNumber = 1;

    public NewlineEventInputStream(InputStream in, Charset charset, LineEventListener listener) {
        this.in = in;
        this.charset = charset;
        this.listener = listener;
    }

    public NewlineEventInputStream(InputStream in, LineEventListener listener) {
        this(in, Charset.defaultCharset(), listener);
    }

    @Override
    public int read() throws IOException {
        int byteRead = in.read();
        if (byteRead == -1) {
            return -1;
        }

        if (byteRead == '\n') {
            String lineContent = new String(currentLineBuffer.toByteArray(), charset);
            listener.onNewline(lineContent, currentLineNumber++);
            currentLineBuffer.reset();
        } else {
            currentLineBuffer.write(byteRead);
        }

        return byteRead;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
