package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.annotations.VoidResponseSelect;

/**
 * 基于SpEL表达式的返回值结果转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/17 11:27
 */
public class SpELVoidResponseSelectConvert extends AbstractSpELVoidResponseConvert {

    @Override
    public <T> T convert(VoidResponse voidResponse, ConvertContext context) {
        // 获取表达式配置
        String expression = context.toAnnotation(VoidResponseSelect.class).value();

        // 解析SpEL表达式获取结果
        T returnObject = context.parseExpression(expression, context.getRealMethodReturnType());
        return returnObject != null ? returnObject : getDefaultValue(context);
    }
}
