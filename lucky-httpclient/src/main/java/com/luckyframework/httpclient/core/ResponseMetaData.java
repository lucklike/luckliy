package com.luckyframework.httpclient.core;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.web.ContentTypeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 响应原数据
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/5 13:09
 */
public final class ResponseMetaData {

    private final Request request;
    private final int status;
    private final HttpHeaderManager responseHeader;
    private final InputStreamFactory inputStreamFactory;

    private String downloadFilename;

    public ResponseMetaData(Request request, int status, HttpHeaderManager responseHeader, InputStreamFactory inputStreamFactory) {
        this.request = request;
        this.status = status;
        this.responseHeader = responseHeader;
        this.inputStreamFactory = inputStreamFactory;
    }

    /**
     * 获取请求实例
     * @return 请求实例
     */
    public Request getRequest() {
        return request;
    }

    /**
     * 获取响应的状态码
     * @return 响应的状态码
     */
    public int getStatus() {
        return status;
    }

    /**
     * 获取响应头信息
     * @return 响应头信息
     */
    public HttpHeaderManager getResponseHeader() {
        return responseHeader;
    }

    /**
     * 获取响应体内容对应的InputStream
     * @return 响应体内容对应的InputStream
     * @throws IOException 获取失败时会抛出该异常
     */
    public InputStream getInputStream() throws IOException {
        return inputStreamFactory.getInputStream();
    }

    /**
     * 当前请求是否成功响应，状态为200时表示成功，其他表示失败
     * @return 当前请求是否成功响应
     */
    public boolean isSuccess() {
        return status == 200 || status == 302 || status == 301;
    }

    /**
     * 获取请求URL
     * @return 取请求URL
     */
    public String getRequestUrl() {
        return this.request.getUrl();
    }

    /**
     * 文件下载场景时使用，用于获取当前正在下载的文件名称
     * @return 当前正在下载的文件名称
     */
    public String getDownloadFilename() {
        if (downloadFilename == null) {
            this.downloadFilename = ResourceNameParser.getResourceName(this);
        }
        return downloadFilename;
    }


    /**
     * 获取响应体长度（单位：字节）
     * @return 响应体长度
     */
    public long getContentLength() {
        Header header = getHeaderManager().getFirstHeader(HttpHeaders.CONTENT_LENGTH);
        return header == null ? -1 : ConversionUtils.conversion(header.getValue(), long.class);
    }

    /**
     * 获取响应头管理器
     * @return 响应头管理器
     */
    public HttpHeaderManager getHeaderManager(){
        return this.responseHeader;
    }

    /**
     * 获取响应的Content-Type
     *
     * @return 响应Content-Type
     */
    public ContentType getContentType() {
        ContentType contentType = getHeaderManager().getContentType();
        if (contentType != ContentType.NON) {
            return contentType;
        }
        try {
            String mimeType = ContentTypeUtils.getMimeType(getInputStream());
            return mimeType == null ? ContentType.NON : ContentType.create(mimeType, "");
        } catch (IOException e) {
            return ContentType.NON;
        }
    }

    public String getCookie(String name) {
        List<Header> cookieList = getCookies();
        for (Header header : cookieList) {
            if (header.containsKey(name)) {
                return header.getInternalValue(name);
            }
        }
        return null;
    }

    public List<Header> getCookies() {
        List<Header> header = getHeaderManager().getHeader(HttpHeaders.RESPONSE_COOKIE);
        return header == null ? Collections.emptyList() : header;
    }

    public Map<String, Object> getSimpleCookies() {
        List<Header> cookieList = getCookies();
        Map<String, Object> cookieMap = new HashMap<>(cookieList.size());
        cookieList.forEach(h -> cookieMap.put(h.getName(), h.getValue()));
        return cookieMap;
    }

    public Map<String, Object> getSimpleHeaders() {
        return getHeaderManager().getSimpleHeaders();
    }

}
