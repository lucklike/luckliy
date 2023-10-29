package com.luckyframework.httpclient.proxy;

import com.luckyframework.spel.ParamWrapper;

import java.lang.annotation.Annotation;
import java.util.List;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_INSTANCE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD_CONTEXT;

/**
 * 静态参数解析器，用户将用户配置再注解中的信息转化为Http参数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 09:42
 */
@FunctionalInterface
public interface StaticParamResolver extends SupportSpELImport {

    /**
     * 参数解析，将注解解析成为参数集合
     *
     * @param context 上下文信息
     * @return 参数集合
     */
    List<ParamInfo> parser(MethodContext context, Annotation staticParamAnn);


    default Object parseExpression(String expression, MethodContext context, Annotation staticParamAnn) {
        return HttpClientProxyObjectFactory.getSpELConverter().parseExpression(
                getImportCompletedParamWrapper(context)
                        .setExpression(expression)
                        .addVariable(CLASS, context.getClassContext().getCurrentAnnotatedElement())
                        .addVariable(METHOD, context.getCurrentAnnotatedElement())
                        .addVariables(context.getCurrentAnnotatedElement(), context.getArguments())
                        .addVariable(CLASS_CONTEXT, context.getClassContext())
                        .addVariable(METHOD_CONTEXT, context)
                        .addVariable(ANNOTATION_INSTANCE, staticParamAnn)
                        .addVariables(context.getCurrentAnnotatedElement(), context.getArguments()));
    }
}
