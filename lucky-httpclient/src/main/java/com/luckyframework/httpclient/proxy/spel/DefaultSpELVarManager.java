package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.spel.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_LENGTH;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_TYPE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_COOKIE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_FORM;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_HEADER;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_METHOD;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_PATH;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_QUERY;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_URL;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_URL_PATH;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_BODY;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_BYTE_BODY;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_COOKIE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_HEADER;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_STATUS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_STREAM_BODY;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_STRING_BODY;

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
        requestVar.addRootVariable(REQUEST, LazyValue.of(request));
        requestVar.addRootVariable(REQUEST_URL, LazyValue.rtc(request::getUrl));
        requestVar.addRootVariable(REQUEST_URL_PATH, LazyValue.rtc(() -> request.getURL().getPath()));
        requestVar.addRootVariable(REQUEST_METHOD, LazyValue.rtc(request::getRequestMethod));
        requestVar.addRootVariable(REQUEST_QUERY, LazyValue.rtc(request::getSimpleQueries));
        requestVar.addRootVariable(REQUEST_PATH, LazyValue.rtc(request::getPathParameters));
        requestVar.addRootVariable(REQUEST_FORM, LazyValue.rtc(request::getFormParameters));
        requestVar.addRootVariable(REQUEST_HEADER, LazyValue.rtc(request::getSimpleHeaders));
        requestVar.addRootVariable(REQUEST_COOKIE, LazyValue.rtc(request::getSimpleCookies));
    }

    @NonNull
    @Override
    public MapRootParamWrapper getRequestVar() {
        return this.requestVar;
    }

    @Override
    public void setResponseVar(Response response, Context context) {
        responseVar.addRootVariable(RESPONSE, LazyValue.of(response));
        responseVar.addRootVariable(RESPONSE_STATUS, LazyValue.of(response::getStatus));
        responseVar.addRootVariable(CONTENT_LENGTH, LazyValue.of(response::getContentLength));
        responseVar.addRootVariable(CONTENT_TYPE, LazyValue.of(response::getContentType));
        responseVar.addRootVariable(RESPONSE_HEADER, LazyValue.of(response::getSimpleHeaders));
        responseVar.addRootVariable(RESPONSE_COOKIE, LazyValue.of(response::getSimpleCookies));
        responseVar.addRootVariable(RESPONSE_STREAM_BODY, LazyValue.rtc(response::getInputStream));
        responseVar.addRootVariable(RESPONSE_STRING_BODY, LazyValue.of(response::getStringResult));
        responseVar.addRootVariable(RESPONSE_BYTE_BODY, LazyValue.of(response::getResult));
        responseVar.addRootVariable(RESPONSE_BODY, LazyValue.of(() -> getResponseBody(response, context.getConvertMetaType())));

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
