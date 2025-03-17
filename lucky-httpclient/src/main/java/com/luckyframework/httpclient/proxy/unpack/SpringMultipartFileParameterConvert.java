package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.proxy.annotations.MultiFile;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 将Spring{@link org.springframework.web.multipart.MultipartFile Spring MultipartFile}
 * 转化为{@link com.luckyframework.httpclient.core.meta.HttpFile}的参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/15 01:03
 */
public class SpringMultipartFileParameterConvert implements ParameterConvert {

    @Override
    public boolean canConvert(ValueContext context, Object value) {
        Class<?> elementType = ContainerUtils.getElementType(value);
        return MultipartFile.class.isAssignableFrom(elementType);
    }

    @Override
    public Object convert(ValueContext context, Object value) {
        if (ContainerUtils.isIterable(value)) {
            int iteratorLength = ContainerUtils.getIteratorLength(value);
            HttpFile[] httpFiles = new HttpFile[iteratorLength];
            int i = 0;
            for (Object mf : ContainerUtils.getIterable(value)) {
                httpFiles[i++] = toHttpFile((MultipartFile) mf);
            }
            return httpFiles;
        }
        return toHttpFile((MultipartFile) value);
    }


    private HttpFile toHttpFile(MultipartFile multipartFile) {
        try {
            return new HttpFile(multipartFile.getInputStream(), multipartFile.getOriginalFilename());
        } catch (IOException e) {
            throw new ContextValueUnpackException(e);
        }
    }
}
