package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.spel.ParamWrapper;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.*;

/**
 * 上下文参数包装器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/19 21:51
 */
public class ContextParamWrapper {

    private final ParamWrapper paramWrapper;
    private final Map<String, Object> rootArgMap;

    public ContextParamWrapper() {
        this.paramWrapper = new ParamWrapper();
        rootArgMap = new ConcurrentHashMap<>(16);
    }

    private Map<String, Object> getRootMap() {
        return rootArgMap;
    }

    public ContextParamWrapper extractRootKeyValue(String key, Object value) {
        getRootMap().put(key, value);
        return this;
    }

    public ContextParamWrapper extractVariableKeyValue(String key, Object value) {
        paramWrapper.addVariable(key, value);
        return this;
    }

    public ContextParamWrapper extractRootMap(Map<String, Object> map) {
        getRootMap().putAll(map);
        return this;
    }

    public ContextParamWrapper extractVariableMap(Map<String, Object> map) {
        paramWrapper.addVariables(map);
        return this;
    }


    public ContextParamWrapper extractContext(Context context) {
//        extractRootMap(context.getHttpProxyFactory().getSpringElRootVariables());
//        extractVariableMap(context.getHttpProxyFactory().getSpringElVariables());
        extractRootKeyValue(THIS, context.getProxyObject());
        extractRootKeyValue(CONTEXT, context);
        extractRootKeyValue(CONTEXT_ANNOTATED_ELEMENT, context.getCurrentAnnotatedElement());
        return this;
    }

    public ContextParamWrapper extractMethodContext(MethodContext context) {
        extractContext(context)
                .extractRootKeyValue(METHOD_CONTEXT, context)
                .extractRootKeyValue(CLASS_CONTEXT, context.getClassContext())
                .extractRootKeyValue(METHOD, context.getCurrentAnnotatedElement())
                .extractRootKeyValue(CLASS, context.getClassContext().getCurrentAnnotatedElement());
        return this;
    }

    public ContextParamWrapper extractAnnotationContext(AnnotationContext context) {
        extractRootKeyValue(ANNOTATION_CONTEXT, context);
        extractRootKeyValue(ANNOTATION_INSTANCE, context.getAnnotation());
        return this;
    }

    public ContextParamWrapper extractRequest(Request request) {
        extractRootKeyValue(REQUEST, request);
        extractRootKeyValue(REQUEST_URL, request.getUrl());
        extractRootKeyValue(REQUEST_METHOD, request.getRequestMethod());
        extractRootKeyValue(REQUEST_QUERY, request.getSimpleQueries());
        extractRootKeyValue(REQUEST_PATH, request.getPathParameters());
        extractRootKeyValue(REQUEST_FORM, request.getFormParameters());
        extractRootKeyValue(REQUEST_HEADER, request.getSimpleHeaders());
        extractRootKeyValue(REQUEST_COOKIE, request.getSimpleCookies());
        return this;
    }

    public ContextParamWrapper extractResponse(Response response, Class<?> metaType) {
        extractRootKeyValue(RESPONSE, response);
        extractRootKeyValue(RESPONSE_STATUS, response.getStatus());
        extractRootKeyValue(CONTENT_LENGTH, response.getContentLength());
        extractRootKeyValue(CONTENT_TYPE, response.getContentType());
        extractRootKeyValue(RESPONSE_BODY, response.getEntity(metaType));
        extractRootKeyValue(RESPONSE_HEADER, response.getSimpleHeaders());
        extractRootKeyValue(RESPONSE_COOKIE, response.getSimpleCookies());
        return this;
    }

    public ContextParamWrapper extractVoidResponse(VoidResponse voidResponse) {
        extractRootKeyValue(VOID_RESPONSE, voidResponse);
        extractRootKeyValue(RESPONSE_STATUS, voidResponse.getStatus());
        extractRootKeyValue(CONTENT_LENGTH, voidResponse.getContentLength());
        extractRootKeyValue(CONTENT_TYPE, voidResponse.getContentType());
        extractRootKeyValue(RESPONSE_HEADER, voidResponse.getSimpleHeaders());
        extractRootKeyValue(RESPONSE_COOKIE, voidResponse.getSimpleCookies());
        return this;
    }

    public ContextParamWrapper extractPackages(Collection<String> packages) {
        paramWrapper.importPackage(packages.toArray(new String[0]));
        return this;
    }

    public ContextParamWrapper extractPackages(String... packages) {
        paramWrapper.importPackage(packages);
        return this;
    }

    public ContextParamWrapper setExpression(String expression) {
        paramWrapper.setExpression(expression);
        return this;
    }

    public ContextParamWrapper setReturnType(Class<?> returnType) {
        paramWrapper.setExpectedResultType(returnType);
        return this;
    }

    public ContextParamWrapper extractException(Throwable throwable) {
        extractRootKeyValue(THROWABLE, throwable);
        return this;
    }


    public ParamWrapper getParamWrapper() {
        return this.paramWrapper.setRootObject(getRootMap());
    }

    public ParamWrapper getParamWrapper(Method method, Object[] args) {
        return this.paramWrapper.setRootObject(method, args, getRootMap());
    }
}
