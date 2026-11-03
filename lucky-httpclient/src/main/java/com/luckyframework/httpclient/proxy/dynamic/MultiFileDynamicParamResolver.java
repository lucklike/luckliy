package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.exception.ParameterConvertException;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.proxy.annotations.MultiFile;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.io.HttpResource;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
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

    private static final String INDEX_PLACEHOLDER = "{_index_}";

    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        Object value = materializeIterator(valueContext.getValue());
        String fileName = context.parseExpression(context.toAnnotation(MultiFile.class).fileName());
        HttpFile[] httpFiles;
        // 文件名是否已由当前分支自行确定：Map传参以key为文件名，兜底分支用fileName构造，均无需事后再覆盖
        boolean fileNameHandled;
        // 空数组、空集合、空Iterator不产生任何文件项，避免在类型判定阶段抛异常
        if (isEmptyContainer(value)) {
            httpFiles = new HttpFile[0];
            fileNameHandled = true;
        }
        // 资源类型可以直接转为HttpFile
        else if (HttpExecutor.isResourceParam(value)) {
            httpFiles = HttpExecutor.toHttpFiles(value);
            fileNameHandled = false;
        }
        // 字符串类型或者是字符串数组、集合类型
        else if (isStringIterable(value)) {
            List<Resource> resourceList = new ArrayList<>();
            if (ContainerUtils.isIterable(value)) {
                Iterator<Object> iterator = ContainerUtils.getIterator(value);
                while (iterator.hasNext()) {
                    resourceList.addAll(Arrays.asList(ConversionUtils.conversion(iterator.next(), Resource[].class)));
                }
            } else {
                resourceList.addAll(Arrays.asList(ConversionUtils.conversion(value, Resource[].class)));
            }
            httpFiles = HttpExecutor.toHttpFiles(toHttpResources(resourceList));
            fileNameHandled = false;
        }
        // byte[]、Byte[]、InputStream、InputStreamSource对应的Map
        else if (Map.class.isAssignableFrom(value.getClass())) {
            List<HttpFile> httpFileList = new ArrayList<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String mKey = context.parseExpression(String.valueOf(entry.getKey()), String.class);
                Object mValue = entry.getValue();

                if (isHttpFileObject(mValue)) {
                    httpFileList.add(toHttpFile(mValue, resolveFileName(mKey, 0)));
                } else {
                    Class<?> elementType = ContainerUtils.getElementType(mValue);
                    if (isHttpFileType(elementType)) {
                        if (ContainerUtils.isIterable(mValue)) {
                            Iterator<Object> iterator = ContainerUtils.getIterator(mValue);
                            int i = 0;
                            while (iterator.hasNext()) {
                                httpFileList.add(toHttpFile(iterator.next(), resolveFileName(mKey, i++)));
                            }
                        } else {
                            httpFileList.add(toHttpFile(mValue, resolveFileName(mKey, 0)));
                        }
                    } else {
                        throw new IllegalArgumentException(StringUtils.format("The value '{}' corresponding to the key named '{}' in the Map cannot be converted to HttpFile type.", ClassUtils.getClassName(mValue), mKey));
                    }
                }
            }
            httpFiles = httpFileList.toArray(new HttpFile[0]);
            fileNameHandled = true;
        }
        // byte[]、Byte[]、InputStream、InputStreamSource系列
        else {
            if (!StringUtils.hasText(fileName)) {
                fileName = NanoIdUtils.randomNanoId();
            }
            if (isHttpFileObject(value)) {
                httpFiles = new HttpFile[]{toHttpFile(value, resolveFileName(fileName, 0))};
            } else {
                Class<?> elementType = ContainerUtils.getElementType(value);
                if (isHttpFileType(elementType)) {
                    if (ContainerUtils.isIterable(value)) {
                        List<HttpFile> httpFileList = new ArrayList<>();
                        Iterator<Object> iterator = ContainerUtils.getIterator(value);
                        int i = 0;
                        while (iterator.hasNext()) {
                            httpFileList.add(toHttpFile(iterator.next(), resolveFileName(fileName, i++)));
                        }
                        httpFiles = httpFileList.toArray(new HttpFile[0]);
                    } else {
                        httpFiles = new HttpFile[]{toHttpFile(value, resolveFileName(fileName, 0))};
                    }
                } else {
                    throw new IllegalArgumentException(StringUtils.format("The '{}' type parameter cannot be converted to HttpFile", ClassUtils.getClassName(value)));
                }
            }
            fileNameHandled = true;
        }
        // 资源类型自带文件名时，允许用注解上的fileName覆盖；Map传参以key为文件名，不参与覆盖
        if (!fileNameHandled && StringUtils.hasText(fileName)) {
            httpFiles = applyFileName(httpFiles, fileName);
        }
        // 注解上显式声明了Content-Type时，统一覆盖本次所有文件的Content-Type
        String contentType = context.parseExpression(context.toAnnotation(MultiFile.class).contentType(), String.class);
        if (StringUtils.hasText(contentType)) {
            httpFiles = applyContentType(httpFiles, contentType);
        }
        return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), httpFiles));
    }

    /**
     * 应用注解上声明的Content-Type，返回新的{@link HttpFile}数组，不修改调用方传入的对象
     *
     * @param httpFiles   原始文件数组
     * @param contentType 要应用的Content-Type
     * @return 应用Content-Type后的新数组
     */
    private HttpFile[] applyContentType(HttpFile[] httpFiles, String contentType) {
        HttpFile[] result = new HttpFile[httpFiles.length];
        for (int i = 0; i < httpFiles.length; i++) {
            HttpFile httpFile = httpFiles[i];
            result[i] = new HttpFile(httpFile, httpFile::getFileName, contentType, httpFile.getDescriptor());
        }
        return result;
    }

    /**
     * 应用注解上声明的文件名，返回新的{@link HttpFile}数组，不修改调用方传入的对象
     *
     * @param httpFiles        原始文件数组
     * @param fileNameTemplate 文件名模板，可使用{@code {_index_}}占位符
     * @return 应用文件名后的新数组
     */
    private HttpFile[] applyFileName(HttpFile[] httpFiles, String fileNameTemplate) {
        HttpFile[] result = new HttpFile[httpFiles.length];
        for (int i = 0; i < httpFiles.length; i++) {
            HttpFile httpFile = httpFiles[i];
            String useFileName = resolveFileName(fileNameTemplate, i);
            result[i] = new HttpFile(httpFile, () -> useFileName, httpFile.getContentType(), httpFile.getDescriptor());
        }
        return result;
    }

    /**
     * 替换文件名模板中的下标占位符
     *
     * @param fileNameTemplate 文件名模板
     * @param index            当前下标
     * @return 替换后的文件名
     */
    private String resolveFileName(String fileNameTemplate, int index) {
        return fileNameTemplate.replace(INDEX_PLACEHOLDER, String.valueOf(index));
    }

    /**
     * 判断参数是否为空的数组、集合或Iterator
     * <p>{@code byte[]}与{@code Byte[]}表示的是单个文件的内容，即便长度为0也要照常上传，因此不在此列</p>
     *
     * @param value 参数值
     * @return 是否为空容器
     */
    private boolean isEmptyContainer(Object value) {
        if (value instanceof byte[] || value instanceof Byte[]) {
            return false;
        }
        return ContainerUtils.isIterable(value) && !ContainerUtils.getIterator(value).hasNext();
    }

    /**
     * 把http/https协议的资源包装为{@link HttpResource}，使其能按响应头补全文件名后缀
     * <p>处理方式与编程式API {@code RequestParameter#addResources(String, Resource...)} 保持一致</p>
     *
     * @param resources 由字符串转换得到的资源列表
     * @return 包装后的资源数组
     */
    private Resource[] toHttpResources(List<Resource> resources) {
        Resource[] result = new Resource[resources.size()];
        for (int i = 0; i < resources.size(); i++) {
            Resource resource = resources.get(i);
            try {
                String protocol = resource.getURL().getProtocol();
                if (("http".equals(protocol) || "https".equals(protocol)) && resource instanceof UrlResource) {
                    result[i] = new HttpResource((UrlResource) resource);
                } else {
                    result[i] = resource;
                }
            } catch (IOException e) {
                throw new ParameterConvertException(e, "Unable to convert resource '{}' to HTTP resource.", resource.toString());
            }
        }
        return result;
    }

    /**
     * 把{@link Iterator}类型的参数物化为{@link List}
     * <p>
     * Iterator只能单向消费，而下面的分支判定（{@code getElementType}）与遍历都会再次获取迭代器，
     * 直接使用会导致首个元素被类型判定消耗掉、进而少上传一个文件，故先整体物化。
     * 同时实现了{@link Iterable}的对象不受影响，因为每次都能获取到新的迭代器。
     *
     * @param value 参数值
     * @return 物化后的参数值，非Iterator类型时原样返回
     */
    private Object materializeIterator(Object value) {
        if (!(value instanceof Iterator) || value instanceof Iterable) {
            return value;
        }
        List<Object> elementList = new ArrayList<>();
        Iterator<?> iterator = (Iterator<?>) value;
        while (iterator.hasNext()) {
            elementList.add(iterator.next());
        }
        return elementList;
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
        } else if (object instanceof HttpFile) {
            // HttpFile与MultipartFile都实现了InputStreamSource，必须优先处理以保留其Content-Type
            HttpFile httpFile = (HttpFile) object;
            return new HttpFile(httpFile, () -> fileName, httpFile.getContentType(), httpFile.getDescriptor());
        } else if (object instanceof MultipartFile) {
            MultipartFile multipartFile = (MultipartFile) object;
            return new HttpFile(multipartFile, fileName, multipartFile.getContentType());
        } else if (object instanceof InputStreamSource) {
            return new HttpFile((InputStreamSource) object, () -> fileName, "[InputStreamSource] " + fileName);
        } else if (object instanceof byte[]) {
            return new HttpFile(((byte[]) object), fileName);
        } else if (object instanceof Byte[]) {
            return new HttpFile(toBytes((Byte[]) object), fileName);
        } else if (object instanceof ByteBuffer || object instanceof Reader) {
            return new HttpFile(HttpExecutor.toByte(object), fileName);
        } else {
            throw new IllegalArgumentException(StringUtils.format("The '{}' type parameter cannot be converted to HttpFile", object.getClass().getName()));
        }
    }

    private boolean isHttpFileObject(Object value) {
        return value instanceof byte[] ||
                value instanceof Byte[] ||
                value instanceof ByteBuffer ||
                value instanceof Reader ||
                value instanceof InputStreamSource ||
                value instanceof InputStream;
    }

    private boolean isHttpFileType(Class<?> clazz) {
        return clazz == byte[].class ||
                clazz == Byte[].class ||
                ByteBuffer.class.isAssignableFrom(clazz) ||
                Reader.class.isAssignableFrom(clazz) ||
                InputStreamSource.class.isAssignableFrom(clazz) ||
                InputStream.class.isAssignableFrom(clazz);
    }

    private boolean isStringIterable(Object value) {
        return ContainerUtils.getElementType(value) == String.class;
    }
}
