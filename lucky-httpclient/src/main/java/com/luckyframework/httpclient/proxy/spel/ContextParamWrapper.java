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

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_INSTANCE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_LENGTH;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_TYPE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTEXT_ANNOTATED_ELEMENT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD_CONTEXT;
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
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.SPRING_EL_ENV;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.THIS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.THROWABLE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.VOID_RESPONSE;

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
        extractRootKeyValue(SPRING_EL_ENV, context.getHttpProxyFactory().getExpressionParams());
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

    public ContextParamWrapper extractResponse(Response response) {
        extractRootKeyValue(RESPONSE, response);
        extractRootKeyValue(RESPONSE_STATUS, response.getStatus());
        extractRootKeyValue(CONTENT_LENGTH, response.getContentLength());
        extractRootKeyValue(CONTENT_TYPE, response.getContentType());
        extractRootKeyValue(RESPONSE_BODY, getBodyResult(response));
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

    public ContextParamWrapper setExpression(String expression) {
        paramWrapper.setExpression(expression);
        return this;
    }

    public ContextParamWrapper setReturnType(Class<?> returnType) {
        paramWrapper.setExpectedResultType(returnType);
        return this;
    }

    public static Object getBodyResult(Response response) {
        if (response.isJsonType()) {
            return response.jsonStrToEntity(Object.class);
        }
        if (response.isXmlType()) {
            return response.xmlStrToEntity(Object.class);
        }
        if (response.isJavaType()) {
            return response.javaObject();
        }
        return response.getStringResult();
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
