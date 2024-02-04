package com.luckyframework.httpclient.proxy.url;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.annotations.DomainName;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import static com.luckyframework.httpclient.proxy.SpELUtils.createSpELArgs;
import static com.luckyframework.httpclient.proxy.SpELUtils.getContextParamWrapper;
import static com.luckyframework.httpclient.proxy.SpELUtils.parseExpression;

/**
 * 支持SpEL表达式的域名获取器，SpEL表达式部分需要写在#{}中
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 10:14
 */
public class SpELURLGetter implements URLGetter, DomainNameGetter {


    @Override
    public String getUrl(HttpRequestContext context) {
        return parseExpressionGetUrl(context, HttpRequest.ATTRIBUTE_URL);
    }

    @Override
    public String getDomainName(DomainNameContext context) {
        return parseExpressionGetUrl(context, DomainName.ATTRIBUTE_VALUE);
    }


    private String parseExpressionGetUrl(AnnotationContext context, String annotationAttribute) {
        String expression = context.getAnnotationAttribute(annotationAttribute, String.class);
        if (!StringUtils.hasText(expression)) {
            return "";
        }

        MethodContext methodContext = (MethodContext) context.getContext();
        return parseExpression(
                getContextParamWrapper(
                        methodContext,
                        createSpELArgs()
                                .setExpression(expression)
                                .setReturnType(String.class)
                                .extractSpELEnv()
                                .extractMethodContext(methodContext)
                                .extractAnnotationContext(context)
                )
        );
    }

}
