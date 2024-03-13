package com.luckyframework.httpclient.core;


import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.Charset;

/**
 * 二进制请求体序列化实现类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/11 16:48
 */
public class BinaryBodySerialization implements BodySerialization {
    @Override
    public byte[] serialization(Object object, Charset charset) throws Exception {
        if (HttpExecutor.isBinaryParam(object)) {
            return HttpExecutor.toByte(object);
        }
        Resource resource = ConversionUtils.conversion(object, Resource.class);
        return FileCopyUtils.copyToByteArray(resource.getInputStream());
    }
}
