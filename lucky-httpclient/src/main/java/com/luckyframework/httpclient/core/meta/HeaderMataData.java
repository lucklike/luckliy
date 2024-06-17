package com.luckyframework.httpclient.core.meta;

import com.luckyframework.httpclient.core.util.ResourceNameParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 响应头元数据
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/1 02:46
 */
public class HeaderMataData {

    private final Request request;
    private final int status;
    private final HttpHeaderManager responseHeader;
    private String downloadFilename;

    public HeaderMataData(Request request, int status, HttpHeaderManager responseHeader) {
        this.request = request;
        this.status = status;
        this.responseHeader = responseHeader;
    }

    /**
     * 获取请求实例
     *
     * @return 请求实例
     */
    public Request getRequest() {
        return this.request;
    }

    /**
     * 获取响应的状态码
     *
     * @return 响应的状态码
     */
    public int getStatus() {
        return status;
    }

    /**
     * 获取响应头管理器
     *
     * @return 响应头管理器
     */
    public HttpHeaderManager getHeaderManager() {
        return this.responseHeader;
    }

    /**
     * 当前请求是否成功响应，状态码为2xx和3xx时视为成功
     *
     * @return 当前请求是否成功响应
     */
    public boolean isSuccess() {
        int index = status / 100;
        return index == 2 || index == 3;
    }

    /**
     * 获取请求URL
     *
     * @return 取请求URL
     */
    public String getRequestUrl() {
        return this.request.getUrl();
    }

    /**
     * 文件下载场景时使用，用于获取当前正在下载的文件名称
     *
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
     *
     * @return 响应体长度
     */
    public long getContentLength() {
        Header header = getHeaderManager().getFirstHeader(HttpHeaders.CONTENT_LENGTH);
        if (header == null) {
            return -1;
        }
        Object lengthObj = header.getValue();
        return lengthObj == null ? -1 : Long.parseLong(lengthObj.toString());
    }

    /**
     * 获取响应的Content-Type
     *
     * @return 响应Content-Type
     */
    public ContentType getContentType() {
        return getHeaderManager().getContentType();
    }

    /**
     * 获取Cookie值
     *
     * @param name Cookie名称
     * @return 对应得Cookie值
     */
    public String getCookie(String name) {
        List<Header> cookieList = getCookies();
        for (Header header : cookieList) {
            if (header.containsKey(name)) {
                return header.getInternalValue(name);
            }
        }
        return null;
    }

    /**
     * 获取所有的响应头对象
     *
     * @return 所有的响应头对象
     */
    public List<Header> getCookies() {
        return getHeaderManager().getHeader(HttpHeaders.RESPONSE_COOKIE);
    }

    /**
     * 获取所有Cookie键值对所组成得Map
     *
     * @return 所有Cookie键值对所组成得Map
     */
    public Map<String, Object> getSimpleCookies() {
        List<Header> cookieList = getCookies();
        Map<String, Object> cookieMap = new HashMap<>();
        cookieList.forEach(h -> cookieMap.putAll(h.getNameValuePairMap()));
        return cookieMap;
    }

    /**
     * 获取所有响应头键值对所组成得Map
     *
     * @return 所有响应头键值对所组成得Map
     */
    public Map<String, Object> getSimpleHeaders() {
        return getHeaderManager().getSimpleHeaders();
    }

}
