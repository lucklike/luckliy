package com.luckyframework.httpclient.generalapi.describe;


import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.CommonFunctions;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Callback;

public class TraceFunction {

    @Callback(lifecycle = Lifecycle.REQUEST)
    public static void addTraceQueryParam(Request request) {
        request.addQueryParameter("_traceId_", CommonFunctions.nanoid(15));
    }

}
