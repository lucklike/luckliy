package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.CookieStore;
import com.luckyframework.httpclient.core.MemoryCookieStore;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;

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
    public VoidResponse doAfterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        cookieStore.saveCookie(voidResponse.getResponseMetaData());
        return voidResponse;
    }

    @Override
    public Response doAfterExecute(Response response, InterceptorContext context) {
        cookieStore.saveCookie(response.getResponseMetaData());
        return response;
    }
}
