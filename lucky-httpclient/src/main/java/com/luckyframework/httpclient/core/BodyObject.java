package com.luckyframework.httpclient.core;


import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.serializable.SerializationException;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.luckyframework.httpclient.core.SerializationConstant.*;

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
     * body内容
     */
    private final byte[] body;

    BodyObject(ContentType contentType, String body) {
        this.contentType = contentType;
        this.body = body.getBytes(getCharset());
    }

    BodyObject(ContentType contentType, byte[] body) {
        this.contentType = contentType;
        this.body = body;
    }

    public static BodyObject builder(String mimeType, Charset charset, byte[] body) {
        return new BodyObject(new ContentType(mimeType, charset), body);
    }

    public static BodyObject builder(String mimeType, Charset charset, String body) {
        return new BodyObject(new ContentType(mimeType, charset), body);
    }

    public static BodyObject builder(String mimeType, String charset, String body) {
        return builder(mimeType, Charset.forName(charset), body);
    }

    public static BodyObject builder(String mimeType, String charset, byte[] body) {
        return builder(mimeType, Charset.forName(charset), body);
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

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param bytes 二进制数据
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject byteBody(byte[] bytes) {
        return new BodyObject(ContentType.APPLICATION_OCTET_STREAM, bytes);
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param file 文件对象
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject byteBody(File file) {
        try {
            return new BodyObject(ContentType.APPLICATION_OCTET_STREAM, FileCopyUtils.copyToByteArray(file));
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
    public static BodyObject byteBody(InputStream in) {
        try {
            return new BodyObject(ContentType.APPLICATION_OCTET_STREAM, FileCopyUtils.copyToByteArray(in));
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
    public static BodyObject byteBody(MultipartFile multipartFile) {
        try {
            return new BodyObject(ContentType.APPLICATION_OCTET_STREAM, FileCopyUtils.copyToByteArray(multipartFile.getInputStream()));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 返回二进制流格式的BodyObject
     *
     * @param resource Spring资源类型参数
     * @return 二进制流格式的BodyObject
     */
    public static BodyObject byteBody(Resource resource) {
        try {
            return byteBody(resource.getInputStream());
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
    public static BodyObject byteBody(String resourceLocation) {
        try {
            return byteBody(ConversionUtils.conversion(resourceLocation, Resource.class));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 返回Java序列化格式的BodyObject
     *
     * @param serializable 资源路径
     * @return Java序列化格式的BodyObject
     */
    public static BodyObject javaBody(Serializable serializable) {
        try {
            return new BodyObject(ContentType.APPLICATION_JAVA_SERIALIZED_OBJECT, JDK_SCHEME.toByte(serializable));
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
    public byte[] getBody() {
        return body;
    }

    /**
     * 获取String形式的body内容
     *
     * @return String形式的body内容
     */
    public String getBodyAsString() {
        return new String(getBody(), getCharset());
    }

    @Override
    public String toString() {
        return StringUtils.format("[{0}] {1}", contentType, getBodyAsString());
    }
}
