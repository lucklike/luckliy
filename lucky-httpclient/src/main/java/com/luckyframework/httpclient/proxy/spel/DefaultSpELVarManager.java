package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.spel.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CONTENT_LENGTH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CONTENT_TYPE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_COOKIE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_FORM_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_HEADER_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_METHOD_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_PATH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_QUERY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_URL_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_URL_PATH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_BYTE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_COOKIE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_HEADER_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_STATUS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_STREAM_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_STRING_BODY_$;

/**
 * SpEl变量管理器的默认实现
 */
public abstract class DefaultSpELVarManager implements SpELVarManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultSpELVarManager.class);

    private final MapRootParamWrapper contextVar = new MapRootParamWrapper();
    private final MapRootParamWrapper requestVar = new MapRootParamWrapper();
    private final MapRootParamWrapper responseVar = new MapRootParamWrapper();


    @NonNull
    @Override
    public MapRootParamWrapper getContextVar() {
        return this.contextVar;
    }

    @Override
    public void setRequestVar(Request request) {
        requestVar.addRootVariable($_REQUEST_$, LazyValue.of(request));
        requestVar.addRootVariable($_REQUEST_URL_$, LazyValue.rtc(request::getUrl));
        requestVar.addRootVariable($_REQUEST_URL_PATH_$, LazyValue.rtc(() -> request.getURL().getPath()));
        requestVar.addRootVariable($_REQUEST_METHOD_$, LazyValue.rtc(request::getRequestMethod));
        requestVar.addRootVariable($_REQUEST_QUERY_$, LazyValue.rtc(request::getSimpleQueries));
        requestVar.addRootVariable($_REQUEST_PATH_$, LazyValue.rtc(request::getPathParameters));
        requestVar.addRootVariable($_REQUEST_FORM_$, LazyValue.rtc(request::getFormParameters));
        requestVar.addRootVariable($_REQUEST_HEADER_$, LazyValue.rtc(request::getSimpleHeaders));
        requestVar.addRootVariable($_REQUEST_COOKIE_$, LazyValue.rtc(request::getSimpleCookies));
    }

    @NonNull
    @Override
    public MapRootParamWrapper getRequestVar() {
        return this.requestVar;
    }

    @Override
    public void setResponseVar(Response response, Context context) {
        responseVar.addRootVariable($_RESPONSE_$, LazyValue.of(response));
        responseVar.addRootVariable($_RESPONSE_STATUS_$, LazyValue.of(response::getStatus));
        responseVar.addRootVariable($_CONTENT_LENGTH_$, LazyValue.of(response::getContentLength));
        responseVar.addRootVariable($_CONTENT_TYPE_$, LazyValue.of(response::getContentType));
        responseVar.addRootVariable($_RESPONSE_HEADER_$, LazyValue.of(response::getSimpleHeaders));
        responseVar.addRootVariable($_RESPONSE_COOKIE_$, LazyValue.of(response::getSimpleCookies));
        responseVar.addRootVariable($_RESPONSE_STREAM_BODY_$, LazyValue.rtc(response::getInputStream));
        responseVar.addRootVariable($_RESPONSE_STRING_BODY_$, LazyValue.of(response::getStringResult));
        responseVar.addRootVariable($_RESPONSE_BYTE_BODY_$, LazyValue.of(response::getResult));
        responseVar.addRootVariable($_RESPONSE_BODY_$, LazyValue.of(() -> getResponseBody(response, context.getConvertMetaType())));

    }

    @NonNull
    @Override
    public MapRootParamWrapper getResponseVar() {
        return this.responseVar;
    }

    public static Object getResponseBody(Response response, Class<?> metaType) {
        try {
            Object entity = response.getEntity(metaType);
            return entity == null ? response.getStringResult() : entity;
        } catch (Exception e) {
            log.warn("The response body cannot be converted to the specified '{}' type, and the response result will be stored in the SpEL runtime environment as a String", metaType);
            return response.getStringResult();
        }
    }


}
