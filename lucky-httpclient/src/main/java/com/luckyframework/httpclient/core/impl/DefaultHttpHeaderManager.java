package com.luckyframework.httpclient.core.impl;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.Header;
import com.luckyframework.httpclient.core.HttpHeaderManager;

import java.util.*;

/**
 * 请求头实现
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/6/10 下午10:13
 */
public class DefaultHttpHeaderManager implements HttpHeaderManager {

    private final Map<String, List<Header>> headers = new LinkedHashMap<>();

    /**
     * 添加一个请求头
     *
     * @param name   名称
     * @param header 头信息
     */
    public DefaultHttpHeaderManager addHeader(String name, Object header) {
        check(name, header);
        name = name.toLowerCase();
        if (headers.containsKey(name)) {
            headers.get(name).add(Header.builderAdd(name, header));
        } else {
            List<Header> list = new ArrayList<>();
            list.add(Header.builderAdd(name, header));
            headers.put(name, list);
        }
        return this;
    }

    /**
     * 设置一个请求头
     *
     * @param name   名称
     * @param header 头信息
     */
    public DefaultHttpHeaderManager setHeader(String name, Object header) {
        check(name, header);
        name = name.toLowerCase();
        if (headers.containsKey(name)) {
            headers.get(name).add(Header.builderSet(name, header));
        } else {
            List<Header> list = new ArrayList<>();
            list.add(Header.builderSet(name, header));
            headers.put(name, list);
        }
        return this;
    }

    @Override
    public DefaultHttpHeaderManager putHeader(String name, Object header) {
        check(name, header);
        name = name.toLowerCase();
        if (headers.containsKey(name)) {
            headers.get(name).add(Header.builderShow(name, header));
        } else {
            List<Header> list = new ArrayList<>();
            list.add(Header.builderShow(name, header));
            headers.put(name, list);
        }
        return this;
    }

    @Override
    public DefaultHttpHeaderManager setHeaders(Map<String, List<Header>> headers) {
        headers.forEach((k, v) -> {
            headers.put(k, new LinkedList<>(v));
        });
        return this;
    }

    @Override
    public DefaultHttpHeaderManager removerHeader(String name) {
        checkHeaderName(name);
        name = name.toLowerCase();
        headers.remove(name);
        return this;
    }


    @Override
    public List<Header> getHeader(String name) {
        checkHeaderName(name);
        name = name.toLowerCase();
        return this.headers.get(name);
    }

    @Override
    public DefaultHttpHeaderManager removerFirstHeader(String name) {
        checkHeaderName(name);
        name = name.toLowerCase();
        List<Header> headerList = this.headers.get(name);
        if (!ContainerUtils.isEmptyCollection(headerList)) {
            headerList.remove(0);
        }
        return this;
    }

    @Override
    public DefaultHttpHeaderManager removerLastHeader(String name) {
       checkHeaderName(name);
        name = name.toLowerCase();
        List<Header> headerList = this.headers.get(name);
        if (!ContainerUtils.isEmptyCollection(headerList)) {
            headerList.remove(headerList.size() - 1);
        }
        return this;
    }

    @Override
    public DefaultHttpHeaderManager removerHeader(String name, int index) {
       checkHeaderName(name);
        name = name.toLowerCase();
        List<Header> headerList = this.headers.get(name);
        if (!ContainerUtils.isEmptyCollection(headerList)) {
            headerList.remove(index);
        }
        return this;
    }

    @Override
    public Map<String, List<Header>> getHeaderMap() {
        return this.headers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("HEADERS: {");
        for (Map.Entry<String, List<Header>> entry : headers.entrySet()) {
            String name = entry.getKey();
            List<Header> valueList = entry.getValue();
            if (ContainerUtils.isEmptyCollection(valueList)) {
                sb.append(name).append("=,");
            } else {
                StringBuilder lb = new StringBuilder(name).append("=[");
                for (Header header : valueList) {
                    lb.append(header).append(", ");
                }
                String ls = lb.toString();
                ls = ls.endsWith(", ") ? ls.substring(0, ls.length() - 2) : ls;
                ls = ls + "]";
                sb.append(ls).append(",");
            }
        }
        String ss = sb.toString();
        ss = ss.endsWith(",") ? ss.substring(0, ss.length() - 1) : ss;
        ss = ss + "}";
        return ss;
    }
}
