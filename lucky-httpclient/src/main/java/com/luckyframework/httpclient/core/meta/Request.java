package com.luckyframework.httpclient.core.meta;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.proxy.ProxyInfo;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请求接口
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/28 8:21 下午
 */
public interface Request extends RequestParameter, HttpHeaderManager {

    /**
     * 默认的连接超时时间
     */
    Integer DEF_CONNECTION_TIME_OUT = 60 * 1000;
    /**
     * 默认的读超时时间
     */
    Integer DEF_READ_TIME_OUT = 20 * 1000;
    /**
     * 默认的写超时时间
     */
    Integer DEF_WRITER_TIME_OUT = 20 * 1000;

    /**
     * 请求克隆
     * @return 克隆后的请求实例
     */
    Request copy();

    /**
     * 目标资源的完整URL地址（http://user:password@host:port/path?query#ref）
     */
    String getUrl();

    /**
     * UserInfo部分
     */
    String getUserInfo();

    /**
     * 设置UserInfo部分
     * @param userInfo userInfo
     * @return this
     */
    Request setUserInfo(String userInfo);

    /**
     * Ref部分
     */
    String getRef();

    /**
     * 设置Ref部分
     * @param ref ref
     * @return this
     */
    Request setRef(String ref);

    /**
     * 请求方式(GET、POST、DELETE、PUT...)
     */
    RequestMethod getRequestMethod();

    /**
     * 设置请求方法
     *
     * @param requestMethod 请求方法枚举实例
     * @return this
     */
    Request setRequestMethod(RequestMethod requestMethod);

    /**
     * 请求头管理器
     */
    HttpHeaderManager getHeaderManager();

    /**
     * 请求参数
     */
    RequestParameter getRequestParameter();

    /**
     * 获取连接超时时间
     *
     * @return 连接超时时间 (单位：ms；默认值: 6000ms)
     */
    Integer getConnectTimeout();

    /**
     * 设置连接超时时间(单位：ms；默认值: 2000ms)
     *
     * @param connectionTime 连接超时时间 ,单位：ms
     */
    Request setConnectTimeout(Integer connectionTime);

    /**
     * 获取数据读取的超时时间
     *
     * @return 数据读取的超时时间 (单位：ms；默认值: 2000ms)
     */
    Integer getReadTimeout();

    /**
     * 设置数据读取的超时时间(单位：ms；默认值: 2000ms)
     *
     * @param readTimeout 响应数据的超时时间,单位：ms
     */
    Request setReadTimeout(Integer readTimeout);

    /**
     * 获取数据写入的超时时间
     *
     * @return 据写入的超时时间 (单位：ms；默认值: 2000ms)
     */
    Integer getWriterTimeout();

    /**
     * 设置数据写入的超时时间(单位：ms；默认值: 2000ms)
     *
     * @param writerTimeout 响应数据的超时时间,单位：ms
     */
    Request setWriterTimeout(Integer writerTimeout);

    /**
     * 获取域名校验器
     *
     * @return 域名校验器
     */
    HostnameVerifier getHostnameVerifier();

    /**
     * 设置域名校验器
     */
    Request setHostnameVerifier(HostnameVerifier hostnameVerifier);

    /**
     * 获取{@link SSLSocketFactory}
     *
     * @return SSLSocketFactory
     */
    SSLSocketFactory getSSLSocketFactory();

    /**
     * 设置{@link SSLSocketFactory}
     *
     * @param sslSocketFactory SSLSocketFactory
     */
    Request setSSLSocketFactory(SSLSocketFactory sslSocketFactory);

    /**
     * 设置代理
     *
     * @param proxyInfo 代理信息
     */
    Request setProxyInfo(ProxyInfo proxyInfo);

    /**
     * 获取代理对象信息
     *
     * @return 代理对象信息
     */
    ProxyInfo getProxyInfo();

    /**
     * 请求转换，将一个请求转化成另一种方式的请求
     * @param method 请求方法
     * @return 转化后的请求实例
     */
    default Request change(RequestMethod method) {
        return copy().setRequestMethod(method);
    }

    /**
     * 目标资源的{@link URL}
     */
    default URL getURL() {
        try {
            return new URL(getUrl());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 目标资源的{@link URI}
     */
    default URI getURI() {
        try {
            URL url = getURL();
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 获取当前URI的协议信息
     * @return 协议信息
     */
    default String getProtocol() {
        try {
            return getURL().getProtocol();
        } catch (Exception e) {
            int i = getUrl().indexOf(":");
            if (i != -1) {
                return getUrl().substring(0, i);
            }
            return null;
        }
    }

    /**
     * 是否是一个HTTPS请求
     *
     * @return 是否是一个HTTPS请求
     */
    default boolean isHttps() {
        return "HTTPS".equalsIgnoreCase(getProtocol());
    }

    default Request addCookie(String name, String value) {
        addHeader(HttpHeaders.REQUEST_COOKIE, name + "=" + value);
        return this;
    }

    default String getCookie(String name) {
        List<Header> cookieList = getCookies();
        for (Header header : cookieList) {
            if (header.containsKey(name)) {
                return header.getInternalValue(name);
            }
        }
        return null;
    }

    default List<Header> getCookies() {
        return getHeader(HttpHeaders.REQUEST_COOKIE);
    }

    default Map<String, Object> getSimpleCookies() {
        List<Header> cookieList = getCookies();
        Map<String, Object> cookieMap = new HashMap<>();
        cookieList.forEach(h -> cookieMap.putAll(h.getNameValuePairMap()));
        return cookieMap;
    }

    default Map<String, Object> getSimpleQueries() {
        Map<String, Object> simpleQueries = new HashMap<>();

        String query = getURL().getQuery();
        if (StringUtils.hasText(query)) {
            String[] nvStrArr = query.split("&", -1);
            for (String nvStr : nvStrArr) {
                int index = nvStr.indexOf("=");
                if (index != -1) {
                    simpleQueries.put(nvStr.substring(0, index), nvStr.substring(index + 1));
                }
            }
        }

        Map<String, List<Object>> queryParameters = getQueryParameters();
        queryParameters.forEach((k, v) -> {
            Object value;
            if (ContainerUtils.isEmptyCollection(v)) {
                value = null;
            } else if (v.size() == 1) {
                value = v.get(0);
            } else {
                value = v;
            }
            simpleQueries.put(k, value);
        });
        return simpleQueries;
    }


    default Request removeCookie(String name) {
        List<Header> cookieList = getHeader(HttpHeaders.REQUEST_COOKIE);
        cookieList.removeIf(header -> header.containsKey(name));
        return this;
    }

    default Request trySetProxyAuthenticator() {
        ProxyInfo proxyInfo = getProxyInfo();
        if (proxyInfo != null) {
            if (proxyInfo.getProxy().type() == Proxy.Type.HTTP) {
                proxyInfo.setHttpAuthenticator(this);
            } else if (proxyInfo.getProxy().type() == Proxy.Type.SOCKS) {
                proxyInfo.setSocksAuthenticator();
            }
        }
        return this;
    }

    default void tryResetAuthenticator() {
        ProxyInfo proxyInfo = getProxyInfo();
        if (proxyInfo != null) {
            proxyInfo.resetAuthenticator();
        }
    }

    //-------------------------------------------------------------------
    //              Static Methods (Builder Method)
    //-------------------------------------------------------------------

    /**
     * 快速构建一个Http请求
     *
     * @param url           请求地址,支持Rest参数占位符
     * @param requestMethod 请求方式[GET、POST、DELETE、PUT...]
     * @param pathParams    Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest builder(String url, RequestMethod requestMethod, Object... pathParams) {
        url = StringUtils.format(url, pathParams);
        return new DefaultRequest(url, requestMethod);
    }

    /**
     * 构建一个Http[GET]请求
     *
     * @param url        请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest get(String url, Object... pathParams) {
        return builder(url, RequestMethod.GET, pathParams);
    }

    /**
     * 构建一个Http[POST]请求
     *
     * @param url        请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest post(String url, Object... pathParams) {
        return builder(url, RequestMethod.POST, pathParams);
    }

    /**
     * 构建一个Http[DELETE]请求
     *
     * @param url        请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest delete(String url, Object... pathParams) {
        return builder(url, RequestMethod.DELETE, pathParams);
    }

    /**
     * 构建一个Http[PUT]请求
     *
     * @param url        请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest put(String url, Object... pathParams) {
        return builder(url, RequestMethod.PUT, pathParams);
    }

    /**
     * 构建一个Http[HEAD]请求
     *
     * @param url        请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest head(String url, Object... pathParams) {
        return builder(url, RequestMethod.HEAD, pathParams);
    }

    /**
     * 构建一个Http[PATCH]请求
     *
     * @param url        请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest patch(String url, Object... pathParams) {
        return builder(url, RequestMethod.PATCH, pathParams);
    }

    /**
     * 构建一个Http[CONNECT]请求
     *
     * @param url        请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest connect(String url, Object... pathParams) {
        return builder(url, RequestMethod.CONNECT, pathParams);
    }

    /**
     * 构建一个Http[OPTIONS]请求
     *
     * @param url        请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest options(String url, Object... pathParams) {
        return builder(url, RequestMethod.OPTIONS, pathParams);
    }

    /**
     * 构建一个Http[TRACE]请求
     *
     * @param url        请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static DefaultRequest trace(String url, Object... pathParams) {
        return builder(url, RequestMethod.TRACE, pathParams);
    }

}
