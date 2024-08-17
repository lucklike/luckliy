package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.ClientCookie;
import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
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
import java.nio.charset.Charset;
import java.nio.file.Files;

import static com.luckyframework.httpclient.core.meta.HttpHeaders.CONTENT_DISPOSITION;
import static com.luckyframework.httpclient.core.meta.HttpHeaders.CONTENT_LENGTH;
import static com.luckyframework.httpclient.core.meta.HttpHeaders.RESPONSE_COOKIE;

/**
 * Mock Response
 */
public class MockResponse implements Response {

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


    private MockResponse() {
    }

    public static MockResponse create(Request request) {
        MockResponse mockResponse = new MockResponse();
        mockResponse.request = request;
        mockResponse.status = 200;
        mockResponse.headers = new DefaultHttpHeaderManager();
        mockResponse.bodyStream = new ByteArrayInputStream(new byte[0]);
        return mockResponse;
    }

    public static MockResponse create() {
        return create(null);
    }

    public MockResponse request(Request request) {
        this.request = request;
        return this;
    }

    public MockResponse status(int status) {
        this.status = status;
        return this;
    }

    public MockResponse header(String name, Object value) {
        this.headers.addHeader(name, value);
        return this;
    }

    public MockResponse contentType(String contentType) {
        this.headers.setContentType(contentType);
        return this;
    }

    public MockResponse contentType(ContentType contentType) {
        this.headers.setContentType(contentType);
        return this;
    }

    public MockResponse contentLength(long contentLength) {
        return header(CONTENT_LENGTH, contentLength);
    }

    public MockResponse contentDisposition(String filename) {
        return header(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
    }

    public MockResponse cookie(String cookie) {
        return header(RESPONSE_COOKIE, cookie);
    }

    public MockResponse cookie(ClientCookie cookie) {
        return cookie(cookie.toString());
    }

    public MockResponse body(InputStream bodyStream) {
        this.bodyStream = bodyStream;
        return this;
    }

    public MockResponse body(byte[] bodyBytes) {
        this.bodyStream = new ByteArrayInputStream(bodyBytes);
        contentLength(bodyBytes.length);
        return this;
    }

    public MockResponse body(String body) {
        return body(body.getBytes());
    }

    public MockResponse txt(String body) {
        return body(body.getBytes()).contentType("text/plain");
    }

    public MockResponse html(String body) {
        return body(body.getBytes()).contentType("text/html");
    }

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

    public MockResponse java(Object javaObject) {
        try {
            return body(CommonFunctions.java(javaObject))
                    .contentType(ContentType.APPLICATION_JAVA_SERIALIZED_OBJECT);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    public MockResponse protobuf(Object protobufObject) {
        try {
            return body(CommonFunctions.protobuf(protobufObject))
                    .contentType(ContentType.APPLICATION_PROTOBUF);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

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

    public MockResponse file(String filePath) {
        return file(new File(filePath));
    }

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

    public MockResponse resource(String resourceLocation) {
        return resource(ConversionUtils.conversion(resourceLocation, Resource.class));
    }

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
}
