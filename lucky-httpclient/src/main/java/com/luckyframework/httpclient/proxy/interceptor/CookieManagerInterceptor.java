package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.ClientCookie;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cookie管理器拦截器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/4 23:18
 */
public class CookieManagerInterceptor implements Interceptor {

    private final Map<String, List<ClientCookie>> cookieMap = new ConcurrentHashMap<>(16);

}
