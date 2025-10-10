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
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.serializable.JacksonSerializationScheme;
import com.luckyframework.serializable.JaxbXmlSerializationScheme;
import com.luckyframework.web.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.common.FontUtil.COLOR_CYAN;
import static com.luckyframework.common.FontUtil.COLOR_GREEN;
import static com.luckyframework.common.FontUtil.COLOR_MULBERRY;
import static com.luckyframework.common.FontUtil.COLOR_RED;
import static com.luckyframework.common.FontUtil.COLOR_YELLOW;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_UNIQUE_ID_$;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$IS_MOCK$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$MOCK_RESPONSE_FACTORY$__;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName.$_HTTP_EXE_TIME_$;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName.$_RETRY_COUNT_$;

public class DefaultLoggerHandler implements LoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLoggerHandler.class);

    private final String INDENT_STR = "\n\t";
    public static final String LINE_BREAK = "\n";
    public static final String FORM_DELIMITER = "&";

    private final Set<String> allowPrintLogBodyMimeTypes = new HashSet<>();
    private long allowPrintLogRespBodyMaxLength = -1L;
    private long allowPrintLogReqBodyMaxLength = -1L;
    private String respCondition;

    private String reqCondition;

    private boolean printRespHeader = true;


    {
        allowPrintLogBodyMimeTypes.add("application/json");
        allowPrintLogBodyMimeTypes.add("application/*+json");

        allowPrintLogBodyMimeTypes.add("application/xml");
        allowPrintLogBodyMimeTypes.add("application/*+xml");
        allowPrintLogBodyMimeTypes.add("text/xml");

        allowPrintLogBodyMimeTypes.add("application/x-protobuf");
        allowPrintLogBodyMimeTypes.add("application/x-java-serialized-object");

        allowPrintLogBodyMimeTypes.add("text/plain");
        allowPrintLogBodyMimeTypes.add("text/html");

    }

    public void setPrintRespHeader(boolean printRespHeader) {
        this.printRespHeader = printRespHeader;
    }

    public void setAllowPrintLogRespBodyMaxLength(long allowPrintLogRespBodyMaxLength) {
        this.allowPrintLogRespBodyMaxLength = allowPrintLogRespBodyMaxLength;
    }

    public void setAllowPrintLogReqBodyMaxLength(long allowPrintLogReqBodyMaxLength) {
        this.allowPrintLogReqBodyMaxLength = allowPrintLogReqBodyMaxLength;
    }

    public void setRespCondition(String respCondition) {
        this.respCondition = respCondition;
    }

    public void setReqCondition(String reqCondition) {
        this.reqCondition = reqCondition;
    }

    public void setAllowPrintLogBodyMimeTypes(Set<String> mimeTypes) {
        allowPrintLogBodyMimeTypes.clear();
        addAllowPrintLogBodyMimeTypes(mimeTypes);
    }

    public void addAllowPrintLogBodyMimeTypes(Set<String> mimeTypes) {
        for (String mimeType : mimeTypes) {
            allowPrintLogBodyMimeTypes.add(mimeType.toLowerCase());
        }
    }


    @Override
    public void recordRequestLog(MethodContext context, Request request) {
        if (prohibition(context)) {
            return;
        }

        boolean printLog;
        String reqCondition = getReqCondition(context);
        if (!StringUtils.hasText(reqCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(reqCondition, boolean.class);
        }
        if (printLog) {
            try {
                logger.info(getRequestLogInfo(request, context));
            } catch (Exception e) {
                logger.error("An exception occurred while printing the request log.", e);
            }

        }
    }


    @Override
    public void recordMetaResponseLog(MethodContext context, Response response) {
        if (prohibition(context)) {
            return;
        }

        boolean printLog;
        String respCondition = getRespCondition(context);
        if (!StringUtils.hasText(respCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(respCondition, boolean.class);
        }
        if (printLog) {
            try {
                logger.info(getResponseLogInfo(response.getStatus(), response.getRequest(), response.getHeaderManager(), response, context));
            } catch (Exception e) {
                logger.error("An exception occurred while printing the response log.", e);
            }

        }
    }


    @Override
    public void recordFinalResponseLog(MethodContext context, Response response) {
        // not print
    }

    private boolean prohibition(MethodContext context) {
        return context.isAnnotatedCheckParent(PrintLogProhibition.class);
    }


    private String getRequestLogInfo(Request request, MethodContext context) {
        StringBuilder logBuilder = new StringBuilder("\n>>");
        String TITLE_SYNC = " REQUEST ";
        String TITLE_ASYNC = "⚡️REQUEST⚡️";
        String title = isAsyncRequest(context) ? TITLE_ASYNC : TITLE_SYNC;

        logBuilder.append(INDENT_STR).append(FontUtil.getBackCyanStr(title));
        logBuilder.append(INDENT_STR).append("🔍 ").append(FontUtil.getWhiteStr("[" + Thread.currentThread().getName() + "] ")).append(FontUtil.getWhiteUnderline(context.getRootVar($_UNIQUE_ID_$, String.class)));
        logBuilder.append(INDENT_STR).append("〰️ ").append(context.getHttpExecutor().getClass().getName());
        logBuilder.append(INDENT_STR).append("➰ ").append(context.getCurrentAnnotatedElement().toString());

        logBuilder.append(LINE_BREAK)
                .append(INDENT_STR)
                .append(FontUtil.getMulberryStr(request.getRequestMethod().toString()))
                .append(" ")
                .append(FontUtil.getBlueUnderline(request.getUrl()))
                .append(" ")
                .append(FontUtil.getMulberryStr(context.getHttpExecutor().getHttpVersionString(request)));
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
            for (Map.Entry<String, Object> entry : request.getMultipartFormParameters().entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                if (HttpExecutor.isResourceParam(value)) {
                    HttpFile[] httpFiles = HttpExecutor.toHttpFiles(value);
                    for (HttpFile httpFile : httpFiles) {
                        String descriptor = httpFile.getDescriptor();
                        logBuilder.append(INDENT_STR).append(Console.getYellowString("--LuckyBoundary"));
                        logBuilder.append(INDENT_STR).append(Console.getRedString("Content-Disposition: ")).append("form-data; name=\"").append(name).append("\"").append("; filename=\"").append(httpFile.getFileName()).append("\"");
                        String mimeType = ContentTypeUtils.getMimeTypeOrDefault(descriptor.endsWith("]") ? descriptor.substring(0, descriptor.length() - 1) : descriptor, "text/plain");
                        logBuilder.append(INDENT_STR).append(Console.getRedString("Content-Type: ")).append(mimeType);

                        logBuilder.append(LINE_BREAK).append(INDENT_STR).append(Console.getBlueString("< " + descriptor));
                    }
                } else {
                    logBuilder.append(INDENT_STR).append(Console.getYellowString("--LuckyBoundary"));
                    logBuilder.append(INDENT_STR).append(Console.getRedString("Content-Disposition:")).append(" form-data; name=\"").append(name).append("\"");
                    logBuilder.append(INDENT_STR).append(Console.getRedString("Content-Type:")).append(" text/plain");
                    logBuilder.append(LINE_BREAK).append(INDENT_STR).append(Console.getCyanString(value));
                }
            }
            logBuilder.append(INDENT_STR).append(Console.getYellowString("--LuckyBoundary--"));

        } else if (!ContainerUtils.isEmptyMap(request.getFormParameters())) {
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
        String color;
        int pr = status / 100;
        switch (pr) {
            case 5:
                color = COLOR_RED;
                break;
            case 4:
                color = COLOR_MULBERRY;
                break;
            case 3:
                color = COLOR_YELLOW;
                break;
            case 2:
                color = COLOR_GREEN;
                break;
            default:
                color = COLOR_CYAN;
        }

        Integer retryCount = context.getRootVar($_RETRY_COUNT_$, Integer.class);

        String title;
        if (retryCount != null) {
            title = isAsyncRequest(context) ? (isMock(context) ? "⚡️MOCK-RESPONSE(🔁 " + retryCount + ") ⚡️" : "⚡️RESPONSE(🔁" + retryCount + ")⚡️") : (isMock(context) ? " MOCK-RESPONSE(🔁 " + retryCount + ")" : " RESPONSE(🔁 " + retryCount + ")");
        } else {
            title = isAsyncRequest(context) ? (isMock(context) ? "⚡️MOCK-RESPONSE⚡️" : "⚡️RESPONSE⚡️") : (isMock(context) ? " MOCK-RESPONSE " : " RESPONSE ");
        }

        logBuilder.append("<<");
        logBuilder.append(INDENT_STR).append(FontUtil.getBackColorStr(color, title));
        logBuilder.append(INDENT_STR).append("🔍 ").append(FontUtil.getWhiteStr("[" + Thread.currentThread().getName() + "] ")).append(FontUtil.getWhiteUnderline(context.getRootVar($_UNIQUE_ID_$, String.class)));
        logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, request.getRequestMethod().toString())).append(" ").append(FontUtil.getUnderlineColorString(color, request.getUrl()));

        if (pr != 2) {
            logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, "API")).append(" ").append(FontUtil.getUnderlineColorString(color, context.getCurrentAnnotatedElement().toString()));
        }

        logBuilder.append(LINE_BREAK).append(INDENT_STR).append(context.getHttpExecutor().getHttpVersionString(request)).append(" ").append(FontUtil.getColorStr(color, "" + status)).append(" (").append(UnitUtils.millisToTime(context.getRootVar($_HTTP_EXE_TIME_$, long.class))).append(")");

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
        Set<MimeType> allowPrintLogBodyMimeTypes = getAllowPrintLogBodyMimeTypes(context).stream().map(MimeType::valueOf).collect(Collectors.toSet());
        long maxLength = getAllowPrintLogRespBodyMaxLength(context);
        boolean isAllowMimeType = ContentTypeUtils.isCompatibleWith(allowPrintLogBodyMimeTypes, response.getContentType().getMimeType());
        logBuilder.append(LINE_BREAK);

        if (isAllowMimeType) {
            if (response.isJsonBody()) {
                logBuilder.append(FontUtil.getColorStr(color, contextTruncation(jsonFormat(response.getStringResult()), maxLength)));
            } else if (response.isXmlBody()) {
                logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, contextTruncation(xmlFormat(response.getStringResult()).replace(LINE_BREAK, INDENT_STR), maxLength)));
            } else if (response.isJavaBody()) {
                logBuilder.append(INDENT_STR).append(FontUtil.getColorStr(color, contextTruncation(javaBodyToString(response), maxLength)));
            } else if (response.isProtobufBody()) {
                try {
                    Class<?> convertMetaType = context.getConvertMetaType();
                    if (convertMetaType == Object.class) {
                        convertMetaType = context.getRealMethodReturnResolvableType().resolve();
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

    private String getReqCondition(MethodContext context) {
        if (context.isAnnotatedCheckParent(PrintLog.class)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).reqCondition();
        }
        return reqCondition;
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


    public String getRespCondition(MethodContext context) {
        if (context.isAnnotatedCheckParent(PrintLog.class)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).respCondition();
        }
        return respCondition;
    }

    public boolean isPrintRespHeader(MethodContext context) {
        if (context.isAnnotatedCheckParent(PrintLog.class)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).printRespHeader();
        }
        return printRespHeader;
    }

    public Set<String> getAllowPrintLogBodyMimeTypes(MethodContext context) {
        if (context.isAnnotatedCheckParent(PrintLog.class)) {
            return new HashSet<>(Arrays.asList(context.getMergedAnnotationCheckParent(PrintLog.class).allowMimeTypes()));
        }
        return allowPrintLogBodyMimeTypes;
    }

    public long getAllowPrintLogRespBodyMaxLength(MethodContext context) {
        if (context.isAnnotatedCheckParent(PrintLog.class)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).allowRespBodyMaxLength();
        }
        return allowPrintLogRespBodyMaxLength;
    }

    public long getAllowPrintLogReqBodyMaxLength(MethodContext context) {
        if (context.isAnnotatedCheckParent(PrintLog.class)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).allowReqBodyMaxLength();
        }
        return allowPrintLogReqBodyMaxLength;
    }


    private String contextTruncation(String text, long maxLength) {
        if (maxLength < 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, (int) maxLength) + "\n\n\t⇡......allow-print-max-length=" + maxLength + "......⇡";
    }

    private String javaBodyToString(Response response) {
        try {
            return JDK_SCHEME.deserialization(response.getStringResult(), Object.class).toString();
        } catch (Exception e) {
            return response.getStringResult();
        }
    }

    private boolean isMock(MethodContext methodContext) {
        if (methodContext.getVar(__$MOCK_RESPONSE_FACTORY$__) != null) {
            return true;
        }
        return Objects.equals(Boolean.TRUE, methodContext.getVar(__$IS_MOCK$__));
    }

}
