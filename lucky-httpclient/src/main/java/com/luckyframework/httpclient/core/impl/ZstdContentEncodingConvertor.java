package com.luckyframework.httpclient.core.impl;

import com.github.luben.zstd.ZstdInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于Zstd的内容解码转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 01:21
 */
public class ZstdContentEncodingConvertor implements ContentEncodingConvertor {

    @Override
    public InputStream inputStreamConvert(InputStream sourceInputStream) throws IOException {
        return new ZstdInputStream(sourceInputStream);
    }

    @Override
    public String contentEncoding() {
        return "zstd";
    }

}
