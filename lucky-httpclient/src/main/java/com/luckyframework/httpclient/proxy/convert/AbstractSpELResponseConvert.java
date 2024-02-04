package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.exception.ResponseProcessException;
import com.luckyframework.httpclient.proxy.SpELUtils;
import com.luckyframework.httpclient.proxy.annotations.ResultSelect;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.spel.ParamWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用的基于SpEL表达式的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 01:55
 */
public abstract class AbstractSpELResponseConvert implements ResponseConvert {

    private static final Logger log = LoggerFactory.getLogger(AbstractSpELResponseConvert.class);

    protected Object getBodyResult(Response response) {
        if (response.isJsonType()) {
            return response.jsonStrToEntity(Object.class);
        }
        if (response.isXmlType()) {
            return response.xmlStrToEntity(Object.class);
        }
        return null;
    }

    protected <T> T getMethodResult(Response response, MethodContext methodContext) {
        return response.getEntity(methodContext.getRealMethodReturnType());
    }

    protected <T> T getDefaultValue(Response response, ConvertContext context) {
        String defaultValueSpEL = context.getAnnotationAttribute(ResultSelect.ATTRIBUTE_DEFAULT_VALUE, String.class);
        String exMsg = context.getAnnotationAttribute(ResultSelect.ATTRIBUTE_EX_MSG, String.class);
        if (StringUtils.hasText(defaultValueSpEL)) {
            if (log.isDebugEnabled()) {
                log.debug("The current request returns the default value :{}", defaultValueSpEL);
            }
            return SpELUtils.parseExpression(
                    getResponseSpElParamWrapper(response, context)
                            .setExpression(defaultValueSpEL)
                            .setExpectedResultType(context.getContext().getRealMethodReturnType())
            );
        }
        if (StringUtils.hasText(exMsg)) {
            throw new ResponseProcessException(
                    String.valueOf(
                            SpELUtils.parseExpression(
                                    getResponseSpElParamWrapper(response, context)
                                            .setExpression(exMsg)
                            )
                    )
            );
        }
        return null;
    }

    protected <T> T parserSpELExpression(String expression, Response response, ConvertContext context) {
        return SpELUtils.parseExpression(
                getResponseSpElParamWrapper(response, context)
                        .setExpression(expression)
                        .setExpectedResultType(context.getContext().getRealMethodReturnType())
        );
    }

    protected ParamWrapper getResponseSpElParamWrapper(Response response, ConvertContext context) {
        return SpELUtils.getContextParamWrapper(context.getContext(),
                SpELUtils.createSpELArgs()
                        .extractSpELEnv()
                        .extractMethodContext(context.getContext())
                        .extractAnnotationContext(context)
                        .extractResponse(response)
                        .extractRequest(response.getRequest())
        );
    }
}
