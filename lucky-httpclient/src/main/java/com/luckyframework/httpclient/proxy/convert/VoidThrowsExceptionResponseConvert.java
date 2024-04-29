package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.annotations.Ex;
import com.luckyframework.httpclient.proxy.annotations.Throws;

/**
 * 抛异常的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/29 9:51
 */
public class VoidThrowsExceptionResponseConvert extends AbstractSpELVoidResponseConvert {

    @Override
    public <T> T convert(VoidResponse voidResponse, ConvertContext context) throws Throwable {
        Throws throwsAnn = context.toAnnotation(Throws.class);
        Ex[] exes = throwsAnn.value();

        for (Ex ex : exes) {
            boolean assertion = context.parseExpression(ex.assertion(), boolean.class);
            if (assertion) {
                Object exObj = context.parseExpression(ex.message());
                if (exObj instanceof Throwable) {
                    throw (Throwable) exObj;
                }
                throw new ActivelyThrownException(String.valueOf(exObj));
            }
        }
        return null;
    }
}
