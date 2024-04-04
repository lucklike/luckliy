package com.luckyframework.httpclient.core.impl;

import com.luckyframework.exception.LuckyRuntimeException;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于Brotli的内容解码转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 02:22
 */
public class BrotliContentEncodingConvertor implements ContentEncodingConvertor {

    private static final Logger log = LoggerFactory.getLogger(BrotliContentEncodingConvertor.class);

    @Override
    public byte[] byteConvert(InputStream in) {
        return brotliDecode(in);
    }

    public byte[] brotliDecode(InputStream inputStream) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(FileCopyUtils.copyToByteArray(inputStream));
             BrotliInputStream ungzip = new BrotliInputStream(in)) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new LuckyRuntimeException("brotli uncompress error.", e).printException(log);
        }
    }
}
