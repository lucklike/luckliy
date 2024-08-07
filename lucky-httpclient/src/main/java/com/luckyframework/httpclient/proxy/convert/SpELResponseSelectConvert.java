package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.RespSelect;

/**
 * 基于SpEL表达式的返回值结果转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/17 11:27
 */
public class SpELResponseSelectConvert extends AbstractSpELResponseConvert {

    @Override
    public <T> T convert(Response response, ConvertContext context) throws Throwable {
        // 获取表达式配置
        String expression = context.toAnnotation(RespSelect.class).expression();

        // 没有做任何配置时，直接对返回值进行转换
        if (!StringUtils.hasText(expression)) {
            return getMethodResult(response, context.getContext());
        }

        // 解析SpEL表达式获取结果
        T returnObject = context.parseExpression(expression, context.getRealMethodReturnType());
        return returnObject != null ? returnObject : getDefaultValue(context, response);
    }
}
