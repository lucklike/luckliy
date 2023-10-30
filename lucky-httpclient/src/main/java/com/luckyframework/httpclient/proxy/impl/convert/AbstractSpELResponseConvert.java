package com.luckyframework.httpclient.proxy.impl.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.exception.ResponseProcessException;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.ResponseConvert;
import com.luckyframework.httpclient.proxy.SpELConvert;
import com.luckyframework.spel.ParamWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_INSTANCE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_LENGTH;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_TYPE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_HEADERS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_STATUS;

/**
 * 通用的基于SpEL表达式的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 01:55
 */
public abstract class AbstractSpELResponseConvert implements ResponseConvert {

    private static final Logger log = LoggerFactory.getLogger(AbstractSpELResponseConvert.class);

    protected Object getResponseResult(Response response) {
        if (response.isJsonType()) {
            return response.jsonStrToEntity(Object.class);
        }
        if (response.isXmlType()) {
            return response.xmlStrToEntity(Object.class);
        }
        return null;
    }

    protected <T> T getMethodResult(Response response, MethodContext methodContext) {
        return response.getEntity(methodContext.getRealMethodReturnType());
    }

    protected <T> T getDefaultValue(Response response, MethodContext methodContext, Annotation annotation) {
        String defaultValueSpEL = methodContext.getAnnotationAttribute(annotation, "defaultValue", String.class);
        String exMsg = methodContext.getAnnotationAttribute(annotation, "exMsg", String.class);
        if (StringUtils.hasText(defaultValueSpEL)) {
            if (log.isDebugEnabled()) {
                log.debug("The current request returns the default value :{}", defaultValueSpEL);
            }
            return getSpELConverter().parseExpression(
                    getResponseSpElParamWrapper(response, methodContext)
                            .setExpression(defaultValueSpEL)
                            .addVariable(ANNOTATION_INSTANCE, annotation)
                            .setExpectedResultType(methodContext.getRealMethodReturnType()));
        }
        if (StringUtils.hasText(exMsg)) {
            throw new ResponseProcessException(
                    String.valueOf((Object) getSpELConverter()
                            .parseExpression(getResponseSpElParamWrapper(response, methodContext)
                                    .addVariable(ANNOTATION_INSTANCE, annotation)
                                    .setExpression(exMsg))));
        }
        return null;
    }

    protected <T> T parserSpELExpression(String expression, Response response, MethodContext methodContext) {
        return getSpELConverter()
                .parseExpression(getResponseSpElParamWrapper(response, methodContext)
                        .setExpression(expression)
                        .setExpectedResultType(methodContext.getRealMethodReturnType()));
    }

    protected SpELConvert getSpELConverter() {
        return HttpClientProxyObjectFactory.getSpELConverter();
    }

    protected ParamWrapper getResponseSpElParamWrapper(Response response, MethodContext methodContext) {
        return getImportCompletedParamWrapper(methodContext)
                .setRootObject(getResponseResult(response))
                .addVariable(REQUEST, response.getRequest())
                .addVariable(RESPONSE, response)
                .addVariable(RESPONSE_STATUS, response.getState())
                .addVariable(CONTENT_TYPE, response.getContentType())
                .addVariable(CONTENT_LENGTH, response.getContentLength())
                .addVariable(RESPONSE_HEADERS, response.getHeaderManager().getHeaderMap())
                .addVariable(CLASS_CONTEXT, methodContext.getClassContext())
                .addVariable(METHOD_CONTEXT, methodContext)
                .addVariable(METHOD, methodContext.getCurrentAnnotatedElement())
                .addVariable(CLASS, methodContext.getClassContext().getCurrentAnnotatedElement())
                .addVariables(methodContext.getCurrentAnnotatedElement(), methodContext.getArguments());
    }

//    class
}
