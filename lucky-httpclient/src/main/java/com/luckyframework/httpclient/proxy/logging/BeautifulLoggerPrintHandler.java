package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.*;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.*;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.slow.ResponseTimeSpent;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.serializable.JacksonSerializationScheme;
import com.luckyframework.serializable.JaxbXmlSerializationScheme;
import com.luckyframework.web.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_REDIRECT_URL_CHAIN_$;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$RETRY_COUNT$_;

public class BeautifulLoggerPrintHandler extends PrintLogAnnotationContextLoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(BeautifulLoggerPrintHandler.class);

    private static final String CHART_STR = "\t";
    private static final String INDENT_STR = "\n\t";
    public static final String LINE_BREAK = "\n";
    public static final String FORM_DELIMITER = "&";
    private static final String HEADER_COLON = ":";
    private static final String HEADER_NAME_DELIMITER = "-";
    private static final String HEADER_VALUE_DELIMITER = "; ";
    private static final char ZERO_WIDTH_SPACE = '\u200B';
    private static final String REQUEST_TITLE = " REQUEST ";
    private static final String ASYNC_REQUEST_TITLE = "⚡️ REQUEST ⚡️";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data; boundary=LuckyBoundary";
    private static final String URL_ENCODED_FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

    @Override
    protected void doRecordRequestLog(MethodContext context, Request request) {
        logger.info(tryRequestDataMask(context, getRequestLogInfo(request, context)));
    }

    @Override
    protected void doRecordMetaResponseLog(MethodContext context, Response response) throws Exception {
        logger.info(tryResponseDataMask(context, getResponseLogInfo(response, context)));
    }

    private String getRequestLogInfo(Request request, MethodContext context) {
        StringBuilder logBuilder = new StringBuilder("\n>>");
        appendRequestTitle(logBuilder, context);
        appendInvocationContext(logBuilder, request, context);
        appendRequestUrl(logBuilder, request, context);
        appendReqHeaders(logBuilder, request);
        appendRequestBody(logBuilder, request, context);
        logBuilder.append(LINE_BREAK).append(">>");
        return logBuilder.toString();
    }

    private void appendRequestTitle(StringBuilder logBuilder, MethodContext context) {
        String title = isAsyncRequest(context) ? ASYNC_REQUEST_TITLE : REQUEST_TITLE;
        logBuilder.append(INDENT_STR).append(FontUtil.getBackCyanStr(title));
    }

    /**
     * 输出请求与响应日志共用的上下文信息：接口描述、线程名、请求ID、HTTP执行器、方法定位
     */
    private void appendInvocationContext(StringBuilder logBuilder, Request request, MethodContext context) {
        logBuilder.append(INDENT_STR).append("🔍 ");
        if (nameDesNotSame(context)) {
            logBuilder.append("[").append(FontUtil.getLightGrayUnderline(getApiDesc(context))).append("]");
        }
        logBuilder.append("[").append(FontUtil.getLightGrayUnderline(getThreadName())).append("][").append(FontUtil.getLightGrayUnderline(request.getUniqueId())).append("]");
        logBuilder.append(INDENT_STR).append("🛰️ ").append(FontUtil.getLightGrayStr(getHttpExecutorStr(context)));
        logBuilder.append(INDENT_STR).append("🎯️ ").append(FontUtil.getLightGrayStr(getMethodName(context)));
    }

    private void appendRequestUrl(StringBuilder logBuilder, Request request, MethodContext context) {
        logBuilder.append(LINE_BREAK).append(INDENT_STR)
                .append(FontUtil.getMulberryStr(request.getRequestMethod().toString())).append(" ")
                .append(FontUtil.getBlueUnderline(request.getUrl() + ZERO_WIDTH_SPACE)).append(" ")
                .append(FontUtil.getMulberryStr(context.getHttpExecutor().getHttpVersionString(request)));
        if (request.getProxyInfo() != null) {
            logBuilder.append(INDENT_STR).append(FontUtil.getRedStr("Proxy: ")).append(request.getProxyInfo().getProxy());
        }
    }

    /**
     * 输出请求体，依次为普通文本体、multipart表单体、urlencoded表单体三种形式
     * <p>表单体不再输出Content-Type行，该名称与真实请求头一同由{@link #appendReqHeaders}做列对齐</p>
     */
    private void appendRequestBody(StringBuilder logBuilder, Request request, MethodContext context) {
        long maxLength = getAllowPrintLogReqBodyMaxLength(context);
        BodyObject body = request.getBody();
        if (body != null) {
            appendTextBody(logBuilder, body, getLogRequestBody(context, request), maxLength);
        } else if (ContainerUtils.isNotEmptyMap(request.getMultipartFormParameters())) {
            appendMultipartBody(logBuilder, request, context, maxLength);
        } else if (ContainerUtils.isNotEmptyMap(request.getFormParameters())) {
            appendFormBody(logBuilder, request, maxLength);
        }
    }

    private void appendTextBody(StringBuilder logBuilder, BodyObject body, String logRequestBody, long maxLength) {
        logBuilder.append(LINE_BREAK);
        if (body.isJsonBody()) {
            logBuilder.append(FontUtil.getCyanStr(contextTruncation(jsonFormat(logRequestBody), maxLength)));
        } else if (body.isXmlBody()) {
            logBuilder.append(INDENT_STR).append(FontUtil.getCyanStr(contextTruncation(xmlFormat(logRequestBody).replace(LINE_BREAK, INDENT_STR), maxLength)));
        } else if (body.isFormBody()) {
            logBuilder.append(INDENT_STR).append(FontUtil.getCyanStr(contextTruncation(logRequestBody.replace(FORM_DELIMITER, FORM_DELIMITER + INDENT_STR), maxLength)));
        } else {
            logBuilder.append(INDENT_STR).append(FontUtil.getCyanStr(contextTruncation(logRequestBody.replace(LINE_BREAK, INDENT_STR), maxLength)));
        }
    }

    private void appendMultipartBody(StringBuilder logBuilder, Request request, MethodContext context, long maxLength) {
        logBuilder.append(LINE_BREAK)
                .append(contextTruncation(multipartData2String(context, request.getMultipartFormParameters()), maxLength));
    }

    /**
     * 将multipart表单数据渲染为边界分隔形式的文本
     */
    private String multipartData2String(MethodContext context, Map<String, Object> multipartFormParameters) {
        StringBuilder reqBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : multipartFormParameters.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (HttpExecutor.isResourceParam(value)) {
                appendMultipartFile(reqBuilder, name, HttpExecutor.toHttpFiles(value));
            } else {
                appendMultipartField(reqBuilder, context, name, value);
            }
        }
        reqBuilder.append(INDENT_STR).append(FontUtil.getYellowStr("--LuckyBoundary--"));
        return reqBuilder.toString();
    }

    private void appendMultipartFile(StringBuilder reqBuilder, String name, HttpFile[] httpFiles) {
        for (HttpFile httpFile : httpFiles) {
            reqBuilder.append(INDENT_STR).append(FontUtil.getYellowStr("--LuckyBoundary"));
            reqBuilder.append(INDENT_STR).append(FontUtil.getRedStr("Content-Disposition: ")).append("form-data; name=\"").append(name).append("\"").append("; filename=\"").append(httpFile.getFileName()).append("\"");
            String mimeType = ContentTypeUtils.getMimeTypeOrDefault(httpFile.getFileName(), "text/plain");
            reqBuilder.append(INDENT_STR).append(FontUtil.getRedStr("Content-Type: ")).append(mimeType);

            reqBuilder.append(LINE_BREAK).append(INDENT_STR).append(FontUtil.getBlueStr("< " + httpFile.getDescriptor()));
        }
    }

    private void appendMultipartField(StringBuilder reqBuilder, MethodContext context, String name, Object value) {
        reqBuilder.append(INDENT_STR).append(FontUtil.getYellowStr("--LuckyBoundary"));
        reqBuilder.append(INDENT_STR).append(FontUtil.getRedStr("Content-Disposition:")).append(" form-data; name=\"").append(name).append("\"");
        reqBuilder.append(INDENT_STR).append(FontUtil.getRedStr("Content-Type:")).append(" text/plain");
        String fieldValue = tryMaskFieldValue(context, name, value.toString(), true);
        reqBuilder.append(LINE_BREAK).append(INDENT_STR).append(FontUtil.getCyanStr(fieldValue.replace(LINE_BREAK, INDENT_STR)));
    }

    private void appendFormBody(StringBuilder logBuilder, Request request, long maxLength) {
        logBuilder.append(LINE_BREAK);
        StringBuilder reqBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : request.getFormParameters().entrySet()) {
            reqBuilder.append(INDENT_STR).append(entry.getKey()).append("=").append(entry.getValue()).append(FORM_DELIMITER);
        }
        logBuilder.append(FontUtil.getCyanStr(contextTruncation(removeEnd(reqBuilder.toString(), FORM_DELIMITER), maxLength)));
    }

    private String getResponseLogInfo(Response response, MethodContext context) throws Exception {
        Request request = response.getRequest();
        int status = response.getStatus();
        String color = getRespColor(status);

        StringBuilder logBuilder = new StringBuilder(LINE_BREAK).append("<<");
        logBuilder.append(INDENT_STR).append(FontUtil.getBackColorStr(color, getResponseTitle(context)));
        appendInvocationContext(logBuilder, request, context);
        appendResponseUrl(logBuilder, request, color);
        appendResponseStatus(logBuilder, request, status, context, color);
        appendRespHeaders(logBuilder, response.getHeaderManager(), context);
        appendResponseBody(logBuilder, response, color, context);

        logBuilder.append(LINE_BREAK).append("<<");
        return logBuilder.toString();
    }

    /**
     * 生成响应日志标题，标题中携带重试次数与重定向次数标记
     * <p>下面各分支的字面量（含括号前后的空格）均需与历史输出保持完全一致</p>
     */
    private String getResponseTitle(MethodContext context) {
        String tag = getResponseTag(context);
        boolean hasTag = StringUtils.hasText(tag);
        boolean async = isAsyncRequest(context);
        boolean mock = isMock(context);
        if (async && mock) {
            return hasTag ? "⚡️🎭 RESPONSE(" + tag + ") 🎭⚡️" : "⚡️🎭 RESPONSE 🎭⚡️";
        }
        if (async) {
            return hasTag ? "⚡️ RESPONSE (" + tag + ")⚡️" : "⚡️ RESPONSE ⚡️";
        }
        if (mock) {
            return hasTag ? " 🎭 RESPONSE(" + tag + ") 🎭 " : " 🎭 RESPONSE 🎭 ";
        }
        return hasTag ? " RESPONSE(" + tag + ")" : " RESPONSE ";
    }

    /**
     * 拼接重试与重定向标记，两者均不存在时返回空字符串
     */
    private String getResponseTag(MethodContext context) {
        String tag = "";
        List<?> redirectChain = context.getRootVar($_REQUEST_REDIRECT_URL_CHAIN_$, List.class);
        if (redirectChain != null) {
            tag = "🛸" + (redirectChain.size() - 1);
        }
        Integer retryCount = context.getRootVar(_$RETRY_COUNT$_, Integer.class);
        if (retryCount != null) {
            tag = "🔁" + retryCount + tag;
        }
        return tag;
    }

    private void appendResponseUrl(StringBuilder logBuilder, Request request, String color) {
        logBuilder.append(LINE_BREAK).append(INDENT_STR)
                .append(FontUtil.getColorStr(color, request.getRequestMethod().toString())).append(" ")
                .append(FontUtil.getUnderlineColorString(color, request.getUrl() + ZERO_WIDTH_SPACE));
    }

    private void appendResponseStatus(StringBuilder logBuilder, Request request, int status, MethodContext context, String color) {
        ResponseTimeSpent responseTimeSpent = getSlowResponseInfo(context);
        boolean slow = isSlow(context, responseTimeSpent);
        String timeColor = slow ? Color.RED.getColorCore() : color;
        String timeTag = slow ? "⚠️" : "";

        logBuilder.append(LINE_BREAK).append(INDENT_STR)
                .append(context.getHttpExecutor().getHttpVersionString(request)).append(" ")
                .append(FontUtil.getColorStr(color, "" + status))
                .append(" (").append(timeTag).append(FontUtil.getColorStr(timeColor, UnitUtils.millisToTime(responseTimeSpent.getExeTime()))).append(")");
    }

    private void appendRespHeaders(StringBuilder logBuilder, HttpHeaderManager headerManager, MethodContext context) {
        if (!isPrintRespHeader(context)) {
            return;
        }
        AlignedText alignedText = new AlignedText();
        for (Map.Entry<String, List<Header>> entry : headerManager.getHeaderMap().entrySet()) {
            for (Header header : entry.getValue()) {
                alignedText.add(FontUtil.getLightGrayStr(getStandardHeader(entry.getKey())), FontUtil.getLightGrayStr(HEADER_COLON), FontUtil.getLightGrayStr(String.valueOf(header.getValue())));
            }
        }
        logBuilder.append(LINE_BREAK).append(alignedText.format(CHART_STR));
    }

    private void appendResponseBody(StringBuilder logBuilder, Response response, String color, MethodContext context) throws Exception {
        long maxLength = getAllowPrintLogRespBodyMaxLength(context);
        logBuilder.append(LINE_BREAK);

        if (!isAllowMimeType(context, response)) {
            logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, getDiscardedBodyDescription(response)));
            return;
        }
        if (response.isJsonBody()) {
            logBuilder.append(FontUtil.getColorStr(color, contextTruncation(jsonFormat(getLogResponseBody(context, response)), maxLength)));
            return;
        }
        String bodyText;
        if (response.isXmlBody()) {
            bodyText = xmlFormat(getLogResponseBody(context, response));
        } else if (response.isJavaBody()) {
            bodyText = response.javaObject().toString();
        } else if (response.isProtobufBody()) {
            bodyText = getProtobufBodyAsString(context, response);
        } else {
            bodyText = getLogResponseBody(context, response);
        }
        logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, contextTruncation(bodyText.replace(LINE_BREAK, INDENT_STR), maxLength)));
    }

    /**
     * 输出请求头：名称列、冒号列、值列按列对齐（名称与冒号红色，值浅灰）
     * <p>冒号单独成一列（列宽固定为1），对齐后的字符位置与响应头将「: 」放在值列开头时完全一致</p>
     *
     * @param logBuilder 日志缓冲区
     * @param request    请求实例
     */
    private void appendReqHeaders(StringBuilder logBuilder, Request request) {
        AlignedText alignedText = new AlignedText();
        HttpHeaderManager httpHeaderManager = request.getHeaderManager();
        for (Map.Entry<String, List<Header>> entry : httpHeaderManager.getHeaderMap().entrySet()) {
            StringBuilder headerValueBuilder = new StringBuilder();
            for (Header header : FormatUtils.filterHeader(entry.getValue())) {
                headerValueBuilder.append(header.getValue()).append(HEADER_VALUE_DELIMITER);
            }
            String headerValue = removeEnd(headerValueBuilder.toString(), HEADER_VALUE_DELIMITER);
            alignedText.add(FontUtil.getRedStr(getStandardHeader(entry.getKey())), FontUtil.getRedStr(HEADER_COLON), FontUtil.getLightGrayStr(headerValue));
        }

        String bodyContentType = getBodyContentType(request);
        if (bodyContentType != null) {
            alignedText.add(FontUtil.getRedStr(CONTENT_TYPE_HEADER), FontUtil.getRedStr(HEADER_COLON), FontUtil.getLightGrayStr(bodyContentType));
        }
        if (alignedText.size() > 0) {
            logBuilder.append(LINE_BREAK).append(alignedText.format(CHART_STR));
        }
    }

    /**
     * 取出需要在请求头区域额外展示的请求体Content-Type
     * <p>
     * multipart与urlencoded表单体由{@link HttpExecutor}在发送时才写入Content-Type，请求头管理器中并没有这一项，
     * 故需补一行并让其与真实请求头参与同一次列对齐；文本请求体的Content-Type已存在于请求头管理器中，返回null避免重复输出。
     * 判断分支与{@link #appendRequestBody}保持互斥，确保只在确实打印了表单体时才补这行。
     *
     * @param request 请求实例
     * @return 需要补充输出的Content-Type值，无需补充时返回null
     */
    private String getBodyContentType(Request request) {
        if (request.getBody() != null) {
            return null;
        }
        if (ContainerUtils.isNotEmptyMap(request.getMultipartFormParameters())) {
            return MULTIPART_FORM_CONTENT_TYPE;
        }
        if (ContainerUtils.isNotEmptyMap(request.getFormParameters())) {
            return URL_ENCODED_FORM_CONTENT_TYPE;
        }
        return null;
    }

    private String getStandardHeader(String name) {
        List<String> strings = Stream.of(name.split(HEADER_NAME_DELIMITER)).map(StringUtils::capitalize).collect(Collectors.toList());
        return StringUtils.join(strings, HEADER_NAME_DELIMITER);
    }

    /**
     * 去除字符串末尾的指定后缀，末尾与后缀不匹配时原样返回
     */
    private static String removeEnd(String text, String suffix) {
        return text.endsWith(suffix) ? text.substring(0, text.length() - suffix.length()) : text;
    }

    private String xmlFormat(String xmlStr) {
        try {
            return JaxbXmlSerializationScheme.prettyPrintByTransformer(xmlStr);
        } catch (Exception e) {
            return xmlStr;
        }
    }

    private String jsonFormat(String jsonStr) {
        try {
            String json = JacksonSerializationScheme.prettyPrinting(jsonStr);
            json = json.replace(LINE_BREAK, INDENT_STR);
            return INDENT_STR + json;
        } catch (Exception e) {
            return INDENT_STR + jsonStr.replace(LINE_BREAK, INDENT_STR);
        }
    }

    private String contextTruncation(String text, long maxLength) {
        if (maxLength < 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, (int) maxLength) + "\n\n\t⇡...(limit:" + maxLength + ")...⇡";
    }

    private String getHttpExecutorStr(Context context) {
        return ClassUtils.getClassName(context.getHttpExecutor());
    }

}
