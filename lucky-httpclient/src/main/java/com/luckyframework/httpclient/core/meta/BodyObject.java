package com.luckyframework.httpclient.core.meta;


import com.luckyframework.common.StringUtils;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.io.MultipartFile;
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
     * 内容长度
     */
    private final int contentLength;
    /**
     * body流
     */
    private InputStream bodyStream;

    /**
     * body内容
     */
    private byte[] bodyBytes;

    /**
     * String格式转换器
     */
    private Supplier<String> stringSupplier;

    BodyObject(ContentType contentType, InputStream bodyStream) {
        this.contentType = contentType;
        this.bodyStream = bodyStream;
        try {
            this.contentLength = bodyStream.available();
            stringSupplier = () -> StringUtils.format("InputStream Body '{}' size: {}", contentType.getMimeType(), UnitUtils.byteTo(contentLength));
        } catch (IOException e) {
            throw new LuckyRuntimeException(e, "Failed to get the request body length.");
        }
    }

    BodyObject(ContentType contentType, byte[] bodyBytes) {
        this.contentType = contentType;
        this.bodyBytes = bodyBytes;
        this.contentLength = bodyBytes.length;
        stringSupplier = () -> StringUtils.format("Binary Body '{}' size: {}", contentType.getMimeType(), UnitUtils.byteTo(contentLength));
    }

    BodyObject(ContentType contentType, String stringBody) {
        this.contentType = contentType;
        this.bodyBytes = stringBody.getBytes(getCharset());
        this.contentLength = bodyBytes.length;
        stringSupplier = () -> stringBody;
    }

    //-------------------------------------------------------------------------------------
    //                                Stream Body
    //-------------------------------------------------------------------------------------

    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType Content-Type
     * @param bodyStream  body内容
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, InputStream bodyStream) {
        return new BodyObject(contentType, bodyStream);
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
        return new BodyObject(new ContentType(mimeType, charset), bodyStream);
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
    //                                StreamSource Body
    //-------------------------------------------------------------------------------------


    /**
     * 返回自定义格式的BodyObject
     *
     * @param contentType      Content-Type
     * @param bodyStreamSource body内容
     * @return 自定义格式的BodyObject
     */
    public static BodyObject builder(ContentType contentType, InputStreamSource bodyStreamSource) {
        try {
            BodyObject bodyObject = new BodyObject(contentType, bodyStreamSource.getInputStream());
            if (bodyStreamSource instanceof Resource) {
                bodyObject.setStringSupplier(() -> StringUtils.format("Resource Body ({}) {}", UnitUtils.byteTo(bodyObject.contentLength), ((Resource) bodyStreamSource).getDescription()));
            } else if (bodyStreamSource instanceof MultipartFile) {
                bodyObject.setStringSupplier(() -> StringUtils.format("MultipartFile Body ({}) {}", UnitUtils.byteTo(bodyObject.contentLength), ((MultipartFile) bodyStreamSource).getOriginalFileName()));
            }
            return bodyObject;
        } catch (IOException e) {
            throw new SerializationException(e);
        }

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
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, String charset, InputStreamSource bodyStreamSource) {
        return builder(mimeType, Charset.forName(charset), bodyStreamSource);
    }


    //-------------------------------------------------------------------------------------
    //                                Byte Body
    //-------------------------------------------------------------------------------------

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
     * @param mimeType Mime类型
     * @param charset  编码字符集
     * @param byteBody 字节数组请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, byte[] byteBody) {
        return new BodyObject(new ContentType(mimeType, charset), byteBody);
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
     * @param mimeType   Mime类型
     * @param charset    编码字符集
     * @param stringBody 字符串请求体
     * @return BodyObject
     */
    public static BodyObject builder(String mimeType, Charset charset, String stringBody) {
        return new BodyObject(new ContentType(mimeType, charset), stringBody);
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
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(File file) {
        try {
            String mimeType = ContentTypeUtils.getMimeType(file.getName());
            ContentType contentType = mimeType == null ? ContentType.APPLICATION_OCTET_STREAM : ContentType.create(mimeType, (Charset) null);
            BodyObject bodyObject = new BodyObject(contentType, Files.newInputStream(file.toPath()));
            bodyObject.setStringSupplier(() -> StringUtils.format("File Body ({}) {}", UnitUtils.byteTo(file.length()), file.getAbsolutePath()));
            return bodyObject;
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param in 输入流
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(InputStream in) {
        try {
            return new BodyObject(ContentType.APPLICATION_OCTET_STREAM, in);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param multipartFile MultipartFile
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(MultipartFile multipartFile) {
        try {
            BodyObject bodyObject = new BodyObject(ContentType.APPLICATION_OCTET_STREAM, multipartFile.getInputStream());
            bodyObject.setStringSupplier(() -> {
                try {
                    return StringUtils.format("MultipartFile Body ({}) {}", UnitUtils.byteTo(multipartFile.getSize()), multipartFile.getOriginalFileName());
                } catch (IOException e) {
                    throw new LuckyRuntimeException(e);
                }
            });
            return bodyObject;
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param streamSource Spring资源类型参数
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(InputStreamSource streamSource) {
        try {
            return builder(ContentType.APPLICATION_OCTET_STREAM, streamSource);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param resourceLocation 资源路径
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject binaryBody(String resourceLocation) {
        try {
            return binaryBody(ConversionUtils.conversion(resourceLocation, Resource.class));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
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
            BodyObject bodyObject = new BodyObject(ContentType.APPLICATION_JAVA_SERIALIZED_OBJECT, JDK_SCHEME.toByte(serializable));
            bodyObject.setStringSupplier(serializable::toString);
            return bodyObject;
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 设置String格式转换器
     *
     * @param stringSupplier String格式转换器
     */
    public void setStringSupplier(Supplier<String> stringSupplier) {
        this.stringSupplier = stringSupplier;
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
                bodyBytes = FileCopyUtils.copyToByteArray(bodyStream);
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
            return bodyStream;
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

    /**
     * 获取请求体的长度
     *
     * @return 请求体长度
     */
    public int getBodyLength() {
        return this.contentLength;
    }


    @Override
    public String toString() {
        String body = "";
        if (stringSupplier != null) {
            body = stringSupplier.get();
        }
        return String.format("[Content-Type= %s Content-Length = %s] %s", contentType, contentLength, "\n" + body);
    }
}
