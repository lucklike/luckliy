package com.luckyframework.httpclient.core.impl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
    public byte[] byteConvert(byte[] old) {
        return brotliDecode(old);
    }

    public byte[] brotliDecode(byte[] old) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(old);
             BrotliInputStream unbrotli = new BrotliInputStream(in)) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = unbrotli.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            byte[] result = out.toByteArray();
            log.info("brotli uncompress successful. Compression ratio: ({} - {}) / {} = {}", result.length, old.length, result.length, StringUtils.decimalToPercent((double) (result.length - old.length) / result.length));
            return result;
        } catch (IOException e) {
            throw new LuckyRuntimeException("brotli uncompress error.", e).printException(log);
        }
    }
}
