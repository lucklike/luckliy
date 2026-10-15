package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.*;
import com.luckyframework.httpclient.core.executor.*;
import com.luckyframework.httpclient.core.meta.*;
import com.luckyframework.httpclient.core.serialization.SerializationConstant;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.slow.ResponseTimeSpent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_REDIRECT_URL_CHAIN_$;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$RETRY_COUNT$_;

/**
 * 简单的日志处理器
 */
public class SimpleLoggerPrintHandler extends PrintLogAnnotationContextLoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimpleLoggerPrintHandler.class);

    /**
     * HTTP执行器类型与日志中展示简称的映射关系，按声明顺序做instanceof匹配，未匹配到时展示"?"
     */
    private static final Map<Class<? extends HttpExecutor>, String> EXECUTOR_NAME_MAPPINGS = new LinkedHashMap<>();

    static {
        EXECUTOR_NAME_MAPPINGS.put(JdkHttpExecutor.class, "JDK");
        EXECUTOR_NAME_MAPPINGS.put(HttpClient5Executor.class, "HTTP_CLIENT5");
        EXECUTOR_NAME_MAPPINGS.put(HttpClientExecutor.class, "HTTP_CLIENT");
        EXECUTOR_NAME_MAPPINGS.put(OkHttpExecutor.class, "OKHTTP");
    }

    @Override
    protected void doRecordRequestLog(MethodContext context, Request request) throws Exception {
        long maxLength = getAllowPrintLogReqBodyMaxLength(context);

        String logContent = StringUtils.format("[📤][{}]➡️{}[{}][{}]{}[{}]{}[{}]{}{}{}",
                FontUtil.getSkyBlueUnderline(request.getUniqueId()),
                getAsyncTag(context),
                getHttpExeStr(context),
                FontUtil.getLightGrayUnderline(getApiName(context)),
                getApiDescTag(context),
                request.getRequestMethod(),
                getContentTypeTag(request),
                getBaseUrl(request),
                getQuerySegment(request, maxLength),
                stripLineChars(getBodySegment(context, request, maxLength)),
                getRequestHeaderSegment(request)
        );

        logger.info(tryRequestDataMask(context, logContent));
    }

    /**
     * 拼接请求体片段，无请求体时返回空字符串
     */
    private String getBodySegment(MethodContext context, Request request, long maxLength) throws Exception {
        BodyObject body = request.getBody();
        if (body != null) {
            return "[" + FontUtil.getCyanStr("BODY:") + FontUtil.getCyanUnderline(truncateAndFlatten(getLogRequestBody(context, request), maxLength)) + FontUtil.getCyanStr("]");
        }
        if (ContainerUtils.isNotEmptyMap(request.getMultipartFormParameters())) {
            return "[" + FontUtil.getCyanStr("MULTIPART-FORM:") + FontUtil.getCyanUnderline(truncateAndFlatten(multipartData2String(request.getMultipartFormParameters()), maxLength)) + "]";
        }
        if (ContainerUtils.isNotEmptyMap(request.getFormParameters())) {
            return "[" + FontUtil.getCyanStr("FORM:") + FontUtil.getCyanUnderline(truncateAndFlatten(SerializationConstant.JSON_SCHEME.serialization(request.getFormParameters()), maxLength)) + "]";
        }
        return "";
    }

    /**
     * 拼接请求查询参数片段，无查询参数时返回空字符串
     */
    private String getQuerySegment(Request request, long maxLength) throws Exception {
        if (ContainerUtils.isEmptyMap(request.getSimpleQueries())) {
            return "";
        }
        return "[" + FontUtil.getCyanStr("QUERY:") + FontUtil.getCyanUnderline(truncateAndFlatten(SerializationConstant.JSON_SCHEME.serialization(request.getSimpleQueries()), maxLength)) + "]";
    }

    /**
     * 拼接请求头片段，无请求头时返回空字符串
     */
    private String getRequestHeaderSegment(Request request) throws Exception {
        if (ContainerUtils.isEmptyMap(request.getSimpleHeaders())) {
            return "";
        }
        return "[" + FontUtil.getLightGrayStr("HEADER:") + FontUtil.getLightGrayUnderline(SerializationConstant.JSON_SCHEME.serialization(request.getSimpleHeaders())) + "]";
    }

    /**
     * 异步请求标识片段
     */
    private String getAsyncTag(MethodContext context) {
        return isAsyncRequest(context) ? "[⚡]" : "";
    }

    /**
     * 接口描述片段，描述与方法名相同时不展示
     */
    private String getApiDescTag(MethodContext context) {
        return nameDesNotSame(context) ? "[" + getApiDesc(context) + "]" : "";
    }

    /**
     * 内容类型片段，无内容类型时不展示
     */
    private String getContentTypeTag(Request request) {
        return request.getContentType() == ContentType.NON ? "" : "[" + request.getContentType() + "]";
    }

    @Override
    protected void doRecordMetaResponseLog(MethodContext context, Response response) throws Exception {
        long maxLength = getAllowPrintLogRespBodyMaxLength(context);

        String respColor = getRespColor(response.getStatus());
        ResponseTimeSpent responseTimeSpent = getSlowResponseInfo(context);
        boolean slow = isSlow(context, responseTimeSpent);
        String timeColor = slow ? Color.RED.getColorCore() : respColor;
        String tag = slow ? "⚠️" : "";

        String bodyStr = truncateAndFlatten(stripLineChars(resolveResponseBody(context, response)), maxLength);

        String logContent = StringUtils.format("[📩][{}]⬅️{}[{}][{}]{}[{}][{}][{}][{}]{}",
                FontUtil.getSkyBlueUnderline(response.getRequest().getUniqueId()),
                getAsyncTag(context),
                getHttpExeStr(context),
                FontUtil.getLightGrayUnderline(getApiName(context)),
                getApiDescTag(context),
                tag + FontUtil.getColorStr(timeColor, UnitUtils.millisToTime(responseTimeSpent.getExeTime())),
                FontUtil.getColorStr(respColor, String.valueOf(response.getStatus())),
                buildResponseUrl(context, response.getRequest()),
                FontUtil.getColorStr(respColor, "BODY:") + FontUtil.getUnderlineColorString(respColor, bodyStr),
                getResponseHeaderSegment(context, response)
        );

        logger.info(tryResponseDataMask(context, logContent));
    }

    /**
     * 拼接响应日志中的URL，需带上Mock、重试、重定向等前置标记
     */
    private String buildResponseUrl(MethodContext context, Request request) {
        String url = getBaseUrl(request);
        List<?> redirectChain = context.getRootVar($_REQUEST_REDIRECT_URL_CHAIN_$, List.class);
        if (redirectChain != null) {
            int redirectCount = redirectChain.size() - 1;
            url = "🛸" + redirectCount + "][" + url;
        }
        Integer retryCount = context.getRootVar(_$RETRY_COUNT$_, Integer.class);
        if (retryCount != null) {
            url = "🔁" + retryCount + "][" + url;
        }
        if (isMock(context)) {
            url = "🎭][" + url;
        }
        return url;
    }

    /**
     * 解析用于日志打印的响应体文本
     */
    private String resolveResponseBody(MethodContext context, Response response) {
        if (!isAllowMimeType(context, response)) {
            return getDiscardedBodyDescription(response);
        }
        if (response.isProtobufBody()) {
            return getProtobufBodyAsString(context, response);
        }
        if (response.isJavaBody()) {
            return response.javaObject().toString();
        }
        return getLogResponseBody(context, response);
    }

    /**
     * 拼接响应头片段，未开启响应头打印或无响应头时返回空字符串
     */
    private String getResponseHeaderSegment(MethodContext context, Response response) throws Exception {
        if (!isPrintRespHeader(context) || ContainerUtils.isEmptyMap(response.getSimpleHeaders())) {
            return "";
        }
        return "[" + FontUtil.getLightGrayStr("HEADER:") + FontUtil.getLightGrayUnderline(SerializationConstant.JSON_SCHEME.serialization(response.getSimpleHeaders())) + "]";
    }

    /**
     * 截断超长文本，并将换行压平为单行展示
     */
    private String truncateAndFlatten(String text, long maxLength) {
        if (maxLength < 0 || text.length() <= maxLength) {
            return flat(text);
        }
        return flat(text.substring(0, (int) maxLength) + "...(limit: " + maxLength + ")...");
    }

    /**
     * 去除文本中的换行符与制表符，保证单行输出
     */
    private String stripLineChars(String text) {
        return text.replace("\n", "").replace("\r", "").replace("\t", "");
    }

    private String getHttpExeStr(MethodContext context) {
        HttpExecutor httpExecutor = context.getHttpExecutor();
        for (Map.Entry<Class<? extends HttpExecutor>, String> mapping : EXECUTOR_NAME_MAPPINGS.entrySet()) {
            if (mapping.getKey().isInstance(httpExecutor)) {
                return mapping.getValue();
            }
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
