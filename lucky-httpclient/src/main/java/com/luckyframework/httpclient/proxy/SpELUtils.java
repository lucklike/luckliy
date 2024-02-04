package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.spel.ParamWrapper;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_INSTANCE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_LENGTH;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_TYPE;
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
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.VOID_RESPONSE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_STATUS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.SPRING_EL_ENV;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.THIS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.THROWABLE;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/21 03:47
 */
public class SpELUtils {


    public static  <T> T parseExpression(ParamWrapper paramWrapper) {
        return HttpClientProxyObjectFactory.getSpELConverter().parseExpression(paramWrapper);
    }

    public static ExtraSpELArgs createSpELArgs() {
        return new ExtraSpELArgs();
    }


    public static ParamWrapper getContextParamWrapper(MethodContext context, ExtraSpELArgs extraArgs) {
        ParamWrapper paramWrapper = getImportCompletedParamWrapper(context)
                                        .setRootObject(context.getCurrentAnnotatedElement(), context.getArguments(), extraArgs.getExtraArgMap());
        if (StringUtils.hasText(extraArgs.getExpression())) {
            paramWrapper.setExpression(extraArgs.getExpression());
        }
        if (extraArgs.getReturnType() != null) {
            paramWrapper.setExpectedResultType(extraArgs.getReturnType());
        }
        return paramWrapper;
    }

    public static ParamWrapper getImportCompletedParamWrapper(Context context) {
        ParamWrapper paramWrapper = new ParamWrapper();
        List<AnnotatedElement> annotatedElements = new LinkedList<>();
        String thisClassPackage = null;
        Context tempContext = context;
        while (tempContext != null) {
            if (tempContext instanceof ClassContext) {
                thisClassPackage = ((ClassContext) tempContext).getCurrentAnnotatedElement().getPackage().getName();
            }
            annotatedElements.add(0, tempContext.getCurrentAnnotatedElement());
            tempContext = tempContext.getParentContext();
        }
        paramWrapper.importPackage(annotatedElements);
        if (thisClassPackage != null) {
            paramWrapper.importPackage(thisClassPackage);
        }
        return paramWrapper;
    }


    public static class ExtraSpELArgs {
        private final Map<String, Object> extraArgMap = new HashMap<>();

        private String expression;

        private Class<?> returnType;

        private ExtraSpELArgs(){}

        public ExtraSpELArgs extractSpELEnv(){
            extraArgMap.put(SPRING_EL_ENV, HttpClientProxyObjectFactory.getExpressionParams());
            return this;
        }

        public ExtraSpELArgs extractMethodContext(MethodContext context) {
            extraArgMap.put(THIS, context.getProxyObject());
            extraArgMap.put(METHOD_CONTEXT, context);
            extraArgMap.put(CLASS_CONTEXT, context.getClassContext());
            extraArgMap.put(METHOD, context.getCurrentAnnotatedElement());
            extraArgMap.put(CLASS, context.getClassContext().getCurrentAnnotatedElement());
            return this;
        }

        public ExtraSpELArgs extractAnnotationContext(AnnotationContext context) {
            extraArgMap.put(ANNOTATION_CONTEXT, context);
            extraArgMap.put(ANNOTATION_INSTANCE, context.getAnnotation());
            return this;
        }

        public ExtraSpELArgs extractRequest(Request request) {
            extraArgMap.put(REQUEST, request);
            extraArgMap.put(REQUEST_URL, request.getUrl());
            extraArgMap.put(REQUEST_METHOD, request.getRequestMethod());
            extraArgMap.put(REQUEST_QUERY, request.getSimpleQueries());
            extraArgMap.put(REQUEST_PATH, request.getPathParameters());
            extraArgMap.put(REQUEST_FORM, request.getRequestParameters());
            extraArgMap.put(REQUEST_HEADER, request.getSimpleHeaders());
            extraArgMap.put(REQUEST_COOKIE, request.getSimpleCookies());
            return this;
        }

        public ExtraSpELArgs extractResponse(Response response) {
            extraArgMap.put(RESPONSE, response);
            extraArgMap.put(RESPONSE_STATUS, response.getStatus());
            extraArgMap.put(CONTENT_LENGTH, response.getContentLength());
            extraArgMap.put(CONTENT_TYPE, response.getContentType());
            extraArgMap.put(RESPONSE_BODY, getBodyResult(response));
            extraArgMap.put(RESPONSE_HEADER, response.getSimpleHeaders());
            extraArgMap.put(RESPONSE_COOKIE, response.getSimpleCookies());
            return this;
        }

        public ExtraSpELArgs extractVoidResponse(VoidResponse voidResponse) {
            extraArgMap.put(VOID_RESPONSE, voidResponse);
            extraArgMap.put(RESPONSE_STATUS, voidResponse.getStatus());
            extraArgMap.put(CONTENT_LENGTH, voidResponse.getContentLength());
            extraArgMap.put(CONTENT_TYPE, voidResponse.getContentType());
            extraArgMap.put(RESPONSE_HEADER, voidResponse.getSimpleHeaders());
            extraArgMap.put(RESPONSE_COOKIE, voidResponse.getSimpleCookies());
            return this;
        }

        public ExtraSpELArgs extractKeyValue(String key, Object value) {
            extraArgMap.put(key, value);
            return this;
        }

        public ExtraSpELArgs setExpression(String expression) {
            this.expression = expression;
            return this;
        }

        public ExtraSpELArgs setReturnType(Class<?> returnType) {
            this.returnType = returnType;
            return this;
        }

        protected Object getBodyResult(Response response) {
            if (response.isJsonType()) {
                return response.jsonStrToEntity(Object.class);
            }
            if (response.isXmlType()) {
                return response.xmlStrToEntity(Object.class);
            }
            return null;
        }

        public ExtraSpELArgs extractException(Throwable throwable) {
            extraArgMap.put(THROWABLE, throwable);
            return this;
        }

        public Map<String, Object> getExtraArgMap() {
            return extraArgMap;
        }

        public String getExpression() {
            return expression;
        }

        public Class<?> getReturnType() {
            return returnType;
        }
    }
}
