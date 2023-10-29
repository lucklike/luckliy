package com.luckyframework.httpclient.proxy.impl.interceptor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.exception.ConditionNotSatisfiedException;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.ResponseInterceptor;
import com.luckyframework.httpclient.proxy.SpELConvert;
import com.luckyframework.spel.ParamWrapper;

import java.lang.annotation.Annotation;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_HEADERS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_STATUS;

/**
 * 响应条件判断拦截器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/12 03:24
 */
public class ResponseConditionInterceptor implements ResponseInterceptor {

    @Override
    public void responseProcess(Response response, MethodContext context, Annotation responseInterceptorHandleAnn) {
        String[] conditions = context.getAnnotationAttribute(responseInterceptorHandleAnn, getResponseConditionFieldName(), String[].class);
        if (!ContainerUtils.isEmptyArray(conditions)) {
            SpELConvert spELConverter = HttpClientProxyObjectFactory.getSpELConverter();
            for (String condition : conditions) {
                boolean isPass = spELConverter.parseExpression(
                        getImportCompletedParamWrapper(context)
                                .setExpression(condition)
                                .setRootObject(response.getEntity(Object.class))
                                .addVariable(REQUEST, response.getRequest())
                                .addVariable(RESPONSE_STATUS, response.getState())
                                .addVariable(RESPONSE_HEADERS, response.getHeaderManager().getHeaderMap())
                                .addVariable(CLASS_CONTEXT, context.getClassContext())
                                .addVariable(METHOD_CONTEXT, context)
                                .addVariable(METHOD, context.getCurrentAnnotatedElement())
                                .addVariable(CLASS, context.getClassContext().getCurrentAnnotatedElement())
                                .addVariables(context.getCurrentAnnotatedElement(), context.getArguments())
                                .setExpectedResultType(boolean.class));
                if (!isPass) {
                    throw new ConditionNotSatisfiedException("The response to the current request does not meet the condition '{}' in the condition comment, the current method is: {}, the current request is: {}, the response is: {}",
                            condition,
                            context.getCurrentAnnotatedElement(),
                            response.getRequest(),
                            StringUtils.format("[{}] {}", response.getState(), response.getStringResult()));
                }
            }
        }
    }

    protected String getResponseConditionFieldName() {
        return "value";
    }


}
