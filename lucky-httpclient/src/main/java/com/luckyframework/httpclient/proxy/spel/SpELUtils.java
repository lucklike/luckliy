package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.annotations.SpELVar;
import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.spel.ParamWrapper;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.*;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/21 03:47
 */
public class SpELUtils {


    public static <T> T parseExpression(Context context, ParamWrapper paramWrapper) {
        SpELConvert spELConverter = context.getHttpProxyFactory().getSpELConverter();
        extractSpELVal(context, spELConverter, paramWrapper);
        return spELConverter.parseExpression(paramWrapper);
    }

    public static ExtraSpELArgs createSpELArgs() {
        return new ExtraSpELArgs();
    }


    public static ParamWrapper getContextParamWrapper(MethodContext context, ExtraSpELArgs extraArgs) {
        ParamWrapper paramWrapper = getImportCompletedParamWrapper(context)
                .setRootObject(context.getCurrentAnnotatedElement(), context.getAfterProcessArguments(), extraArgs.getRootArgMap());
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

    private static void extractSpELVal(Context context, SpELConvert spELConverter, ParamWrapper paramWrapper) {
        List<Context> contextList = new ArrayList<>();
        Context tempContext = context;
        while (tempContext != null) {
            contextList.add(tempContext);
            tempContext = tempContext.getParentContext();
        }

        for (int i = contextList.size() - 1; i >= 0; i--) {
            doExtractSpELVal(contextList.get(i), spELConverter, paramWrapper);
        }
    }

    private static void doExtractSpELVal(Context context, SpELConvert spELConverter, ParamWrapper paramWrapper) {
        if (context == null) {
            return;
        }
        SpELVar spELVarAnn = context.getMergedAnnotation(SpELVar.class);
        if (spELVarAnn == null) {
            return;
        }

        for (String rootExp : spELVarAnn.root()) {
            TempPair<String, Object> pair = analyticExpression(spELConverter, paramWrapper, rootExp);
            ((Map<String, Object>) paramWrapper.getRootObject()).put(pair.getOne(), pair.getTwo());
        }

        for (String valExp : spELVarAnn.var()) {
            TempPair<String, Object> pair = analyticExpression(spELConverter, paramWrapper, valExp);
            paramWrapper.getVariables().put(pair.getOne(), pair.getTwo());
        }
    }

    private static TempPair<String, Object> analyticExpression(SpELConvert spELConverter, ParamWrapper paramWrapper, String expression) {
        int index = expression.indexOf("=");
        if (index == -1) {
            throw new IllegalArgumentException("Wrong @SpELVar expression: '" + expression + "'");
        }
        String nameExpression = expression.substring(0, index).trim();
        String valueExpression = expression.substring(index + 1).trim();

        ParamWrapper namePw = new ParamWrapper(paramWrapper).setExpression(nameExpression).setExpectedResultType(String.class);
        ParamWrapper valuePw = new ParamWrapper(paramWrapper).setExpression(valueExpression).setExpectedResultType(Object.class);

        return TempPair.of(spELConverter.parseExpression(namePw), spELConverter.parseExpression(valuePw));

    }


    public static class ExtraSpELArgs {
        private final Map<String, Object> rootArgMap = new HashMap<>();
        private String expression;

        private Class<?> returnType;

        private ExtraSpELArgs() {
        }

        public ExtraSpELArgs extractContext(Context context) {
            rootArgMap.put(SPRING_EL_ENV, context.getHttpProxyFactory().getExpressionParams());
            rootArgMap.put(THIS, context.getProxyObject());
            rootArgMap.put(CONTEXT, context);
            rootArgMap.put(CONTEXT_ANNOTATED_ELEMENT, context.getCurrentAnnotatedElement());
            return this;
        }

        public ExtraSpELArgs extractMethodContext(MethodContext context) {
            extractContext(context)
                    .extractKeyValue(METHOD_CONTEXT, context)
                    .extractKeyValue(CLASS_CONTEXT, context.getClassContext())
                    .extractKeyValue(METHOD, context.getCurrentAnnotatedElement())
                    .extractKeyValue(CLASS, context.getClassContext().getCurrentAnnotatedElement());
            return this;
        }

        public ExtraSpELArgs extractAnnotationContext(AnnotationContext context) {
            rootArgMap.put(ANNOTATION_CONTEXT, context);
            rootArgMap.put(ANNOTATION_INSTANCE, context.getAnnotation());
            return this;
        }

        public ExtraSpELArgs extractRequest(Request request) {
            rootArgMap.put(REQUEST, request);
            rootArgMap.put(REQUEST_URL, request.getUrl());
            rootArgMap.put(REQUEST_METHOD, request.getRequestMethod());
            rootArgMap.put(REQUEST_QUERY, request.getSimpleQueries());
            rootArgMap.put(REQUEST_PATH, request.getPathParameters());
            rootArgMap.put(REQUEST_FORM, request.getFormParameters());
            rootArgMap.put(REQUEST_HEADER, request.getSimpleHeaders());
            rootArgMap.put(REQUEST_COOKIE, request.getSimpleCookies());
            return this;
        }

        public ExtraSpELArgs extractResponse(Response response) {
            rootArgMap.put(RESPONSE, response);
            rootArgMap.put(RESPONSE_STATUS, response.getStatus());
            rootArgMap.put(CONTENT_LENGTH, response.getContentLength());
            rootArgMap.put(CONTENT_TYPE, response.getContentType());
            rootArgMap.put(RESPONSE_BODY, getBodyResult(response));
            rootArgMap.put(RESPONSE_HEADER, response.getSimpleHeaders());
            rootArgMap.put(RESPONSE_COOKIE, response.getSimpleCookies());
            return this;
        }

        public ExtraSpELArgs extractVoidResponse(VoidResponse voidResponse) {
            rootArgMap.put(VOID_RESPONSE, voidResponse);
            rootArgMap.put(RESPONSE_STATUS, voidResponse.getStatus());
            rootArgMap.put(CONTENT_LENGTH, voidResponse.getContentLength());
            rootArgMap.put(CONTENT_TYPE, voidResponse.getContentType());
            rootArgMap.put(RESPONSE_HEADER, voidResponse.getSimpleHeaders());
            rootArgMap.put(RESPONSE_COOKIE, voidResponse.getSimpleCookies());
            return this;
        }

        public ExtraSpELArgs extractKeyValue(String key, Object value) {
            rootArgMap.put(key, value);
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

        public ExtraSpELArgs extractException(Throwable throwable) {
            rootArgMap.put(THROWABLE, throwable);
            return this;
        }

        public Map<String, Object> getRootArgMap() {
            return rootArgMap;
        }

        public String getExpression() {
            return expression;
        }

        public Class<?> getReturnType() {
            return returnType;
        }
    }
}
