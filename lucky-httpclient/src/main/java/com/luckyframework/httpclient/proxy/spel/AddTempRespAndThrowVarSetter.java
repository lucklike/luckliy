package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.spel.LazyValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getResponseBody;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CONTENT_LENGTH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CONTENT_TYPE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_BYTE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_COOKIE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_HEADER_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_STATUS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_STREAM_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_STRING_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_THROWABLE_$;

public class AddTempRespAndThrowVarSetter implements ContextSpELExecution.ParamWrapperSetter {

    private final Response response;
    private final Throwable throwable;
    private final Context context;

    public AddTempRespAndThrowVarSetter(@NonNull Response response, @NonNull Context context, @Nullable Throwable throwable) {
        this.response = response;
        this.throwable = throwable;
        this.context = context;
    }

    @Override
    public void setting(MutableMapParamWrapper paramWrapper) {
        Map<String, Object> extendMap = new ConcurrentHashMap<>(11);
        if (throwable != null) {
            extendMap.put($_THROWABLE_$, LazyValue.of(throwable));
        }
        extendMap.put($_RESPONSE_$, LazyValue.of(response));
        extendMap.put($_RESPONSE_STATUS_$, LazyValue.of(response::getStatus));
        extendMap.put($_CONTENT_LENGTH_$, LazyValue.of(response::getContentLength));
        extendMap.put($_CONTENT_TYPE_$, LazyValue.of(response::getContentType));
        extendMap.put($_RESPONSE_HEADER_$, LazyValue.of(response::getSimpleHeaders));
        extendMap.put($_RESPONSE_COOKIE_$, LazyValue.of(response::getSimpleCookies));
        extendMap.put($_RESPONSE_STREAM_BODY_$, LazyValue.rtc(response::getInputStream));
        extendMap.put($_RESPONSE_STRING_BODY_$, LazyValue.of(response::getStringResult));
        extendMap.put($_RESPONSE_BYTE_BODY_$, LazyValue.of(response::getResult));
        extendMap.put($_RESPONSE_BODY_$, LazyValue.of(() -> getResponseBody(response, context.getConvertMetaType())));
        paramWrapper.getRootObject().addFirst(extendMap);
    }

}
