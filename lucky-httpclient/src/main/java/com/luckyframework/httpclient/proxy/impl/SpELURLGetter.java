package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.URLGetter;
import com.luckyframework.spel.ParamWrapper;
import org.springframework.expression.common.TemplateParserContext;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD_CONTEXT;

/**
 * 支持SpEL表达式的域名获取器，SpEL表达式部分需要写在#{}中
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 10:14
 */
public class SpELURLGetter implements URLGetter {


    @Override
    public String getUrl(String configValue, MethodContext methodContext) {
        return HttpClientProxyObjectFactory.getSpELConverter().parseExpression(
                getImportCompletedParamWrapper(methodContext)
                        .setExpression(configValue)
                        .setRootObject(methodContext.getCurrentAnnotatedElement(), methodContext.getArguments(), HttpClientProxyObjectFactory.getExpressionParams())
                        .addVariable(METHOD_CONTEXT, methodContext)
                        .addVariable(CLASS_CONTEXT, methodContext.getClassContext())
                        .addVariable(CLASS, methodContext.getClassContext().getCurrentAnnotatedElement())
                        .addVariable(METHOD, methodContext.getCurrentAnnotatedElement())
                        .setExpectedResultType(String.class));
    }
}
