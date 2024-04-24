package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.*;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_COOKIE;

/**
 * SpEl变量管理器的默认实现
 */
public abstract class DefaultSpELVarManager implements SpELVarManager {

    private final MapRootParamWrapper globalVar = new MapRootParamWrapper();
    private final MapRootParamWrapper contextVar = new MapRootParamWrapper();
    private final MapRootParamWrapper requestVar = new MapRootParamWrapper();
    private final MapRootParamWrapper voidResponseVar = new MapRootParamWrapper();
    private final MapRootParamWrapper responseVar = new MapRootParamWrapper();


    @Override
    public void setGlobalVar(MapRootParamWrapper globalVar) {
        this.globalVar.mergeVar(globalVar);
    }

    @NonNull
    @Override
    public MapRootParamWrapper getGlobalVar() {
        return this.globalVar;
    }


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

    @Override
    public void setVoidResponseVar(VoidResponse voidResponse) {
        voidResponseVar.addRootVariable(VOID_RESPONSE, voidResponse);
        voidResponseVar.addRootVariable(RESPONSE_STATUS, voidResponse.getStatus());
        voidResponseVar.addRootVariable(CONTENT_LENGTH, voidResponse.getContentLength());
        voidResponseVar.addRootVariable(CONTENT_TYPE, voidResponse.getContentType());
        voidResponseVar.addRootVariable(RESPONSE_HEADER, voidResponse.getSimpleHeaders());
        voidResponseVar.addRootVariable(RESPONSE_COOKIE, voidResponse.getSimpleCookies());
    }

    @NonNull
    @Override
    public MapRootParamWrapper getVoidResponseVar() {
        return this.voidResponseVar;
    }

    @Override
    public void setResponseVar(Response response, Class<?> metaType) {
        responseVar.addRootVariable(RESPONSE, response);
        responseVar.addRootVariable(RESPONSE_STATUS, response.getStatus());
        responseVar.addRootVariable(CONTENT_LENGTH, response.getContentLength());
        responseVar.addRootVariable(CONTENT_TYPE, response.getContentType());
        responseVar.addRootVariable(RESPONSE_BODY, getResponseBody(response, metaType));
        responseVar.addRootVariable(RESPONSE_HEADER, response.getSimpleHeaders());
        responseVar.addRootVariable(RESPONSE_COOKIE, response.getSimpleCookies());
    }

    @NonNull
    @Override
    public MapRootParamWrapper getResponseVar() {
        return this.responseVar;
    }

    public static Object getResponseBody(Response response, Class<?> metaType) {
        try {
            return response.getEntity(metaType);
        } catch (Exception e) {
            return response.getStringResult();
        }
    }
}
