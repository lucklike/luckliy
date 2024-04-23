package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.exception.ResponseProcessException;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
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
public abstract class AbstractSpELResponseConvert implements ResponseConvert {

    private static final Logger log = LoggerFactory.getLogger(AbstractSpELResponseConvert.class);

    protected <T> T getMethodResult(Response response, MethodContext methodContext) {
        return response.getEntity(methodContext.getRealMethodReturnType());
    }

    protected <T> T getDefaultValue(Response response, ConvertContext context) {
        ResultConvert resultConvertAnn = context.toAnnotation(ResultConvert.class);
        String defaultValueSpEL = resultConvertAnn.defaultValue();
        String exMsg = resultConvertAnn.exMsg();
        if (StringUtils.hasText(defaultValueSpEL)) {
            if (log.isDebugEnabled()) {
                log.debug("The current request returns the default value :{}", defaultValueSpEL);
            }
            return context.parseExpression(
                    defaultValueSpEL,
                    context.getRealMethodReturnType(),
                    getContextParamSetter(context, response)
            );
        }
        if (StringUtils.hasText(exMsg)) {
            throw new ResponseProcessException(
                    String.valueOf((Object) context.parseExpression(exMsg, getContextParamSetter(context, response)))
            );
        }
        return null;
    }

    protected Consumer<ContextParamWrapper> getContextParamSetter(AnnotationContext context, Response response) {
        return cpw -> cpw.extractResponse(response, context.getConvertMetaType()).extractRequest(response.getRequest());
    }
}
