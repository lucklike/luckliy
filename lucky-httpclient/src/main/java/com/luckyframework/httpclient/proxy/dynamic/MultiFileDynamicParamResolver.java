package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.proxy.annotations.MultiFile;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 资源参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:10
 */
public class MultiFileDynamicParamResolver extends AbstractDynamicParamResolver {

    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        Object value = valueContext.getValue();
        HttpFile[] httpFiles;
        // 资源类型可以直接转为HttpFile
        if (valueContext.isResourceValue()) {
            httpFiles = HttpExecutor.toHttpFiles(value);
        }
        // 字符串类型或者是字符串数组、集合类型
        else if (isStringIterable(value)) {
            if (ContainerUtils.isIterable(value)) {
                List<Resource> resourceList = new ArrayList<>();
                Iterator<Object> iterator = ContainerUtils.getIterator(value);
                while (iterator.hasNext()) {
                    resourceList.addAll(Arrays.asList(ConversionUtils.conversion(iterator.next(), Resource[].class)));
                }
                httpFiles = HttpExecutor.toHttpFiles(resourceList);
            } else {
                httpFiles = HttpExecutor.toHttpFiles(ConversionUtils.conversion(value, Resource[].class));
            }
        }
        // byte[]、Byte[]、InputStream、InputStreamSource对应的Map
        else if (Map.class.isAssignableFrom(value.getClass())) {
            List<HttpFile> httpFileList = new ArrayList<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String mKey = String.valueOf(entry.getKey());
                Object mValue = entry.getValue();

                if (isHttpFileObject(mValue)) {
                    httpFileList.add(toHttpFile(mValue, mKey));
                } else {
                    Class<?> elementType = ContainerUtils.getElementType(mValue);
                    if (isHttpFileType(elementType)) {
                        if (ContainerUtils.isIterable(mValue)) {
                            Iterator<Object> iterator = ContainerUtils.getIterator(mValue);
                            int i = 0;
                            String _index_ = "{_index_}";
                            while (iterator.hasNext()) {
                                httpFileList.add(toHttpFile(iterator.next(), mKey.replace(_index_, String.valueOf(i++))));
                            }
                        } else {
                            httpFileList.add(toHttpFile(mValue, mKey));
                        }
                    } else {
                        throw new IllegalArgumentException(StringUtils.format("The value '{}' corresponding to the key named '{}' in the Map cannot be converted to HttpFile type.", ClassUtils.getClassName(mValue), mKey));
                    }
                }
            }
            httpFiles = httpFileList.toArray(new HttpFile[0]);
        }
        // byte[]、Byte[]、InputStream、InputStreamSource系列
        else {
            String fileName = context.toAnnotation(MultiFile.class).fileName();
            if (!StringUtils.hasText(fileName)) {
                throw new IllegalArgumentException(StringUtils.format("The @MultiFile parameter of type '{}' must specify the fileName", ClassUtils.getClassName(value)));
            }
            fileName = context.parseExpression(fileName);
            if (isHttpFileObject(value)) {
                httpFiles = new HttpFile[]{toHttpFile(value, fileName)};
            } else {
                Class<?> elementType = ContainerUtils.getElementType(value);
                if (isHttpFileType(elementType)) {
                    if (ContainerUtils.isIterable(value)) {
                        List<HttpFile> httpFileList = new ArrayList<>();
                        Iterator<Object> iterator = ContainerUtils.getIterator(value);
                        int i = 0;
                        String _index_ = "{_index_}";
                        while (iterator.hasNext()) {
                            httpFileList.add(toHttpFile(iterator.next(), fileName.replace(_index_, String.valueOf(i++))));
                        }
                        httpFiles = httpFileList.toArray(new HttpFile[0]);
                    } else {
                        httpFiles = new HttpFile[]{toHttpFile(value, fileName)};
                    }
                } else {
                    throw new IllegalArgumentException(StringUtils.format("The '{}' type parameter cannot be converted to HttpFile", ClassUtils.getClassName(value)));
                }
            }
        }
        return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), httpFiles));
    }

    private byte[] toBytes(Byte[] bytes) {
        byte[] bytesArr = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            bytesArr[i] = bytes[i];
        }
        return bytesArr;
    }

    private HttpFile toHttpFile(Object object, String fileName) {
        if (object instanceof InputStream) {
            return new HttpFile(((InputStream) object), fileName);
        } else if (object instanceof InputStreamSource) {
            return new HttpFile((InputStreamSource) object, () -> fileName, "[InputStreamSource] " + fileName);
        } else if (object instanceof byte[]) {
            return new HttpFile(((byte[]) object), fileName);
        } else if (object instanceof Byte[]) {
            return new HttpFile(toBytes((Byte[]) object), fileName);
        } else {
            throw new IllegalArgumentException(StringUtils.format("The '{}' type parameter cannot be converted to HttpFile", object.getClass().getName()));
        }
    }

    private boolean isHttpFileObject(Object value) {
        return value instanceof byte[] ||
                value instanceof Byte[] ||
                value instanceof InputStreamSource ||
                value instanceof InputStream;
    }

    private boolean isHttpFileType(Class<?> clazz) {
        return clazz == byte[].class ||
                clazz == Byte[].class ||
                InputStreamSource.class.isAssignableFrom(clazz) ||
                InputStream.class.isAssignableFrom(clazz);
    }

    private boolean isStringIterable(Object value) {
        return ContainerUtils.getElementType(value) == String.class;
    }
}
