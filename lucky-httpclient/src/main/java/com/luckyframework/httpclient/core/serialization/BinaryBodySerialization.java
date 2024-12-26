package com.luckyframework.httpclient.core.serialization;


import com.luckyframework.common.StringUtils;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Supplier;

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

    @Override
    public Supplier<String> stringSupplier(Object object, byte[] objBytes, String mimeType, Charset charset) {
        if (object instanceof File) {
            File file = (File) object;
            return () -> StringUtils.format("File Body ({}) {}", UnitUtils.byteTo(file.length()), file.getAbsolutePath());
        }
        if (object instanceof Resource) {
            Resource resource = (Resource) object;
            return () -> StringUtils.format("Resource Body ({}) {}", UnitUtils.byteTo(objBytes.length), resource.getDescription());
        }
        if (object instanceof MultipartFile) {
            MultipartFile muFile = (MultipartFile) object;
            return () -> {
                try {
                    return StringUtils.format("MultipartFile Body ({}) {}", UnitUtils.byteTo(muFile.getSize()), muFile.getOriginalFileName());
                } catch (IOException e) {
                    throw new LuckyRuntimeException(e);
                }
            };
        }
        if (object instanceof String) {
            return () -> StringUtils.format("Resource Body ({}) {}", UnitUtils.byteTo(objBytes.length), object);
        }
        return () -> StringUtils.format("{} Body '{}' size: {}", ClassUtils.getClassSimpleName(object), mimeType, UnitUtils.byteTo(objBytes.length));
    }
}
