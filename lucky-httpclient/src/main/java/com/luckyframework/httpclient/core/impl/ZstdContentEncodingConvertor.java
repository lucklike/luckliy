package com.luckyframework.httpclient.core.impl;

import com.github.luben.zstd.Zstd;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于Zstd的内容解码转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 01:21
 */
public class ZstdContentEncodingConvertor implements ContentEncodingConvertor {

    private static final Logger log = LoggerFactory.getLogger(ZstdContentEncodingConvertor.class);


    @Override
    public byte[] byteConvert(byte[] old) {
        return zstdDecompress(old);
    }

    @Override
    public String contentEncoding() {
        return "zstd";
    }

    public byte[] zstdDecompress(byte[] old) {
        try  {
            int size = (int) Zstd.getFrameContentSize(old);
            byte[] result = new byte[size];
            Zstd.decompress(result, old);

            log.info("zstd uncompress successful. Compression ratio: ({} - {}) / {} = {}", result.length, old.length, result.length, StringUtils.decimalToPercent((double) (result.length - old.length) / result.length));
            return result;
        } catch (Exception e) {
            throw new LuckyRuntimeException("zstd uncompress error.", e).printException(log);
        }
    }
}
