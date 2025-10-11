package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.UnitUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.core.serialization.SerializationConstant;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$HTTP_EXE_TIME_$;

/**
 * 简单的日志处理器
 */
public class SimpleLoggerHandler implements LoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimpleLoggerHandler.class);

    @Override
    public void recordRequestLog(MethodContext context, Request request) {
        try {
            logger.info("REQUEST -> [{}][{}] header:{}, query: {}, body: {}, form: {}, multipart-form: {}",
                    request.getRequestMethod(),
                    request.getUrl(),
                    SerializationConstant.JSON_SCHEME.serialization(request.getSimpleHeaders()),
                    SerializationConstant.JSON_SCHEME.serialization(request.getSimpleQueries()),
                    request.getBody() == null ? "{}" : request.getBody().getBodyAsString(),
                    SerializationConstant.JSON_SCHEME.serialization(request.getFormParameters()),
                    SerializationConstant.JSON_SCHEME.serialization(request.getMultipartFormParameters())
            );
        } catch (Exception e) {
            logger.error("An exception occurred while printing the request log.", e);
        }


    }

    @Override
    public void recordMetaResponseLog(MethodContext context, Response response) {
        try {
            logger.info("RESPONSE <- [{}][{}] {}",
                    UnitUtils.millisToTime(context.getRootVar(_$HTTP_EXE_TIME_$, long.class)),
                    response.getStatus(),
                    response.getStringResult().replace("\n", "").replace("\t", "")
            );
        } catch (Exception e) {
            logger.error("An exception occurred while printing the response log.", e);
        }

    }
}
