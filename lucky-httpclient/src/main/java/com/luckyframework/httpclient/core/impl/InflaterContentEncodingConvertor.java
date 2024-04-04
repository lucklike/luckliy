package com.luckyframework.httpclient.core.impl;

import com.luckyframework.exception.LuckyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * 基于deflate的内容解码转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 01:50
 */
public class InflaterContentEncodingConvertor implements ContentEncodingConvertor {

    private static final Logger log = LoggerFactory.getLogger(InflaterContentEncodingConvertor.class);

    @Override
    public byte[] byteConvert(InputStream in) {
        return deflateDecode(in);
    }

    public static byte[] deflateDecode(InputStream inputStream) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(FileCopyUtils.copyToByteArray(inputStream));
             InflaterInputStream ungzip = new InflaterInputStream(in, new Inflater(true))) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new LuckyRuntimeException("deflate uncompress error.", e).printException(log);
        }
    }
}
