package com.luckyframework.httpclient.core.impl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    public byte[] byteConvert(byte[] old) {
        return deflateDecompress(old);
    }

    public static byte[] deflateDecompress(byte[] old) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(old);
             InflaterInputStream undeflate = new InflaterInputStream(in, new Inflater(true))) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = undeflate.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            byte[] result = out.toByteArray();
            log.info("deflate uncompress successful. Compression ratio: ({} - {}) / {} = {}", result.length, old.length, result.length, StringUtils.decimalToPercent((double) (result.length - old.length) / result.length));
            return result;
        } catch (IOException e) {
            throw new LuckyRuntimeException("deflate uncompress error.", e).printException(log);
        }
    }
}
