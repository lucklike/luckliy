package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.httpclient.core.executor.HttpClient5Executor;
import com.luckyframework.httpclient.core.executor.HttpClientExecutor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.executor.OkHttpExecutor;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.core.serialization.SerializationConstant;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.common.FontUtil.COLOR_RED;
import static com.luckyframework.common.FontUtil.COLOR_YELLOW;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_REDIRECT_URL_CHAIN_$;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$RETRY_COUNT$_;

/**
 * 简单的日志处理器
 */
public class SimpleLoggerPrintHandler extends PrintLogAnnotationContextLoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimpleLoggerPrintHandler.class);

    @Override
    protected void doRecordRequestLog(MethodContext context, Request request) throws Exception {
        long maxLength = getAllowPrintLogReqBodyMaxLength(context);

        String bodyStr = "";
        BodyObject body = request.getBody();
        if (body != null) {
            bodyStr = "[" + FontUtil.getCyanStr("BODY:") + FontUtil.getCyanUnderline(contextTruncation(getLogRequestBody(context, request), maxLength)) + FontUtil.getCyanStr("]");
        } else if (ContainerUtils.isNotEmptyMap(request.getMultipartFormParameters())) {
            bodyStr = "[" + FontUtil.getCyanStr("MULTIPART-FORM:") + FontUtil.getCyanUnderline(contextTruncation(multipartData2String(request.getMultipartFormParameters()), maxLength)) + "]";
        } else if (ContainerUtils.isNotEmptyMap(request.getFormParameters())) {
            bodyStr = "[" + FontUtil.getCyanStr("FORM:") + FontUtil.getCyanUnderline(contextTruncation(SerializationConstant.JSON_SCHEME.serialization(request.getFormParameters()), maxLength)) + "]";
        }

        logger.info("{}[{}][{}][{}]{}{->}[{}]{}[{}]{}{}{}",
                isAsyncRequest(context) ? "[⚡]" : "",
                getHttpExeStr(context),
                getUniqueId(context),
                getApiName(context),
                nameDesNotSame(context) ? "[" + getApiDesc(context) + "]" : "",
                request.getRequestMethod(),
                request.getContentType() == ContentType.NON ? "" : "[" + request.getContentType() + "]",
                getBaseUrl(request),
                ContainerUtils.isEmptyMap(request.getSimpleQueries()) ? "" : "[" + FontUtil.getCyanStr("QUERY:") + FontUtil.getCyanUnderline(contextTruncation(SerializationConstant.JSON_SCHEME.serialization(request.getSimpleQueries()), maxLength)) + "]",
                bodyStr,
                ContainerUtils.isEmptyMap(request.getSimpleHeaders()) ? "" : "[" + FontUtil.getWhiteStr("HEADER:") + FontUtil.getWhiteUnderline(SerializationConstant.JSON_SCHEME.serialization(request.getSimpleHeaders())) + "]"
        );
    }

    @Override
    protected void doRecordMetaResponseLog(MethodContext context, Response response) throws Exception {
        long maxLength = getAllowPrintLogRespBodyMaxLength(context);
        Integer retryCount = context.getRootVar(_$RETRY_COUNT$_, Integer.class);
        List<?> redirectChain = context.getRootVar($_REQUEST_REDIRECT_URL_CHAIN_$, List.class);
        String url = getBaseUrl(response.getRequest());
        if (redirectChain != null) {
            int redirectCount = redirectChain.size() - 1;
            url = "🛸" + redirectCount + "][" + url;
        }
        if (retryCount != null) {
            url = "🔁" + retryCount + "][" + url;
        }
        if (isMock(context)) {
            url = "🎭][" + url;
        }

        String respColor = getRespColor(response.getStatus());
        String timeColor;
        String tag;

        if (isSlow(context)) {
            timeColor = COLOR_RED;
            tag = "⚠️";
        } else if (isWarn(context)) {
            timeColor = COLOR_YELLOW;
            tag = "🐌";
        } else {
            timeColor = respColor;
            tag = "";
        }

        // 响应体
        String bodyStr;
        if (isAllowMimeType(context, response)) {
            bodyStr = getLogResponseBody(context, response);
        } else {
            long contentLength = response.getContentLength();
            if (response.getContentType() == ContentType.NON) {
                bodyStr = "Result of unknown type, size: " + contentLength;
            } else {
                bodyStr = StringUtils.format("Is a '{}' result, size: {}", response.getContentType().getMimeType(), contentLength);
            }
        }

        logger.info("{}[{}][{}][{}]{}{<-}[{}][{}][{}][{}]{}",
                isAsyncRequest(context) ? "[⚡]" : "",
                getHttpExeStr(context),
                getUniqueId(context),
                getApiName(context),
                nameDesNotSame(context) ? "[" + getApiDesc(context) + "]" : "",
                tag + FontUtil.getColorStr(timeColor, UnitUtils.millisToTime(getExeTime(context))),
                FontUtil.getColorStr(respColor, String.valueOf(response.getStatus())),
                url,
                FontUtil.getColorStr(respColor, "BODY:") + FontUtil.getUnderlineColorString(respColor, contextTruncation(bodyStr, maxLength)),
                !isPrintRespHeader(context) || ContainerUtils.isEmptyMap(response.getSimpleHeaders()) ? "" : "[" + FontUtil.getWhiteStr("HEADER:") + FontUtil.getWhiteUnderline(SerializationConstant.JSON_SCHEME.serialization(response.getSimpleHeaders())) + "]"
        );
    }


    private String contextTruncation(String text, long maxLength) {
        if (maxLength < 0 || text.length() <= maxLength) {
            return flat(text);
        }
        return flat(text.substring(0, (int) maxLength) + "...(limit: " + maxLength + ")...");
    }

    private String getHttpExeStr(MethodContext context) {
        HttpExecutor httpExecutor = context.getHttpExecutor();
        if (httpExecutor instanceof JdkHttpExecutor) {
            return "JDK" ;
        }
        if (httpExecutor instanceof HttpClient5Executor) {
            return "HTTP_CLIENT5";
        }
        if (httpExecutor instanceof HttpClientExecutor) {
            return "HTTP_CLIENT";
        }
        if (httpExecutor instanceof OkHttpExecutor) {
            return "OKHTTP";
        }
        return "?";
    }

    private String multipartData2String(Map<String, Object> mmap) throws Exception {
        Map<String, Object> resultMap = new LinkedHashMap<>(mmap.size());
        mmap.forEach((k, v) -> {
            if (HttpExecutor.isResourceParam(v)) {
                resultMap.put(k, Stream.of(HttpExecutor.toHttpFiles(v)).map(HttpFile::getDescriptor).collect(Collectors.toList()));

            } else {
                resultMap.put(k, v);
            }
        });
        return SerializationConstant.JSON_SCHEME.serialization(resultMap);
    }

    private String flat(String str) {
        return str.replaceAll("[\\r\\n]+", " ");
    }
}
