package com.luckyframework.httpclient.core;

import java.net.URI;
import java.net.URL;
import java.util.Objects;

/**
 * 包含基本功能的Cookie存储器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/10 00:27
 */
public abstract class BaseCookieStore implements CookieStore {

    /**
     * 加载Cookie到请求中
     *
     * @param request 请求实例
     */
    @Override
    public void loadCookie(Request request) {
        URL uri = request.getURL();
        String domain = uri.getHost();
        String path = uri.getPath();

        for (ClientCookie cookie : doLoadCookie()) {
            String _domain = cookie.getDomain();
            String _path = cookie.getPath();
            if (secure(request, cookie) &&
                domainMatch(_domain, domain) &&
                patchMatch(_path, path) &&
                    !cookie.isExpired()) {
                request.addCookie(cookie.getName(), cookie.getValue());
            }
        }
    }

    /**
     * 保存Cookie
     *
     * @param responseMetaData 响应元数据
     */
    @Override
    public void saveCookie(ResponseMetaData responseMetaData) {
        doSaveCookie(
                responseMetaData.getCookies()
                        .stream()
                        .map(h -> new ClientCookie(h, responseMetaData.getRequest()))
                        .toArray(ClientCookie[]::new)
        );
    }


    /**
     * 保存Cookie
     *
     * @param cookies 该次响应返回的所有Cookie信息
     */
    protected abstract void doSaveCookie(ClientCookie[] cookies);

    /**
     * 加载所有保存的Cookie信息
     *
     * @return 所有保存的Cookie信息
     */
    protected abstract ClientCookie[] doLoadCookie();


    /**
     * 域名匹配
     *
     * @param domain    父域名
     * @param subDomain 子域名
     * @return 是否是父域名的字域名
     */
    protected boolean domainMatch(String domain, String subDomain) {
        return compareDomain(domain, subDomain) || isSubdomain(subDomain, domain);
    }

    /**
     * 路径匹配
     *
     * @param path    父路径
     * @param subPath 子路径
     * @return 是否是父路径的子路径
     */
    protected boolean patchMatch(String path, String subPath) {
        path = path.startsWith("/") ? path : "/" + path;
        subPath = subPath.startsWith("/") ? subPath : "/" + path;
        return subPath.startsWith(path);
    }

    /**
     * 判断两个域名是不是一个域名
     *
     * @param domain1 域名1
     * @param domain2 域名2
     * @return 是否是同一个域名
     */
    protected boolean compareDomain(String domain1, String domain2) {
        domain1 = domain1.startsWith(".") ? domain1 : "." + domain1;
        domain2 = domain2.startsWith(".") ? domain2 : "." + domain2;
        return Objects.equals(domain1, domain2);
    }

    /**
     * 域名匹配
     *
     * @param subdomain 子域名
     * @param domain    父域名
     * @return 是否是父域名的字域名
     */
    protected boolean isSubdomain(String subdomain, String domain) {
        domain = domain.startsWith(".") ? domain : "." + domain;
        return subdomain.endsWith(domain);
    }

    protected boolean secure(Request request, ClientCookie cookie) {
       return !cookie.isSecure() || (cookie.isSecure() && request.isHttps());
    }
}
