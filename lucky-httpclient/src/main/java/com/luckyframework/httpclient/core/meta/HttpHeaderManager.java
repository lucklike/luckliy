package com.luckyframework.httpclient.core.meta;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.KeyCaseSensitivityMap;
import com.luckyframework.common.TempPair;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 头信息管理器
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/30 10:22 上午
 */
public interface HttpHeaderManager {

    /**
     * 添加一个头信息
     *
     * @param name   名称
     * @param header 值
     */
    HttpHeaderManager addHeader(String name, Object header);

    /**
     * 设置一个头信息
     *
     * @param name   名称
     * @param header 值
     */
    HttpHeaderManager setHeader(String name, Object header);

    HttpHeaderManager putHeader(String name, Object header);

    /**
     * 移除一个头信息
     *
     * @param name 名称
     */
    HttpHeaderManager removerHeader(String name);

    HttpHeaderManager setHeaders(Map<String, List<Header>> headers);

    /**
     * 获取第一个匹配的头信息
     *
     * @param name 名称
     * @return 第一个匹配的头信息
     */
    default Header getFirstHeader(String name) {
        List<Header> headerList = getHeader(name);
        if (ContainerUtils.isEmptyCollection(headerList)) {
            return null;
        }
        return headerList.get(0);
    }

    /**
     * 获取最后一个匹配的头信息
     *
     * @param name 名称
     * @return 最后一个匹配的头信息
     */
    default Header getLastHeader(String name) {
        List<Header> headerList = getHeader(name);
        if (ContainerUtils.isEmptyCollection(headerList)) {
            return null;
        }
        return headerList.get(headerList.size() - 1);
    }

    @SuppressWarnings("unchecked")
    default ContentType getContentType() {
        Header header = getFirstHeader(HttpHeaders.CONTENT_TYPE);
        if (header == null || header.getValue() == null) {
            return ContentType.NON;
        }
        String[] contents = header.getValue().toString().split(";");
        String mimeType = null;
        List<TempPair<String, String>> paramsList = new ArrayList<>();
        for (String content : contents) {
            content = content.trim();
            if (!content.contains("=") && mimeType == null) {
                mimeType = content;
            } else if (content.contains("=")) {
                String[] nameValue = content.split("=");
                String name = nameValue[0].trim().toLowerCase();
                String value = nameValue.length == 1 ? "" : nameValue[1].trim().toLowerCase();
                paramsList.add(TempPair.of(name, value));
            }
        }
        mimeType = mimeType == null ? ContentType.DEFAULT_TEXT.getMimeType() : mimeType;
        return ContentType.create(mimeType, paramsList.toArray(new TempPair[0]));
    }

    default HttpHeaderManager setContentType(String contentType) {
        return setHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }

    default HttpHeaderManager setContentType(ContentType contentType) {
        return setHeader(HttpHeaders.CONTENT_TYPE, contentType.toString());
    }

    /**
     * 获取匹配的头信息
     *
     * @param name 名称
     * @return 头信息
     */
    @NonNull
    List<Header> getHeader(String name);

    /**
     * 移除第一个匹配的头信息
     *
     * @param name 名称
     */
    HttpHeaderManager removerFirstHeader(String name);

    /**
     * 移除最后一个匹配的头信息
     *
     * @param name 名称
     */
    HttpHeaderManager removerLastHeader(String name);

    /**
     * 移除指定索引处的头信息
     *
     * @param name 名称
     */
    HttpHeaderManager removerHeader(String name, int index);


    /**
     * 获取所有头名称和头信息信息的Map
     *
     * @return 所有头信息信息组成的Map
     */
    Map<String, List<Header>> getHeaderMap();

    /**
     * 以简单形式获取所有头信息
     *
     * @return Key不区分大小写的Map
     */
    default Map<String, Object> getSimpleHeaders() {
        Map<String, Object> headerMap = new KeyCaseSensitivityMap<>(new HashMap<>());
        getHeaderMap().forEach((name, headers) -> {
            headerMap.put(name, getCurrentHeader(headers));
        });
        return headerMap;
    }

    /**
     * 获取所有的头信息信息
     *
     * @return 获取所有的头信息信息
     */
    default List<Header> getHeaders() {
        List<Header> headers = new LinkedList<>();
        for (List<Header> headerList : getHeaderMap().values()) {
            headers.addAll(headerList);
        }
        return headers;
    }

    /**
     * 添加基于Basic Auth方式的权限认证
     *
     * @param username 用户名
     * @param password 密码
     */
    default HttpHeaderManager setAuthorization(String username, String password, Charset charset) {
        String auth = username + ":" + password;
        byte[] encodeAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodeAuth, charset);
        addHeader(HttpHeaders.AUTHORIZATION, authHeader);
        return this;
    }

    default HttpHeaderManager setAuthorization(String username, String password) {
        return setAuthorization(username, password, StandardCharsets.ISO_8859_1);
    }

    default HttpHeaderManager setProxyAuthorization(String username, String password, Charset charset) {
        String auth = username + ":" + password;
        byte[] encodeAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodeAuth, charset);
        addHeader(HttpHeaders.PROXY_AUTHORIZATION, authHeader);
        return this;
    }

    default HttpHeaderManager setProxyAuthorization(String username, String password) {
        return setProxyAuthorization(username, password, StandardCharsets.ISO_8859_1);
    }

    default void check(String name, Object header) {
        checkHeaderName(name);
        checkHeaderValue(header);
    }

    default void checkHeaderName(String name) {
        Assert.notNull(name, "Header name is null");
    }

    default void checkHeaderValue(Object value) {
        Assert.notNull(value, "Header value is null");
    }

    default Object getCurrentHeader(List<Header> headers) {
        if (ContainerUtils.isEmptyCollection(headers)) {
            throw new IllegalArgumentException("Header list is empty.");
        }
        Header header;
        Header.HeaderType headerType;
        for (int i = headers.size() - 1; i >= 0; i--) {
            header = headers.get(i);
            headerType = header.getHeaderType();
            if (i == 0 || headerType == Header.HeaderType.SET) {
                return header.getValue();
            }
        }
        return null;
    }
}
