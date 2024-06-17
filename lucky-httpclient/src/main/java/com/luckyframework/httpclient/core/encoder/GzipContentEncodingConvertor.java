package com.luckyframework.httpclient.core.encoder;

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

    @Override
    public InputStream inputStreamConvert(InputStream sourceInputStream) throws IOException {
        return new GZIPInputStream(sourceInputStream);
    }

    @Override
    public String contentEncoding() {
        return "gzip";
    }

}
