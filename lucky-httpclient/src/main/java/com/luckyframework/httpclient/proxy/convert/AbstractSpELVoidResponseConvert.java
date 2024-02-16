package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.exception.ResponseProcessException;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.VoidResultConvert;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.SpELUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 通用的基于SpEL表达式的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 01:55
 */
public abstract class AbstractSpELVoidResponseConvert implements VoidResponseConvert {

    private static final Logger log = LoggerFactory.getLogger(AbstractSpELVoidResponseConvert.class);


    protected <T> T getDefaultValue(VoidResponse voidResponse, ConvertContext context) {
        VoidResultConvert voidResultConvertAnn = context.toAnnotation(VoidResultConvert.class);
        String defaultValueSpEL = voidResultConvertAnn.defaultValue();
        String exMsg = voidResultConvertAnn.exMsg();
        if (StringUtils.hasText(defaultValueSpEL)) {
            if (log.isDebugEnabled()) {
                log.debug("The current request returns the default value :{}", defaultValueSpEL);
            }
            return context.parseExpression(
                    defaultValueSpEL,
                    context.getRealMethodReturnType(),
                    getSpElArgConsumer(voidResponse)
            );
        }
        if (StringUtils.hasText(exMsg)) {
            throw new ResponseProcessException(
                    String.valueOf((Object) context.parseExpression(exMsg, getSpElArgConsumer(voidResponse)))
            );
        }
        return null;
    }

    protected Consumer<SpELUtils.ExtraSpELArgs> getSpElArgConsumer(VoidResponse voidResponse) {
        return arg -> arg.extractVoidResponse(voidResponse).extractRequest(voidResponse.getRequest());
    }
}
