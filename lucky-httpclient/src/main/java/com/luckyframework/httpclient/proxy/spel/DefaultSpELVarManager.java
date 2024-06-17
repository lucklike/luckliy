package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.RespImportIntoSpEL;
import com.luckyframework.httpclient.proxy.context.Context;
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
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_BODY;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_COOKIE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_HEADER;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_STATUS;

/**
 * SpEl变量管理器的默认实现
 */
public abstract class DefaultSpELVarManager implements SpELVarManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultSpELVarManager.class);

    private final MapRootParamWrapper contextVar = new MapRootParamWrapper();
    private final MapRootParamWrapper requestVar = new MapRootParamWrapper();
    private final MapRootParamWrapper voidResponseVar = new MapRootParamWrapper();
    private final MapRootParamWrapper responseVar = new MapRootParamWrapper();


    @NonNull
    @Override
    public MapRootParamWrapper getContextVar() {
        return this.contextVar;
    }

    @Override
    public void setRequestVar(Request request) {
        requestVar.addRootVariable(REQUEST, request);
        requestVar.addRootVariable(REQUEST_URL, request.getUrl());
        requestVar.addRootVariable(REQUEST_METHOD, request.getRequestMethod());
        requestVar.addRootVariable(REQUEST_QUERY, request.getSimpleQueries());
        requestVar.addRootVariable(REQUEST_PATH, request.getPathParameters());
        requestVar.addRootVariable(REQUEST_FORM, request.getFormParameters());
        requestVar.addRootVariable(REQUEST_HEADER, request.getSimpleHeaders());
        requestVar.addRootVariable(REQUEST_COOKIE, request.getSimpleCookies());
    }

    @NonNull
    @Override
    public MapRootParamWrapper getRequestVar() {
        return this.requestVar;
    }

    @NonNull
    @Override
    public MapRootParamWrapper getVoidResponseVar() {
        return this.voidResponseVar;
    }

    @Override
    public void setResponseVar(Response response, Context context) {
        RespImportIntoSpEL importAnn = context.getSameAnnotationCombined(RespImportIntoSpEL.class);
        if(importAnn == null) return;

        if (importAnn.importRespInstance()) {
            responseVar.addRootVariable(RESPONSE, response);
        }
        if (importAnn.importHeader()) {
            responseVar.addRootVariable(RESPONSE_STATUS, response.getStatus());
            responseVar.addRootVariable(CONTENT_LENGTH, response.getContentLength());
            responseVar.addRootVariable(CONTENT_TYPE, response.getContentType());
            responseVar.addRootVariable(RESPONSE_HEADER, response.getSimpleHeaders());
            responseVar.addRootVariable(RESPONSE_COOKIE, response.getSimpleCookies());
        }
        if (importAnn.importBody()) {
            responseVar.addRootVariable(RESPONSE_BODY, getResponseBody(response, context.getConvertMetaType()));
        }
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
