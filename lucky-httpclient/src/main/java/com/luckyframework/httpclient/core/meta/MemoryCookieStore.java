package com.luckyframework.httpclient.core.meta;

import com.luckyframework.cache.finder.ExpiringMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.exception.CookieException;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 客户端Cookie存储库
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/5 00:49
 */
public class MemoryCookieStore extends BaseCookieStore {

    private final ExpiringMap<String, ClientCookie> cookieMap;

    private final Function<ClientCookie, String> keyCreate;

    public MemoryCookieStore(ExpiringMap<String, ClientCookie> cookieMap, Function<ClientCookie, String> keyCreate) {
        this.cookieMap = cookieMap;
        this.keyCreate = keyCreate;
    }

    public MemoryCookieStore(ExpiringMap<String, ClientCookie> cookieMap) {
        this(cookieMap, new DefaultKeyCreate());
    }

    public MemoryCookieStore(int cleaningIntervalSeconds, int initialDelaySeconds) {
        this(new ExpiringMap<>(cleaningIntervalSeconds, initialDelaySeconds));
    }

    public MemoryCookieStore() {
        this(10, 10);
    }

    public void addCookie(ClientCookie cookie) {
        String signature = getCookieSignature(cookie);
        ClientCookie oldCookie = cookieMap.getNotExpired(signature);
        if (oldCookie != null && oldCookie.isHttpOnly()) {
            throw new CookieException("Cookie with the httpOnly attribute true cannot be modified");
        }
        this.cookieMap.put(signature, cookie, cookie.getExpireTimeLong());
    }

    public void removeCookie(ClientCookie cookie) {
        this.cookieMap.remove(getCookieSignature(cookie));
    }

    @Override
    public void doSaveCookie(ClientCookie[] cookies) {
        Stream.of(cookies).forEach(this::addCookie);
    }

    @Override
    public ClientCookie[] doLoadCookie() {
        return this.cookieMap.notExpiredValues().toArray(new ClientCookie[0]);
    }

    public String getCookieSignature(ClientCookie cookie) {
        return keyCreate.apply(cookie);
    }

    static class DefaultKeyCreate implements Function<ClientCookie, String> {

        @Override
        public String apply(ClientCookie cookie) {
            return StringUtils.format("{}[{}]", cookie.getDomain(), cookie.getName());
        }
    }

}
