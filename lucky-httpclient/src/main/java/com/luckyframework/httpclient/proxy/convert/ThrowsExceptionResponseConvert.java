package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.Ex;
import com.luckyframework.httpclient.proxy.annotations.Throws;

/**
 * 抛异常的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/29 9:51
 */
public class ThrowsExceptionResponseConvert extends AbstractSpELResponseConvert {

    @Override
    public <T> T convert(Response response, ConvertContext context) throws Throwable {
        Throws throwsAnn = context.toAnnotation(Throws.class);
        Ex[] exes = throwsAnn.value();

        for (Ex ex : exes) {
            boolean assertion = context.parseExpression(ex.assertion(), boolean.class);
            if (assertion) {
                throwException(context, ex.exception());
            }
        }
        String result = throwsAnn.result();
        return StringUtils.hasText(result)
                ? context.parseExpression(result, context.getRealMethodReturnType())
                : getMethodResult(response, context.getContext());
    }
}
