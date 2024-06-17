package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.meta.CookieStore;
import com.luckyframework.httpclient.core.meta.MemoryCookieStore;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;

/**
 * Cookie管理器拦截器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/4 23:18
 */
public class CookieManagerInterceptor implements Interceptor {

    private CookieStore cookieStore;

    public CookieManagerInterceptor() {
        this(new MemoryCookieStore());
    }

    public CookieManagerInterceptor(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public void setCookieStore(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    @Override
    public void doBeforeExecute(Request request, InterceptorContext context) {
        cookieStore.loadCookie(request);
    }

    @Override
    public Response doAfterExecute(Response response, InterceptorContext context) {
        cookieStore.saveCookie(response.getResponseMetaData());
        return response;
    }
}
