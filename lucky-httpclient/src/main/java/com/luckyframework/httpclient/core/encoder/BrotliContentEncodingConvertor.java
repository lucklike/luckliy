package com.luckyframework.httpclient.core.encoder;

import org.brotli.dec.BrotliInputStream;

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

    @Override
    public InputStream inputStreamConvert(InputStream sourceInputStream) throws IOException {
        return new BrotliInputStream(sourceInputStream);
    }

    @Override
    public String contentEncoding() {
        return "br";
    }
}
