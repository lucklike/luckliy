package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.httpclient.core.convert.ProtobufAutoConvert;
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
import com.luckyframework.httpclient.proxy.slow.ResponseTimeSpent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.common.FontUtil.COLOR_RED;
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


        String logContent = StringUtils.format("{}[{}][{}][{}]{}{->}[{}]{}[{}]{}{}{}",
                isAsyncRequest(context) ? "[⚡]" : "",
                getHttpExeStr(context),
                request.getUniqueId(),
                getApiName(context),
                nameDesNotSame(context) ? "[" + getApiDesc(context) + "]" : "",
                request.getRequestMethod(),
                request.getContentType() == ContentType.NON ? "" : "[" + request.getContentType() + "]",
                getBaseUrl(request),
                ContainerUtils.isEmptyMap(request.getSimpleQueries()) ? "" : "[" + FontUtil.getCyanStr("QUERY:") + FontUtil.getCyanUnderline(contextTruncation(SerializationConstant.JSON_SCHEME.serialization(request.getSimpleQueries()), maxLength)) + "]",
                bodyStr.replace("\n", "").replace("\r", "").replace("\t", ""),
                ContainerUtils.isEmptyMap(request.getSimpleHeaders()) ? "" : "[" + FontUtil.getWhiteStr("HEADER:") + FontUtil.getWhiteUnderline(SerializationConstant.JSON_SCHEME.serialization(request.getSimpleHeaders())) + "]"
        );

        logger.info(tryRequestDataMask(context, logContent));
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

        ResponseTimeSpent responseTimeSpent = getSlowResponseInfo(context);
        if (isSlow(context, responseTimeSpent)) {
            timeColor = COLOR_RED;
            tag = "⚠️";
        } else {
            timeColor = respColor;
            tag = "";
        }

        // 响应体
        String bodyStr;
        if (isAllowMimeType(context, response)) {
            if (response.isProtobufBody()) {
                try {
                    Type convertMetaType = context.getConvertMetaType().getMetaType();
                    if (convertMetaType == Object.class) {
                        convertMetaType = context.getMethodConvertReturnResolvableType().resolve();
                    }
                    bodyStr = String.valueOf((Object) ProtobufAutoConvert.convertProtobuf(response, convertMetaType));
                } catch (Exception e) {
                    bodyStr = getLogResponseBody(context, response);
                }
            } else if (response.isJavaBody()) {
                bodyStr = response.javaObject().toString();
            } else {
                bodyStr = getLogResponseBody(context, response);
            }
        } else {
            Long contentLength = response.getContentLength();
            if (response.getContentType() == ContentType.NON) {
                bodyStr = StringUtils.format("Result of unknown type{}", contentLength == null ? "" : ", Size: " + contentLength);
            } else {
                bodyStr = StringUtils.format("Is a '{}' result{}", response.getContentType().getMimeType(), contentLength == null ? "" : ", Size: " + contentLength);
            }
        }

        String logContent = StringUtils.format("{}[{}][{}][{}]{}{<-}[{}][{}][{}][{}]{}",
                isAsyncRequest(context) ? "[⚡]" : "",
                getHttpExeStr(context),
                response.getRequest().getUniqueId(),
                getApiName(context),
                nameDesNotSame(context) ? "[" + getApiDesc(context) + "]" : "",
                tag + FontUtil.getColorStr(timeColor, UnitUtils.millisToTime(responseTimeSpent.getExeTime())),
                FontUtil.getColorStr(respColor, String.valueOf(response.getStatus())),
                url,
                FontUtil.getColorStr(respColor, "BODY:") + FontUtil.getUnderlineColorString(respColor, contextTruncation(bodyStr.replace("\n", "").replace("\r", "").replace("\t", ""), maxLength)),
                !isPrintRespHeader(context) || ContainerUtils.isEmptyMap(response.getSimpleHeaders()) ? "" : "[" + FontUtil.getWhiteStr("HEADER:") + FontUtil.getWhiteUnderline(SerializationConstant.JSON_SCHEME.serialization(response.getSimpleHeaders())) + "]"
        );

        logger.info(tryResponseDataMask(context, logContent));
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
            return "JDK";
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
