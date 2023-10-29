package com.luckyframework.httpclient.proxy.impl.interceptor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.exception.ConditionNotSatisfiedException;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.RequestInterceptor;
import com.luckyframework.httpclient.proxy.SpELConvert;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELImport;

import java.lang.annotation.Annotation;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_INSTANCE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_FORM;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_PATH;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_HEADER;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_QUERY;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_TYPE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST_URL;

/**
 * 请求条件判断
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/12 03:24
 */
public class RequestConditionInterceptor implements RequestInterceptor {


    @Override
    public void requestProcess(Request request, MethodContext context, Annotation reqInterHandleAnn) {
        String[] conditions = context.getAnnotationAttribute(reqInterHandleAnn, getRequestConditionFieldName(), String[].class);
        if (!ContainerUtils.isEmptyArray(conditions)) {
            SpELConvert spELConverter = HttpClientProxyObjectFactory.getSpELConverter();
            for (String condition : conditions) {
                boolean isPass = spELConverter.parseExpression(
                        getImportCompletedParamWrapper(context)
                                .setExpression(condition)
                                .setRootObject(request)
                                .addVariable(REQUEST_URL, request.getUrl())
                                .addVariable(REQUEST_TYPE, request.getRequestMethod())
                                .addVariable(REQUEST_QUERY, request.getQueryParameters())
                                .addVariable(REQUEST_PATH, request.getPathParameters())
                                .addVariable(REQUEST_HEADER, request.getHeaderManager().getHeaderMap())
                                .addVariable(REQUEST_FORM, request.getRequestParameters())
                                .addVariable(CLASS_CONTEXT, context.getClassContext())
                                .addVariable(METHOD_CONTEXT, context)
                                .addVariable(METHOD, context.getCurrentAnnotatedElement())
                                .addVariable(CLASS, context.getClassContext().getCurrentAnnotatedElement())
                                .addVariables(context.getCurrentAnnotatedElement(), context.getArguments())
                                .addVariable(ANNOTATION_INSTANCE, reqInterHandleAnn)
                                .setExpectedResultType(boolean.class));
                if (!isPass) {
                    throw new ConditionNotSatisfiedException("The current request instance does not meet the condition '{}' configured in the condition comment, the current method is : {},  the current request is :{}",
                            condition,
                            context.getCurrentAnnotatedElement(),
                            request);
                }
            }
        }
    }

    protected String getRequestConditionFieldName() {
        return "value";
    }


}
