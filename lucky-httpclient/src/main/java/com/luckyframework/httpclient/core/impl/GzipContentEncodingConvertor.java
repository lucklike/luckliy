package com.luckyframework.httpclient.core.impl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    public byte[] byteConvert(byte[] old) {
        return gzipDecompress(old);
    }

    @Override
    public String contentEncoding() {
        return "gzip";
    }

    public byte[] gzipDecompress(byte[] old) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(old);
             GZIPInputStream ungzip = new GZIPInputStream(in)) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            byte[] result = out.toByteArray();
            log.info("gzip uncompress successful. Compression ratio: ({} - {}) / {} = {}", result.length, old.length, result.length, StringUtils.decimalToPercent((double) (result.length - old.length) / result.length));
            return result;
        } catch (IOException e) {
            throw new LuckyRuntimeException("gzip uncompress error.", e).printException(log);
        }
    }
}
