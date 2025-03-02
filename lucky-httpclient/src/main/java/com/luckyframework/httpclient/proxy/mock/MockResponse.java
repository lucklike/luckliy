package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.ClientCookie;
import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.core.meta.ResponseMetaData;
import com.luckyframework.httpclient.proxy.CommonFunctions;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static com.luckyframework.httpclient.core.meta.HttpHeaders.CONTENT_DISPOSITION;
import static com.luckyframework.httpclient.core.meta.HttpHeaders.CONTENT_LENGTH;
import static com.luckyframework.httpclient.core.meta.HttpHeaders.RESPONSE_COOKIE;

/**
 * Mock Response
 */
public class MockResponse implements Response, RequestAware {

    /**
     * 当前请求实例
     */
    private Request request;

    /**
     * HTTP状态值
     */
    private int status;

    /**
     * 响应头管理器
     */
    private HttpHeaderManager headers;

    /**
     * 响应体对应的Stream
     */
    private InputStream bodyStream;

    /**
     * 响应体对应的byte[]
     */
    private byte[] bodyBytes;


    /**
     * 私有构造器
     */
    private MockResponse() {
    }

    /**
     * 创建一个MockResponse对象
     *
     * @return MockResponse对象
     */
    public static MockResponse create() {
        MockResponse mockResponse = new MockResponse();
        mockResponse.status = 200;
        mockResponse.headers = new DefaultHttpHeaderManager();
        mockResponse.bodyBytes = new byte[0];
        return mockResponse;
    }

    /**
     * 设置请求对象{@link Request}
     *
     * @param request 请求对象
     * @return this
     */
    public MockResponse request(Request request) {
        this.request = request;
        return this;
    }

    /**
     * 设置HTTP状态码
     *
     * @param status HTTP状态码
     * @return this
     */
    public MockResponse status(int status) {
        this.status = status;
        return this;
    }

    /**
     * 添加响应头
     *
     * @param name  名称
     * @param value 值
     * @return this
     */
    public MockResponse header(String name, Object value) {
        this.headers.addHeader(name, value);
        return this;
    }

    /**
     * 设置响应头
     *
     * @param name  名称
     * @param value 值
     * @return this
     */
    public MockResponse setHeader(String name, Object value) {
        this.headers.removerHeader(name);
        this.headers.setHeader(name, value);
        return this;
    }

    /**
     * 添加Content-Type响应头
     *
     * @param contentType Content-Type响应头值
     * @return this
     */
    public MockResponse contentType(String contentType) {
        return setHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }

    /**
     * 添加Content-Type响应头
     *
     * @param contentType {@link ContentType}
     * @return this
     */
    public MockResponse contentType(ContentType contentType) {
        return contentType(contentType.toString());
    }

    /**
     * 添加Content-Length响应头
     *
     * @param contentLength Content-Length响应头值
     * @return this
     */
    public MockResponse contentLength(long contentLength) {
        return setHeader(CONTENT_LENGTH, contentLength);
    }

    /**
     * 添加Content-Disposition响应头
     *
     * @param filename 文件名称
     * @return this
     */
    public MockResponse contentDisposition(String filename) {
        return setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
    }

    /**
     * 添加Cookie
     *
     * @param cookie 字符串形式的Cookie
     * @return this
     */
    public MockResponse cookie(String cookie) {
        return header(RESPONSE_COOKIE, cookie);
    }

    /**
     * 添加Cookie
     *
     * @param cookie {@link ClientCookie}
     * @return this
     */
    public MockResponse cookie(ClientCookie cookie) {
        return cookie(cookie.toString());
    }

    /**
     * 添加{@link InputStream}类型的响应体
     *
     * @param bodyStream {@link InputStream}类型的响应体
     * @return this
     */
    public MockResponse body(InputStream bodyStream) {
        this.bodyStream = bodyStream;
        this.bodyBytes = null;
        return this;
    }

    /**
     * 添加{@link byte[]}类型的响应体
     *
     * @param bodyBytes {@link byte[]}类型的响应体
     * @return this
     */
    public MockResponse body(byte[] bodyBytes) {
        this.bodyBytes = bodyBytes;
        contentLength(bodyBytes.length);
        return this;
    }

    /**
     * 添加{@link String}类型的响应体
     *
     * @param body {@link String}类型的响应体
     * @return this
     */
    public MockResponse body(String body) {
        return body(body.getBytes());
    }

    /**
     * 添加{@link String}类型的响应体，并设置Content-Type为<b>'text/plain'</b>
     *
     * @param body {@link String}类型的响应体
     * @return this
     */
    public MockResponse txt(String body) {
        return body(body.getBytes()).contentType("text/plain");
    }

    /**
     * 添加{@link String}类型的响应体，并设置Content-Type为<b>'text/html'</b>
     *
     * @param body {@link String}类型的响应体
     * @return this
     */
    public MockResponse html(String body) {
        return body(body.getBytes()).contentType("text/html");
    }

    /**
     * 添加一个适用于SSE请求的响应数据
     *
     * @param sseMock SSE响应数据
     * @return this
     */
    public MockResponse sse(SseMock sseMock) {
        return body(sseMock.getTxtStream()).contentType("text/event-stream");
    }

    /**
     * 添加一个ndjson模拟响应体
     *
     * @param ndJsonMock ndjson模拟请求体
     * @return this
     */
    public MockResponse ndjson(NdJsonMock ndJsonMock) {
        return body(ndJsonMock.getTxtStream()).contentType("application/x-ndjson");
    }

    /**
     * 添加JSON类型的响应体，并设置Content-Type为<b>'application/json'</b>
     *
     * @param jsonObject JSON对象
     * @return this
     */
    public MockResponse json(Object jsonObject) {
        String json;
        if (jsonObject instanceof String) {
            json = (String) jsonObject;
        } else {
            try {
                json = CommonFunctions.json(jsonObject);
            } catch (Exception e) {
                throw new SerializationException(e);
            }
        }
        return body(json).contentType(ContentType.APPLICATION_JSON);
    }

    /**
     * 添加XML类型的响应体，并设置Content-Type为<b>'application/xml'</b>
     *
     * @param xmlObject XML对象
     * @return this
     */
    public MockResponse xml(Object xmlObject) {
        String xml;
        if (xmlObject instanceof String) {
            xml = (String) xmlObject;
        } else {
            try {
                xml = CommonFunctions.xml(xmlObject);
            } catch (Exception e) {
                throw new SerializationException(e);
            }
        }
        return body(xml).contentType(ContentType.APPLICATION_XML);
    }

    /**
     * 添加JDK序列化类型的响应体，并设置Content-Type为<b>'application/x-java-serialized-object'</b>
     *
     * @param javaObject JDK序列化对象
     * @return this
     */
    public MockResponse java(Serializable javaObject) {
        try {
            return body(CommonFunctions.java(javaObject))
                    .contentType(ContentType.APPLICATION_JAVA_SERIALIZED_OBJECT);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 添加GoogleProtobuf类型的响应体，并设置Content-Type为<b>'application/x-protobuf'</b>
     *
     * @param protobufObject GoogleProtobuf序列化对象
     * @return this
     */
    public MockResponse protobuf(Object protobufObject) {
        try {
            return body(CommonFunctions.protobuf(protobufObject))
                    .contentType(ContentType.APPLICATION_PROTOBUF);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 添加一个{@link File}类型的响应体，Content-Type会自动设置为文件类型对应的值，Content-Disposition中会添加文件名
     *
     * @param file 文件对象
     * @return this
     */
    public MockResponse file(File file) {
        try {
            return body(Files.newInputStream(file.toPath()))
                    .contentType(getFileContentType(file.getName()))
                    .contentLength(file.length())
                    .contentDisposition(file.getName());
        } catch (Exception e) {
            throw new LuckyRuntimeException(e);
        }
    }

    /**
     * 添加一个{@link File}类型的响应体，Content-Type会自动设置为文件类型对应的值，Content-Disposition中会添加文件名
     *
     * @param filePath 文件路径
     * @return this
     */
    public MockResponse file(String filePath) {
        return file(new File(filePath));
    }

    /**
     * 添加一个{@link Resource}类型的响应体，Content-Type会自动设置为文件类型对应的值，Content-Disposition中会添加文件名
     *
     * @param resource 资源对象
     * @return this
     */
    public MockResponse resource(Resource resource) {
        try {
            if (resource.isFile()) {
                return file(resource.getFile());
            }
            return body(resource.getInputStream())
                    .contentType(getFileContentType(resource.getFilename()))
                    .contentDisposition(resource.getFilename());
        } catch (Exception e) {
            throw new LuckyRuntimeException(e);
        }
    }

    /**
     * 添加一个{@link Resource}类型的响应体，Content-Type会自动设置为文件类型对应的值，Content-Disposition中会添加文件名
     *
     * @param resourceLocation 资源表达式
     * @return this
     */
    public MockResponse resource(String resourceLocation) {
        return resource(ConversionUtils.conversion(resourceLocation, Resource.class));
    }

    /**
     * 文件名转化为mimeType
     *
     * @param fileName 文件名
     * @return mimeType
     */
    public ContentType getFileContentType(String fileName) {
        String mimeType = ContentTypeUtils.getMimeType(fileName);
        return mimeType == null ? ContentType.APPLICATION_OCTET_STREAM : ContentType.create(mimeType, (Charset) null);
    }

    @Override
    public synchronized byte[] getResult() {
        if (bodyBytes == null) {
            try {
                bodyBytes = FileCopyUtils.copyToByteArray(bodyStream);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }
        return bodyBytes;
    }

    @Override
    public InputStream getInputStream() {
        if (bodyBytes == null) {
            return this.bodyStream;
        }
        return new ByteArrayInputStream(bodyBytes);
    }

    @Override
    public ResponseMetaData getResponseMetaData() {
        return new ResponseMetaData(request, status, headers, this::getInputStream);
    }

    @Override
    public void closeResource() {
        try {
            if (bodyBytes == null) {
                getInputStream().close();
            }
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public void setRequest(Request request) {
        this.request = request;
    }
}
