package com.luckyframework.httpclient.core;

import com.luckyframework.common.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 客户端Cookie存储库
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/5 00:49
 */
public class ClientCookieStore {

    private final List<ClientCookie> cookieList = new ArrayList<>(16);


    public void addCookie(ClientCookie cookie) {
        removeCookie(cookie);
        this.cookieList.add(cookie);
    }

    public List<ClientCookie> getCookiesByName(String name) {
        return getEffectiveCookies().stream().filter(c -> Objects.equals(name, c.getName())).collect(Collectors.toList());
    }

    public List<ClientCookie> getCookiesByDomain(String domain, boolean findSubdomain) {
        Stream<ClientCookie> cookieStream = getEffectiveCookies().stream().filter(c -> c.getDomain() != null);
        return findSubdomain
                ? cookieStream.filter(c -> compareDomain(c.getDomain(), domain) || isSubdomain(domain, c.getDomain())).collect(Collectors.toList())
                : cookieStream.filter(c -> compareDomain(c.getDomain(), domain) ).collect(Collectors.toList());
    }

    public List<ClientCookie> getCommonCookies() {
        return getEffectiveCookies().stream().filter(c -> !StringUtils.hasText(c.getDomain()) || ".".equals(c.getDomain())).collect(Collectors.toList());
    }

    public List<ClientCookie> getEffectiveCookies() {
        cookieList.removeIf(ClientCookie::isExpired);
        return cookieList;
    }

    public boolean removeCookie(ClientCookie cookie) {
       return cookieList.removeIf(_cookie -> compareCookies(cookie, _cookie));
    }

    private boolean isSubdomain(String subdomain, String domain) {
        domain = domain.startsWith(".") ? domain : "." + domain;
        return subdomain.endsWith(domain);
    }

    private boolean compareDomain(String domain1, String domain2) {
        domain1 = domain1.startsWith(".") ? domain1 : "." + domain1;
        domain2 = domain2.startsWith(".") ? domain2 : "." + domain2;
        return Objects.equals(domain1, domain2);
    }



    /**
     * <pre>
     * 比较两个Cookie是否相同
     * 1.名称相同
     * 2.域相同
     * </pre>
     *
     * @param c1 cookie1
     * @param c2 cookie2
     * @return 两个Cookie是否相同
     */
    protected boolean compareCookies(ClientCookie c1, ClientCookie c2) {
        return Objects.equals(c1.getName(), c2.getName()) &&
                Objects.equals(c1.getDomain(), c2.getDomain());
    }

}
