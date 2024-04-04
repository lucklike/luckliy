package com.luckyframework.httpclient.core.impl;

import com.luckyframework.exception.LuckyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * 基于Gzip的内容解码转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 01:21
 */
public class GzipContentEncodingConvertor implements ContentEncodingConvertor {

    private static final Logger log = LoggerFactory.getLogger(GzipContentEncodingConvertor.class);


    @Override
    public byte[] byteConvert(InputStream inputStream) {
        return gzipDecode(inputStream);
    }

    public byte[] gzipDecode(InputStream inputStream) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(FileCopyUtils.copyToByteArray(inputStream));
             GZIPInputStream ungzip = new GZIPInputStream(in)) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new LuckyRuntimeException("gzip uncompress error.", e).printException(log);
        }
    }
}
