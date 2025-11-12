package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.Console;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.httpclient.core.convert.ProtobufAutoConvert;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.serializable.JacksonSerializationScheme;
import com.luckyframework.serializable.JaxbXmlSerializationScheme;
import com.luckyframework.web.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.common.FontUtil.COLOR_RED;
import static com.luckyframework.common.FontUtil.COLOR_YELLOW;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_REDIRECT_URL_CHAIN_$;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$RETRY_COUNT$_;

public class BeautifulLoggerPrintHandler extends PrintLogAnnotationContextLoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(BeautifulLoggerPrintHandler.class);

    private final String INDENT_STR = "\n\t";
    public static final String LINE_BREAK = "\n";
    public static final String FORM_DELIMITER = "&";

    @Override
    protected void doRecordRequestLog(MethodContext context, Request request) {
        logger.info(getRequestLogInfo(request, context));
    }

    @Override
    protected void doRecordMetaResponseLog(MethodContext context, Response response) throws Exception {
        logger.info(getResponseLogInfo(response.getStatus(), response.getRequest(), response.getHeaderManager(), response, context));
    }

    private String getRequestLogInfo(Request request, MethodContext context) {
        StringBuilder logBuilder = new StringBuilder("\n>>");
        String TITLE_SYNC = " REQUEST ";
        String TITLE_ASYNC = "⚡️REQUEST⚡️";
        String title = isAsyncRequest(context) ? TITLE_ASYNC : TITLE_SYNC;

        logBuilder.append(INDENT_STR).append(FontUtil.getBackCyanStr(title));
        logBuilder.append(INDENT_STR).append("🔍 ").append("[").append(FontUtil.getWhiteUnderline(getThreadName())).append("][").append(FontUtil.getWhiteUnderline(getUniqueId(context))).append("]");
        if (nameDesNotSame(context)) {
            logBuilder.append("[").append(FontUtil.getWhiteUnderline(getApiDesc(context))).append("]");
        }
        logBuilder.append(INDENT_STR).append("🛰️ ").append(FontUtil.getWhiteStr(getHttpExecutorStr(context)));
        logBuilder.append(INDENT_STR).append("🎯️ ").append(FontUtil.getWhiteStr(getMethodName(context)));


        logBuilder.append(LINE_BREAK).append(INDENT_STR).append(FontUtil.getMulberryStr(request.getRequestMethod().toString())).append(" ").append(FontUtil.getBlueUnderline(request.getUrl())).append(" ").append(FontUtil.getMulberryStr(context.getHttpExecutor().getHttpVersionString(request)));
        if (request.getProxyInfo() != null) {
            logBuilder.append(INDENT_STR).append(Console.getRedString("Proxy: ")).append(request.getProxyInfo().getProxy());
        }
        appendReqHeaders(logBuilder, request.getHeaderManager());

        long maxLength = getAllowPrintLogReqBodyMaxLength(context);
        BodyObject body = request.getBody();
        if (body != null) {
            logBuilder.append(LINE_BREAK);
            if (body.isJsonBody()) {
                logBuilder.append(FontUtil.getCyanStr(contextTruncation(jsonFormat(body.getBodyAsString()), maxLength)));
            } else if (body.isXmlBody()) {
                logBuilder.append(INDENT_STR).append(FontUtil.getCyanStr(contextTruncation(xmlFormat(body.getBodyAsString()).replace(LINE_BREAK, INDENT_STR), maxLength)));
            } else if (body.isFormBody()) {
                logBuilder.append(INDENT_STR).append(FontUtil.getCyanStr(contextTruncation(body.getBodyAsString().replace(FORM_DELIMITER, FORM_DELIMITER + INDENT_STR), maxLength)));
            } else {
                logBuilder.append(INDENT_STR).append(FontUtil.getCyanStr(contextTruncation(body.getBodyAsString().replace(LINE_BREAK, INDENT_STR), maxLength)));
            }

        } else if (ContainerUtils.isNotEmptyMap(request.getMultipartFormParameters())) {
            logBuilder.append(INDENT_STR).append(Console.getRedString("Content-Type: ")).append("multipart/form-data; boundary=LuckyBoundary").append(LINE_BREAK);

            StringBuilder reqBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : request.getMultipartFormParameters().entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                if (HttpExecutor.isResourceParam(value)) {
                    HttpFile[] httpFiles = HttpExecutor.toHttpFiles(value);
                    for (HttpFile httpFile : httpFiles) {
                        String descriptor = httpFile.getDescriptor();
                        reqBuilder.append(INDENT_STR).append(Console.getYellowString("--LuckyBoundary"));
                        reqBuilder.append(INDENT_STR).append(Console.getRedString("Content-Disposition: ")).append("form-data; name=\"").append(name).append("\"").append("; filename=\"").append(httpFile.getFileName()).append("\"");
                        String mimeType = ContentTypeUtils.getMimeTypeOrDefault(descriptor.endsWith("]") ? descriptor.substring(0, descriptor.length() - 1) : descriptor, "text/plain");
                        reqBuilder.append(INDENT_STR).append(Console.getRedString("Content-Type: ")).append(mimeType);

                        reqBuilder.append(LINE_BREAK).append(INDENT_STR).append(Console.getBlueString("< " + descriptor));
                    }
                } else {
                    reqBuilder.append(INDENT_STR).append(Console.getYellowString("--LuckyBoundary"));
                    reqBuilder.append(INDENT_STR).append(Console.getRedString("Content-Disposition:")).append(" form-data; name=\"").append(name).append("\"");
                    reqBuilder.append(INDENT_STR).append(Console.getRedString("Content-Type:")).append(" text/plain");
                    reqBuilder.append(LINE_BREAK).append(INDENT_STR).append(Console.getCyanString(value));
                }
            }
            reqBuilder.append(INDENT_STR).append(Console.getYellowString("--LuckyBoundary--"));

            logBuilder.append(contextTruncation(reqBuilder.toString(), maxLength));

        } else if (ContainerUtils.isNotEmptyMap(request.getFormParameters())) {
            logBuilder.append(INDENT_STR).append(Console.getRedString("Content-Type: ")).append("application/x-www-form-urlencoded");
            logBuilder.append(LINE_BREAK);
            StringBuilder reqBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : request.getFormParameters().entrySet()) {
                reqBuilder.append(INDENT_STR).append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            logBuilder.append(Console.getCyanString(contextTruncation(reqBuilder.toString().endsWith("&") ? reqBuilder.substring(0, reqBuilder.length() - 1) : reqBuilder.toString(), maxLength)));
        }
        logBuilder.append(LINE_BREAK).append(">>");
        return logBuilder.toString();
    }

    private String getResponseLogInfo(int status, Request request, HttpHeaderManager headerManager, Response response, MethodContext context) throws Exception {
        StringBuilder logBuilder = new StringBuilder(LINE_BREAK);
        String color = getRespColor(status);

        Integer retryCount = context.getRootVar(_$RETRY_COUNT$_, Integer.class);
        List<?> redirectChain = context.getRootVar($_REQUEST_REDIRECT_URL_CHAIN_$, List.class);

        String title;
        String tag = "";
        if (redirectChain != null) {
            tag = "🛸" + (redirectChain.size() - 1);
        }
        if (retryCount != null) {
            tag = "🔁" + retryCount + tag;
        }
        if (StringUtils.hasText(tag)) {
            title = isAsyncRequest(context) ? (isMock(context) ? "⚡️🎭 RESPONSE(" + tag + ") 🎭⚡️" : "⚡️RESPONSE(" + tag + ")⚡️") : (isMock(context) ? " 🎭 RESPONSE(" + tag + ") 🎭 " : " RESPONSE(" + tag + ")");
        } else {
            title = isAsyncRequest(context) ? (isMock(context) ? "⚡️🎭 RESPONSE 🎭⚡️" : "⚡️RESPONSE⚡️") : (isMock(context) ? " 🎭 RESPONSE 🎭 " : " RESPONSE ");
        }

        logBuilder.append("<<");
        logBuilder.append(INDENT_STR).append(FontUtil.getBackColorStr(color, title));
        logBuilder.append(INDENT_STR).append("🔍 ").append("[").append(FontUtil.getWhiteUnderline(getThreadName())).append("][").append(FontUtil.getWhiteUnderline(getUniqueId(context))).append("]");
        if (nameDesNotSame(context)) {
            logBuilder.append("[").append(FontUtil.getWhiteUnderline(getApiDesc(context))).append("]");
        }
        logBuilder.append(INDENT_STR).append("🛰️ ").append(FontUtil.getWhiteStr(getHttpExecutorStr(context)));
        logBuilder.append(INDENT_STR).append("🎯️ ").append(FontUtil.getWhiteStr(getMethodName(context)));
        logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, request.getRequestMethod().toString())).append(" ").append(FontUtil.getUnderlineColorString(color, request.getUrl()));

        String timeColor;
        String timeTag;

        if (isSlow(context)) {
            timeColor = COLOR_RED;
            timeTag = "⚠️";
        } else if (isWarn(context)) {
            timeColor = COLOR_YELLOW;
            timeTag = "🐌";
        } else {
            timeColor = color;
            timeTag = "";
        }

        logBuilder.append(LINE_BREAK).append(INDENT_STR)
                .append(context.getHttpExecutor().getHttpVersionString(request)).append(" ")
                .append(FontUtil.getColorStr(color, "" + status))
                .append(" (").append(timeTag).append(FontUtil.getColorStr(timeColor, UnitUtils.millisToTime(getExeTime(context)))).append(")");

        if (isPrintRespHeader(context)) {
            for (Map.Entry<String, List<Header>> entry : headerManager.getHeaderMap().entrySet()) {
                for (Header header : entry.getValue()) {
                    logBuilder.append(INDENT_STR).append(getStandardHeader(entry.getKey())).append(": ").append(header.getValue());
                }
            }
        }

        appendResponseBody(logBuilder, response, color, context);

        logBuilder.append(LINE_BREAK).append("<<");
        return logBuilder.toString();
    }

    private void appendResponseBody(StringBuilder logBuilder, Response response, String color, MethodContext context) throws Exception {
        long resultLength = response.getContentLength();
        long maxLength = getAllowPrintLogRespBodyMaxLength(context);
        logBuilder.append(LINE_BREAK);

        if (isAllowMimeType(context, response)) {
            if (response.isJsonBody()) {
                logBuilder.append(FontUtil.getColorStr(color, contextTruncation(jsonFormat(response.getStringResult()), maxLength)));
            } else if (response.isXmlBody()) {
                logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, contextTruncation(xmlFormat(response.getStringResult()).replace(LINE_BREAK, INDENT_STR), maxLength)));
            } else if (response.isJavaBody()) {
                logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, contextTruncation(javaBodyToString(response), maxLength)));
            } else if (response.isProtobufBody()) {
                try {
                    Type convertMetaType = context.getConvertMetaType();
                    if (convertMetaType == Object.class) {
                        convertMetaType = context.getMethodConvertReturnResolvableType().resolve();
                    }
                    logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, contextTruncation(String.valueOf((Object) ProtobufAutoConvert.convertProtobuf(response, convertMetaType)).replace(LINE_BREAK, INDENT_STR), maxLength)));
                } catch (Exception e) {
                    logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, contextTruncation(response.getStringResult().replace(LINE_BREAK, INDENT_STR), maxLength)));
                }
            } else {
                logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, contextTruncation(response.getStringResult().replace(LINE_BREAK, INDENT_STR), maxLength)));
            }
        } else {
            String msg;
            if (response.getContentType() == ContentType.NON) {
                msg = "Result of unknown type, size: " + resultLength;
            } else {
                msg = StringUtils.format("Is a '{}' result, size: {}", response.getContentType().getMimeType(), resultLength);
            }
            logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, msg));
        }
    }

    private void appendReqHeaders(StringBuilder logBuilder, HttpHeaderManager httpHeaderManager) {
        for (Map.Entry<String, List<Header>> entry : httpHeaderManager.getHeaderMap().entrySet()) {
            StringBuilder headerValueBuilder = new StringBuilder();
            List<Header> headerList = filterHeader(entry.getValue());
            for (Header header : headerList) {
                headerValueBuilder.append(header.getValue()).append("; ");
            }
            logBuilder.append(INDENT_STR).append(Console.getRedString(getStandardHeader(entry.getKey()) + ": ")).append(headerValueBuilder.toString().endsWith("; ") ? headerValueBuilder.substring(0, headerValueBuilder.length() - 2) : headerValueBuilder.toString());
        }
    }

    private String getStandardHeader(String name) {
        String s = "-";
        List<String> strings = Stream.of(name.split(s)).map(StringUtils::capitalize).collect(Collectors.toList());
        return StringUtils.join(strings, s);
    }

    private List<Header> filterHeader(List<Header> list) {
        return FormatUtils.filterHeader(list);
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
            return INDENT_STR + jsonStr;
        }
    }

    private String contextTruncation(String text, long maxLength) {
        if (maxLength < 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, (int) maxLength) + "\n\n\t⇡...(limit:" + maxLength + ")...⇡";
    }

    private String javaBodyToString(Response response) {
        try {
            return JDK_SCHEME.deserialization(response.getStringResult(), Object.class).toString();
        } catch (Exception e) {
            return response.getStringResult();
        }
    }

    private String getHttpExecutorStr(Context context) {
        return String.valueOf(context.getHttpExecutor());
    }

}
