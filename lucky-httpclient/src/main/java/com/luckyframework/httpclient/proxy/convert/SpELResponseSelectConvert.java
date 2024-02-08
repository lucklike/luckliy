package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.annotations.SpElSelect;

/**
 * 基于SpEL表达式的返回值结果转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/17 11:27
 */
public class SpELResponseSelectConvert extends AbstractSpELResponseConvert {

    @Override
    public <T> T convert(Response response, ConvertContext context) {
        // 获取表达式配置
        String expression = context.toAnnotation(SpElSelect.class).expression();

        // 没有做任何配置时，直接对返回值进行转换
        if (!StringUtils.hasText(expression)) {
            return getMethodResult(response, context.getContext());
        }

        // 获取结果
        Object result = getBodyResult(response);

        // 结果为null时返回默认配置
        if (result == null) {
            return getDefaultValue(response, context);
        }

        // 解析SpEL表达式获取结果
        T returnObject = context.parseExpression(expression, context.getRealMethodReturnType(), getSpElArgConsumer(response));
        return returnObject != null ? returnObject : getDefaultValue(response, context);
    }
}
