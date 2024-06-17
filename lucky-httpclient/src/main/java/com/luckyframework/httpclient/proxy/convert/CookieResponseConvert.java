package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.CookieSelect;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_LENGTH;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_TYPE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_STATUS;

/**
 * Cookie转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/16 07:54
 */
public class CookieResponseConvert extends AbstractSpELResponseConvert {

    @Override
    public <T> T convert(Response response, ConvertContext context) throws Throwable {
        CookieSelect ann = context.toAnnotation(CookieSelect.class);
        String expression = ann.expression();
        if (!StringUtils.hasText(expression)) {
            throw new IllegalArgumentException("The 'expression' property of the @CookieSelect annotation cannot be empty.");
        }
        T returnObject = context.parseExpression(expression, context.getRealMethodReturnType(), pws -> {
            pws.addRootVariables(response.getSimpleCookies());
            pws.addRootVariable(RESPONSE_STATUS, response.getStatus());
            pws.addRootVariable(CONTENT_LENGTH, response.getContentLength());
            pws.addRootVariable(CONTENT_TYPE, response.getContentType());
        });
        return returnObject != null ? returnObject : getDefaultValue(context);
    }

}
