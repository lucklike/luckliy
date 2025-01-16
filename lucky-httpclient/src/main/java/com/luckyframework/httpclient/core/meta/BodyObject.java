package com.luckyframework.httpclient.core.meta;


import com.luckyframework.common.StringUtils;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.XML_SCHEME;

/**
 * Body参数
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/30 10:24 上午
 */
public class BodyObject {

    /**
     * Content-Type
     */
    private final ContentType contentType;

    /**
     * body InputStreamSource
     */
    private InputStreamSource bodyStreamSource;

    /**
     * body内容
     */
    private byte[] bodyBytes;

    /**
     * String格式转换器
     */
    private final Supplier<String> stringSupplier;

    BodyObject(ContentType contentType, InputStreamSource bodyStreamSource, Supplier<String> stringSupplier) {
        this.contentType = contentType;
        this.bodyStreamSource = bodyStreamSource;
        this.stringSupplier = stringSupplier == null ? new InputStreamSourceStringSupplier(bodyStreamSource, contentType) : stringSupplier;
    }

    BodyObject(ContentType contentType, InputStreamSource bodyStreamSource) {
        this(contentType, bodyStreamSource, new InputStreamSourceStringSupplier(bodyStreamSource, contentType));
    }

    BodyObject(ContentType contentType, byte[] bodyBytes, Supplier<String> stringSupplier) {
        this.contentType = contentType;
        this.bodyBytes = bodyBytes;
        this.stringSupplier = stringSupplier == null ? new BinaryStringSupplier(bodyBytes.length, contentType) : stringSupplier;
    }

    BodyObject(ContentType contentType, byte[] bodyBytes) {
        this(contentType, bodyBytes, new BinaryStringSupplier(bodyBytes.length, contentType));
    }

    BodyObject(ContentType contentType, String stringBody, Supplier<String> stringSupplier) {
        this.contentType = contentType;
        this.bodyBytes = stringBody.getBytes(getCharset());
        this.stringSupplier = stringSupplier == null ? () -> stringBody : stringSupplier;
    }

    BodyObject(ContentType contentType, String stringBody) {
        this(contentType, stringBody, () -> stringBody);
    }


    //-------------------------------------------------------------------------------------
    //                                StreamSource Body
    //-------------------------------------------------------------------------------------


    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType      Content-Type
     * @param bodyStreamSource body内容
     * @param stringSupplier   String格式转换器
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, InputStreamSource bodyStreamSource, Supplier<String> stringSupplier) {
        return new BodyObject(contentType, bodyStreamSource, stringSupplier);
    }

    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType      Content-Type
     * @param bodyStreamSource body内容
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, InputStreamSource bodyStreamSource) {
        return new BodyObject(contentType, bodyStreamSource);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType         Mime类型
     * @param charset          编码字符集
     * @param bodyStreamSource 流式请求体
     * @param stringSupplier   String格式转换器
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, InputStreamSource bodyStreamSource, Supplier<String> stringSupplier) {
        return builder(new ContentType(mimeType, charset), bodyStreamSource, stringSupplier);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType         Mime类型
     * @param charset          编码字符集
     * @param bodyStreamSource 流式请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, InputStreamSource bodyStreamSource) {
        return builder(new ContentType(mimeType, charset), bodyStreamSource);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType         Mime类型
     * @param charset          编码字符集
     * @param bodyStreamSource 流式请求体
     * @param stringSupplier   String格式转换器
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, String charset, InputStreamSource bodyStreamSource, Supplier<String> stringSupplier) {
        return builder(mimeType, Charset.forName(charset), bodyStreamSource, stringSupplier);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType         Mime类型
     * @param charset          编码字符集
     * @param bodyStreamSource 流式请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, String charset, InputStreamSource bodyStreamSource) {
        return builder(mimeType, Charset.forName(charset), bodyStreamSource);
    }

    //-------------------------------------------------------------------------------------
    //                                Stream Body
    //-------------------------------------------------------------------------------------

    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType    Content-Type
     * @param bodyStream     body内容
     * @param stringSupplier String格式转换器
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, InputStream bodyStream, Supplier<String> stringSupplier) {
        return builder(contentType, () -> bodyStream, stringSupplier);
    }

    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType Content-Type
     * @param bodyStream  body内容
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, InputStream bodyStream) {
        return builder(contentType, () -> bodyStream);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType       Mime类型
     * @param charset        编码字符集
     * @param bodyStream     流式请求体
     * @param stringSupplier String格式转换器
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, InputStream bodyStream, Supplier<String> stringSupplier) {
        return builder(new ContentType(mimeType, charset), bodyStream, stringSupplier);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType   Mime类型
     * @param charset    编码字符集
     * @param bodyStream 流式请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, InputStream bodyStream) {
        return builder(new ContentType(mimeType, charset), bodyStream);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType       Mime类型
     * @param charset        编码字符集
     * @param bodyStream     流式请求体
     * @param stringSupplier String格式转换器
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, String charset, InputStream bodyStream, Supplier<String> stringSupplier) {
        return builder(mimeType, Charset.forName(charset), bodyStream, stringSupplier);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType   Mime类型
     * @param charset    编码字符集
     * @param bodyStream 流式请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, String charset, InputStream bodyStream) {
        return builder(mimeType, Charset.forName(charset), bodyStream);
    }

    //-------------------------------------------------------------------------------------
    //                                Byte Body
    //-------------------------------------------------------------------------------------

    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType    Content-Type
     * @param body           body内容
     * @param stringSupplier String格式转换器
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, byte[] body, Supplier<String> stringSupplier) {
        return new BodyObject(contentType, body, stringSupplier);
    }

    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType Content-Type
     * @param body        body内容
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, byte[] body) {
        return new BodyObject(contentType, body);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType       Mime类型
     * @param charset        编码字符集
     * @param byteBody       字节数组请求体
     * @param stringSupplier String格式转换器
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, byte[] byteBody, Supplier<String> stringSupplier) {
        return builder(new ContentType(mimeType, charset), byteBody, stringSupplier);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType Mime类型
     * @param charset  编码字符集
     * @param byteBody 字节数组请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, byte[] byteBody) {
        return builder(new ContentType(mimeType, charset), byteBody);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType       Mime类型
     * @param charset        编码字符集
     * @param byteBody       字节数组请求体
     * @param stringSupplier String格式转换器
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, String charset, byte[] byteBody, Supplier<String> stringSupplier) {
        return builder(mimeType, Charset.forName(charset), byteBody, stringSupplier);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType Mime类型
     * @param charset  编码字符集
     * @param byteBody 字节数组请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, String charset, byte[] byteBody) {
        return builder(mimeType, Charset.forName(charset), byteBody);
    }

    //-------------------------------------------------------------------------------------
    //                                String Body
    //-------------------------------------------------------------------------------------

    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType    Content-Type
     * @param body           body内容
     * @param stringSupplier String格式转换器
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, String body, Supplier<String> stringSupplier) {
        return new BodyObject(contentType, body, stringSupplier);
    }

    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType Content-Type
     * @param body        body内容
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, String body) {
        return new BodyObject(contentType, body);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType       Mime类型
     * @param charset        编码字符集
     * @param stringBody     字符串请求体
     * @param stringSupplier String格式转换器
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, String stringBody, Supplier<String> stringSupplier) {
        return builder(new ContentType(mimeType, charset), stringBody, stringSupplier);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType   Mime类型
     * @param charset    编码字符集
     * @param stringBody 字符串请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, String stringBody) {
        return builder(new ContentType(mimeType, charset), stringBody);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType       Mime类型
     * @param charset        编码字符集
     * @param stringBody     字符串请求体
     * @param stringSupplier String格式转换器
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, String charset, String stringBody, Supplier<String> stringSupplier) {
        return builder(mimeType, Charset.forName(charset), stringBody, stringSupplier);
    }

    /**
     * 构建一个BodyObject
     *
     * @param mimeType   Mime类型
     * @param charset    编码字符集
     * @param stringBody 字符串请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, String charset, String stringBody) {
        return builder(mimeType, Charset.forName(charset), stringBody);
    }

    //-------------------------------------------------------------------------------------
    //                                Json Body
    //-------------------------------------------------------------------------------------

    /**
     * 返回Json格式的BodyObject
     *
     * @param jsonBody JSON字符串参数
     * @return Json格式的BodyObject
     */
    public static BodyObject jsonBody(String jsonBody) {
        return new BodyObject(ContentType.APPLICATION_JSON, jsonBody);
    }

    /**
     * 返回Json格式的BodyObject
     *
     * @param jsonBody 可序列化为JSON字符的对象
     * @return Json格式的BodyObject
     */
    public static BodyObject jsonBody(Object jsonBody) {
        try {
            return new BodyObject(ContentType.APPLICATION_JSON, JSON_SCHEME.serialization(jsonBody));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    //-------------------------------------------------------------------------------------
    //                                Xml Body
    //-------------------------------------------------------------------------------------

    /**
     * 返回XML格式的BodyObject
     *
     * @param xmlBody XML字符串参数
     * @return XML格式的BodyObject
     */
    public static BodyObject xmlBody(String xmlBody) {
        return new BodyObject(ContentType.APPLICATION_XML, xmlBody);
    }

    /**
     * 返回XML格式的BodyObject
     *
     * @param xmlBody 可序列化为XML字符的对象
     * @return XML格式的BodyObject
     */
    public static BodyObject xmlBody(Object xmlBody) {
        try {
            return new BodyObject(ContentType.APPLICATION_XML, XML_SCHEME.serialization(xmlBody));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    //-------------------------------------------------------------------------------------
    //                                Binary Body
    //-------------------------------------------------------------------------------------

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param bytes          二进制数据
     * @param stringSupplier String格式转换器
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(byte[] bytes, Supplier<String> stringSupplier) {
        return new BodyObject(ContentType.APPLICATION_OCTET_STREAM, bytes, stringSupplier);
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param bytes 二进制数据
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(byte[] bytes) {
        return new BodyObject(ContentType.APPLICATION_OCTET_STREAM, bytes);
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param file 文件对象
     * @param defaultContentType 默认的Content-Type
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(File file, ContentType defaultContentType) {
        String mimeType = ContentTypeUtils.getMimeType(file.getName());
        ContentType contentType = mimeType == null ? defaultContentType : ContentType.create(mimeType, (Charset) null);
        return builder(
                contentType,
                () -> Files.newInputStream(file.toPath()),
                () -> StringUtils.format("File Body ({}) {}", UnitUtils.byteTo(file.length()), file.getAbsolutePath())
        );
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param file 文件对象
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(File file) {
        return binaryBody(file, ContentType.APPLICATION_OCTET_STREAM);
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param in             输入流
     * @param stringSupplier String格式转换器
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(InputStream in, Supplier<String> stringSupplier) {
        return builder(ContentType.APPLICATION_OCTET_STREAM, in, stringSupplier);
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param in 输入流
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(InputStream in) {
        return builder(ContentType.APPLICATION_OCTET_STREAM, in);
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param multipartFile MultipartFile
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(MultipartFile multipartFile) {
        String originalFileName = multipartFile.getOriginalFileName();
        String mimeType = ContentTypeUtils.getMimeType(originalFileName);
        ContentType contentType = mimeType == null ? ContentType.APPLICATION_OCTET_STREAM : ContentType.create(mimeType, (Charset) null);
        return builder(
                contentType,
                multipartFile,
                () -> {
                    try {
                        return StringUtils.format("MultipartFile Body ({}) {}", UnitUtils.byteTo(multipartFile.getSize()), multipartFile.getOriginalFileName());
                    } catch (IOException e) {
                        throw new LuckyRuntimeException(e);
                    }
                }
        );
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param streamSource   Spring资源类型参数
     * @param stringSupplier String格式转换器
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(InputStreamSource streamSource, Supplier<String> stringSupplier) {
        return builder(ContentType.APPLICATION_OCTET_STREAM, streamSource, stringSupplier);
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param streamSource Spring资源类型参数
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(InputStreamSource streamSource) {
        return builder(ContentType.APPLICATION_OCTET_STREAM, streamSource);
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param resourceLocation 资源路径
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(String resourceLocation) {
        return binaryBody(ConversionUtils.conversion(resourceLocation, Resource.class));
    }

    //-------------------------------------------------------------------------------------
    //                                Java Body
    //-------------------------------------------------------------------------------------

    /**
     * 返回Java序列化格式的BodyObject
     *
     * @param serializable 资源路径
     * @return Java序列化格式的BodyObject
     */
    public static BodyObject javaBody(Serializable serializable) {
        try {
            return builder(ContentType.APPLICATION_JAVA_SERIALIZED_OBJECT, JDK_SCHEME.toByte(serializable), serializable::toString);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 获取Content-Type
     *
     * @return Content-Type
     */
    public ContentType getContentType() {
        return contentType;
    }

    public Charset getCharset() {
        return (contentType == null || contentType.getCharset() == null) ? StandardCharsets.UTF_8 : contentType.getCharset();
    }

    /**
     * 获取body参数内容
     *
     * @return body参数内容
     */
    public synchronized byte[] getBody() {
        if (bodyBytes == null) {
            try {
                bodyBytes = FileCopyUtils.copyToByteArray(bodyStreamSource.getInputStream());
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }
        return bodyBytes;
    }

    /**
     * 获取Body流
     *
     * @return body流
     */
    public synchronized InputStream getBodyStream() {
        if (bodyBytes == null) {
            try {
                return bodyStreamSource.getInputStream();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }
        return new ByteArrayInputStream(bodyBytes);
    }

    /**
     * 获取String形式的body内容
     *
     * @return String形式的body内容
     */
    public String getBodyAsString() {
        if (stringSupplier != null) {
            return stringSupplier.get();
        }
        return new String(getBody(), getCharset());
    }

    @Override
    public String toString() {
        return stringSupplier.get();
    }

    /**
     * 针对{@link InputStreamSource} 的String格式转换器
     */
    static class InputStreamSourceStringSupplier implements Supplier<String> {

        private final InputStreamSource source;
        private final ContentType contentType;

        public InputStreamSourceStringSupplier(InputStreamSource source, ContentType contentType) {
            this.source = source;
            this.contentType = contentType;
        }

        @Override
        public String get() {
            if (source instanceof MultipartFile) {
                try {
                    MultipartFile multipartFile = (MultipartFile) source;
                    return StringUtils.format("{} Body ({}) {}", ClassUtils.getClassSimpleName(source), UnitUtils.byteTo(multipartFile.getSize()), multipartFile.getOriginalFileName());
                } catch (IOException e) {
                    throw new LuckyRuntimeException(e);
                }
            }

            if (source instanceof Resource) {
                try {
                    Resource resource = (Resource) source;
                    return StringUtils.format("{} Body ({}) {}", ClassUtils.getClassSimpleName(source), UnitUtils.byteTo(resource.contentLength()), resource.getDescription());
                } catch (IOException e) {
                    throw new LuckyRuntimeException(e);
                }
            }

            return StringUtils.format("{} Body '{}' ", ClassUtils.getClassSimpleName(source), contentType.getMimeType());
        }
    }

    static class BinaryStringSupplier implements Supplier<String> {

        private final int length;
        private final ContentType contentType;

        BinaryStringSupplier(int length, ContentType contentType) {
            this.length = length;
            this.contentType = contentType;
        }

        @Override
        public String get() {
            return StringUtils.format("Binary Body '{}' size: {}", contentType.getMimeType(), UnitUtils.byteTo(length));
        }
    }
}
