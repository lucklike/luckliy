package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.exception.ResponseProcessException;
import com.luckyframework.httpclient.proxy.annotations.VoidResultConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用的基于SpEL表达式的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 01:55
 */
public abstract class AbstractSpELVoidResponseConvert implements VoidResponseConvert {

    private static final Logger log = LoggerFactory.getLogger(AbstractSpELVoidResponseConvert.class);


    protected <T> T getDefaultValue(ConvertContext context) throws Throwable {
        VoidResultConvert voidResultConvertAnn = context.toAnnotation(VoidResultConvert.class);
        String defaultValueSpEL = voidResultConvertAnn.defaultValue();
        String exception = voidResultConvertAnn.exception();
        if (StringUtils.hasText(defaultValueSpEL)) {
            if (log.isDebugEnabled()) {
                log.debug("The current request returns the default value :{}", defaultValueSpEL);
            }
            return context.parseExpression(defaultValueSpEL, context.getRealMethodReturnType());
        }
        if (StringUtils.hasText(exception)) {
            Object exObj = context.parseExpression(exception);
            if (exObj instanceof Throwable) {
                throw (Throwable) exObj;
            }
            throw new ResponseProcessException(
                    String.valueOf((Object) context.parseExpression(exception))
            );
        }
        return null;
    }
}
