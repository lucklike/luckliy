package com.luckyframework.httpclient.core.impl;

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

    @Override
    public InputStream inputStreamConvert(InputStream sourceInputStream) throws IOException {
        return new InflaterInputStream(sourceInputStream, new Inflater(true));
    }

    @Override
    public String contentEncoding() {
        return "deflate";
    }
}
