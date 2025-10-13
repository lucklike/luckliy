package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.httpclient.core.executor.HttpClient5Executor;
import com.luckyframework.httpclient.core.executor.HttpClientExecutor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.executor.OkHttpExecutor;
import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.core.serialization.SerializationConstant;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_UNIQUE_ID_$;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$HTTP_EXE_TIME_$;

/**
 * 简单的日志处理器
 */
public class SimpleLoggerPrintHandler extends PrintLogAnnotationContextLoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimpleLoggerPrintHandler.class);

    @Override
    protected void doRecordRequestLog(MethodContext context, Request request) throws Exception {
        long maxLength = getAllowPrintLogReqBodyMaxLength(context);
        logger.info("{}[{}]{->}<{}>[{}]{}[{}]{}{}{}{}{}",
                isAsyncRequest(context) ? "[⚡]" : "",
                getHttpExeStr(context),
                context.getRootVar($_UNIQUE_ID_$, String.class),
                request.getRequestMethod(),
                request.getContentType() == ContentType.NON ? "" : "[" + request.getContentType() + "]",
                request.getUrl(),
                ContainerUtils.isEmptyMap(request.getSimpleQueries()) ? "" : " query: " + contextTruncation(SerializationConstant.JSON_SCHEME.serialization(request.getSimpleQueries()), maxLength),
                request.getBody() == null ? "" : " body: " + contextTruncation(request.getBody().getBodyAsString(), maxLength),
                ContainerUtils.isEmptyMap(request.getFormParameters()) ? "" : " form: " + contextTruncation(SerializationConstant.JSON_SCHEME.serialization(request.getFormParameters()), maxLength),
                ContainerUtils.isEmptyMap(request.getMultipartFormParameters()) ? "" : " multipart-form: " + contextTruncation(SerializationConstant.JSON_SCHEME.serialization(request.getMultipartFormParameters()), maxLength),
                ContainerUtils.isEmptyMap(request.getSimpleHeaders()) ? "" : " header: " + SerializationConstant.JSON_SCHEME.serialization(request.getSimpleHeaders())
        );
    }

    @Override
    protected void doRecordMetaResponseLog(MethodContext context, Response response) throws Exception {
        long maxLength = getAllowPrintLogRespBodyMaxLength(context);
        logger.info("{}[{}]{<-}<{}>[{}][{}] {}{}",
                isAsyncRequest(context) ? "[⚡]" : "",
                getHttpExeStr(context),
                context.getRootVar($_UNIQUE_ID_$, String.class),
                UnitUtils.millisToTime(context.getRootVar(_$HTTP_EXE_TIME_$, long.class)),
                response.getStatus(),
                "body: " + contextTruncation(response.getStringResult(), maxLength),
                !isPrintRespHeader(context) || ContainerUtils.isEmptyMap(response.getSimpleHeaders()) ? "" : " header: " + SerializationConstant.JSON_SCHEME.serialization(response.getSimpleHeaders())
        );
    }


    private String contextTruncation(String text, long maxLength) {
        if (maxLength < 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, (int) maxLength) + "...(limit: " + maxLength + ")";
    }

    private String getHttpExeStr(MethodContext context) {
        HttpExecutor httpExecutor = context.getHttpExecutor();
        if (httpExecutor instanceof JdkHttpExecutor) {
            return "J";
        }
        if (httpExecutor instanceof HttpClient5Executor) {
            return "C5";
        }
        if (httpExecutor instanceof HttpClientExecutor) {
            return "C";
        }
        if (httpExecutor instanceof OkHttpExecutor) {
            return "O";
        }
        return "?";
    }

}
