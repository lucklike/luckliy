package com.luckyframework.httpclient.proxy.impl.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.MethodContext;

import java.lang.annotation.Annotation;

/**
 * 基于SpEL表达式的返回值结果转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/17 11:27
 */
public class SpELResponseSelectConvert extends AbstractSpELResponseConvert {

    @Override
    public <T> T convert(Response response, MethodContext methodContext, Annotation resultConvertAnn) {
        // 获取表达式配置
        String expression = methodContext.getAnnotationAttribute(resultConvertAnn, "expression", String.class);

        // 没有做任何配置时，直接对返回值进行转换
        if (!StringUtils.hasText(expression)) {
            return getMethodResult(response, methodContext);
        }

        // 获取结果
        Object result = getResponseResult(response);

        // 结果为null时返回默认配置
        if (result == null) {
            return getDefaultValue(response, methodContext, resultConvertAnn);
        }

        // 解析SpEL表达式获取结果
        T returnObject = parserSpELExpression(expression, response, methodContext);
        return returnObject != null ? returnObject : getDefaultValue(response, methodContext, resultConvertAnn);
    }
}
