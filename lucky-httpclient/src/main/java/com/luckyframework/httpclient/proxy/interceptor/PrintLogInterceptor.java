package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.common.Console;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.Table;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import com.luckyframework.httpclient.proxy.annotations.InterceptorMeta;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.mock.MockMeta;
import com.luckyframework.serializable.JacksonSerializationScheme;
import com.luckyframework.serializable.JaxbXmlSerializationScheme;
import com.luckyframework.web.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.common.Console.getWhiteString;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_EXE_TIME_$;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$IS_MOCK$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$MOCK_RESPONSE_FACTORY$__;

/**
 * 打印请求日志的拦截器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/6 11:20
 */
public class PrintLogInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(PrintLogInterceptor.class);

    private final Set<String> allowPrintLogBodyMimeTypes = new HashSet<>();
    private long allowPrintLogBodyMaxLength = -1L;
    private String respCondition;

    private String reqCondition;

    private boolean printAnnotationInfo = false;
    private boolean printArgsInfo = false;
    private boolean forcePrintBody = false;
    private boolean printRespHeader = true;


    {
        allowPrintLogBodyMimeTypes.add("application/json");
        allowPrintLogBodyMimeTypes.add("application/xml");
        allowPrintLogBodyMimeTypes.add("application/x-java-serialized-object");
        allowPrintLogBodyMimeTypes.add("text/xml");
        allowPrintLogBodyMimeTypes.add("text/plain");
        allowPrintLogBodyMimeTypes.add("text/html");
    }

    public void setPrintArgsInfo(boolean printArgsInfo) {
        this.printArgsInfo = printArgsInfo;
    }

    public void setForcePrintBody(boolean forcePrintBody) {
        this.forcePrintBody = forcePrintBody;
    }

    public void setPrintRespHeader(boolean printRespHeader) {
        this.printRespHeader = printRespHeader;
    }

    public void setAllowPrintLogBodyMaxLength(long allowPrintLogBodyMaxLength) {
        this.allowPrintLogBodyMaxLength = allowPrintLogBodyMaxLength;
    }

    public void setRespCondition(String respCondition) {
        this.respCondition = respCondition;
    }

    public void setReqCondition(String reqCondition) {
        this.reqCondition = reqCondition;
    }

    public void setPrintAnnotationInfo(boolean printAnnotationInfo) {
        this.printAnnotationInfo = printAnnotationInfo;
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

    public boolean isPrintAnnotationInfo(InterceptorContext context) {
        if (hasPrintLogAnnotation(context)) {
            setPrintAnnotationInfo(context.getMergedAnnotationCheckParent(PrintLog.class).printAnnotationInfo());
        }
        return printAnnotationInfo;
    }

    public boolean isPrintArgsInfo(InterceptorContext context) {
        if (hasPrintLogAnnotation(context)) {
            setPrintArgsInfo(context.getMergedAnnotationCheckParent(PrintLog.class).printArgsInfo());
        }
        return printArgsInfo;
    }

    public boolean isForcePrintBody(InterceptorContext context) {
        if (hasPrintLogAnnotation(context)) {
            setForcePrintBody(context.getMergedAnnotationCheckParent(PrintLog.class).forcePrintBody());
        }
        return forcePrintBody;
    }

    public boolean isPrintRespHeader(InterceptorContext context) {
        if (hasPrintLogAnnotation(context)) {
            setPrintRespHeader(context.getMergedAnnotationCheckParent(PrintLog.class).printRespHeader());
        }
        return printRespHeader;
    }

    public Set<String> getAllowPrintLogBodyMimeTypes(InterceptorContext context) {
        if (hasPrintLogAnnotation(context)) {
            setAllowPrintLogBodyMimeTypes(new HashSet<>(Arrays.asList(context.getMergedAnnotationCheckParent(PrintLog.class).allowMimeTypes())));
        }
        return allowPrintLogBodyMimeTypes;
    }

    public long getAllowPrintLogBodyMaxLength(InterceptorContext context) {
        if (hasPrintLogAnnotation(context)) {
            setAllowPrintLogBodyMaxLength(context.getMergedAnnotationCheckParent(PrintLog.class).allowBodyMaxLength());
        }
        return allowPrintLogBodyMaxLength;
    }

    public String getRespCondition(InterceptorContext context) {
        if (hasPrintLogAnnotation(context)) {
            setRespCondition(context.getMergedAnnotationCheckParent(PrintLog.class).respCondition());
        }
        return respCondition;
    }

    public String getReqCondition(InterceptorContext context) {
        if (hasPrintLogAnnotation(context)) {
            setReqCondition(context.getMergedAnnotationCheckParent(PrintLog.class).reqCondition());
        }
        return reqCondition;
    }


    @Override
    public void doBeforeExecute(Request request, InterceptorContext context) {
        boolean printLog;
        String reqCondition = getReqCondition(context);
        if (!StringUtils.hasText(reqCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(reqCondition, boolean.class);
        }
        if (printLog) {
            try {
                log.info(getRequestLogInfo(request, context));
            } catch (Exception e) {
                log.error("An exception occurred while printing the request log.", e);
            }

        }
    }

    @Override
    public Response doAfterExecute(Response response, InterceptorContext context) {
        boolean printLog;
        String respCondition = getRespCondition(context);
        if (!StringUtils.hasText(respCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(respCondition, boolean.class);
        }
        if (printLog) {
            try {
                log.info(getResponseLogInfo(response.getStatus(), response.getRequest(), response.getHeaderManager(), response, context));
            } catch (Exception e) {
                log.error("An exception occurred while printing the response log.", e);
            }

        }
        return response;
    }

    @Override
    public Class<? extends Annotation> prohibition() {
        return PrintLogProhibition.class;
    }

    private String getRequestLogInfo(Request request, InterceptorContext context) throws Exception {
        MethodContext methodContext = context.getContext();
        StringBuilder logBuilder = new StringBuilder("\n>>");
        String title = isAsync(context) ? " ⚡ REQUEST ⚡ " : "  REQUEST  ";
        logBuilder.append("\n\t").append(getColorString("36", title));
        logBuilder.append("\n\t").append(getWhiteString("Executor & Method"));
        logBuilder.append("\n\t").append(methodContext.getHttpExecutor().getClass().getName());
        logBuilder.append("\n\t").append(methodContext.getCurrentAnnotatedElement().toString());

        boolean isPrintAnnotationInfo = isPrintAnnotationInfo(context);
        boolean isPrintArgsInfo = isPrintArgsInfo(context);

        if (isPrintAnnotationInfo) {

            // @SSLMeta
            HostnameVerifier hostnameVerifier = request.getHostnameVerifier();
            SSLSocketFactory sslSocketFactory = request.getSSLSocketFactory();
            if (hostnameVerifier != null || sslSocketFactory != null) {
                logBuilder.append("\n\t").append(getWhiteString("@SSLMeta"));
                if (hostnameVerifier != null) {
                    logBuilder.append("\n\t").append("[using ] ").append(hostnameVerifier);
                }
                if (sslSocketFactory != null) {
                    logBuilder.append("\n\t").append("[using ] ").append(sslSocketFactory);
                }
            }

            // @MockMeta
            appendAnnotationInfo(methodContext, MockMeta.class, "@MockMeta", logBuilder, false);

            // @StaticParam
            appendAnnotationInfo(methodContext, StaticParam.class, "@StaticParam", logBuilder, true);

            // @InterceptorMeta
            List<InterceptorPerformer> performerList = methodContext.getInterceptorPerformerList();
            List<InterceptorMeta> interClassAnn = methodContext.getClassContext().findNestCombinationAnnotations(InterceptorMeta.class);
            List<InterceptorMeta> interMethodAnn = methodContext.findNestCombinationAnnotations(InterceptorMeta.class);
            if (ContainerUtils.isNotEmptyCollection(interClassAnn) || ContainerUtils.isNotEmptyCollection(interMethodAnn) || ContainerUtils.isNotEmptyCollection(performerList)) {
                logBuilder.append("\n\t").append(getWhiteString("@Interceptor"));

                class SortEntry {
                    final int priority;
                    final String string;

                    public SortEntry(int priority, String string) {
                        this.priority = priority;
                        this.string = string;
                    }

                    public int getPriority() {
                        return priority;
                    }

                    public String getString() {
                        return string;
                    }


                }
                List<SortEntry> sortEntryList = new ArrayList<>();

                for (InterceptorPerformer performer : performerList) {
                    sortEntryList.add(new SortEntry(performer.getPriority(context.getContext()), "[using ] (" + performer.getPriority(context.getContext()) + ")" + performer.getInterceptor(context.getContext())));
                }
                for (Annotation ann : interClassAnn) {
                    InterceptorMeta interAnn = methodContext.toAnnotation(ann, InterceptorMeta.class);
                    sortEntryList.add(new SortEntry(interAnn.priority(), "[class ] (" + interAnn.priority() + ")" + ann.toString()));
                }
                for (Annotation ann : interMethodAnn) {
                    InterceptorMeta interAnn = methodContext.toAnnotation(ann, InterceptorMeta.class);
                    sortEntryList.add(new SortEntry(interAnn.priority(), "[method ] (" + interAnn.priority() + ")" + ann.toString()));
                }
                sortEntryList.stream().sorted(Comparator.comparing(SortEntry::getPriority)).forEach(s -> logBuilder.append("\n\t").append(s.getString()));
            }

            // @ResultConvert
            appendAnnotationInfo(methodContext, ResultConvertMeta.class, "@ResultConvert", logBuilder, true);

            // @ExceptionHandleMeta
            appendAnnotationInfo(methodContext, ExceptionHandleMeta.class, "@ExceptionHandleMeta", logBuilder, false);

            // Timeout
            logBuilder.append("\n\t").append(getWhiteString("@Timeout"));
            logBuilder.append("\n\t").append("connect-timeout=").append(UnitUtils.millisToTime(request.getConnectTimeout() == null ? Request.DEF_CONNECTION_TIME_OUT : request.getConnectTimeout())).append(", read-timeout=").append(UnitUtils.millisToTime(request.getReadTimeout() == null ? Request.DEF_READ_TIME_OUT : request.getReadTimeout())).append(", writer-timeout=").append(UnitUtils.millisToTime(request.getWriterTimeout() == null ? Request.DEF_WRITER_TIME_OUT : request.getWriterTimeout()));

        }

        if (isPrintArgsInfo) {
            // Args
            if (ContainerUtils.isEmptyArray(methodContext.getParameterContexts())) {
                logBuilder.append("\n");
            } else {
                logBuilder.append("\n\t").append(getWhiteString("Args\n"));
                Table table = new Table();
                table.styleThree();
                table.addHeader("index", "arg-name", "req-name", "value", "realValue", "setter", "resolver");

                for (ParameterContext parameterContext : methodContext.getParameterContexts()) {
                    DynamicParam byAnn = parameterContext.getSameAnnotationCombined(DynamicParam.class);
                    table.addDataRow(parameterContext.getIndex(), parameterContext.getName(), parameterContext.isExplicitHttpParam() ? ((byAnn != null && StringUtils.hasText(byAnn.name())) ? byAnn.name() : parameterContext.getName()) : "-", "(" + parameterContext.getType().getRawClass().getSimpleName() + ")" + StringUtils.toString(parameterContext.doGetValue()), "(" + (parameterContext.getValue() == null ? "null" : parameterContext.getValue().getClass().getSimpleName()) + ")" + StringUtils.toString(parameterContext.getValue()), parameterContext.isExplicitHttpParam() ? (byAnn != null ? byAnn.setter().clazz().getSimpleName() : "QueryParameterSetter") : "-", parameterContext.isExplicitHttpParam() ? (byAnn != null ? byAnn.resolver().clazz().getSimpleName() : "LookUpSpecialAnnotationDynamicParamResolver") : "-");
                }
                logBuilder.append(table.formatAndRightShift(1));
            }
        }

        if (!isPrintArgsInfo) {
            logBuilder.append("\n");
        }

        logBuilder.append("\n\t").append(Console.getMulberryString(request.getRequestMethod() + " ")).append(getUnderlineColorString("35", request.getUrl()));
        if (request.getProxyInfo() != null) {
            logBuilder.append("\n\t").append(Console.getRedString("Proxy: ")).append(request.getProxyInfo().getProxy());
        }
        appendReqHeaders(logBuilder, request.getHeaderManager());

        BodyObject body = request.getBody();
        if (body != null) {
            logBuilder.append("\n");
            if (body.getContentType().getMimeType().equalsIgnoreCase("application/json")) {
                logBuilder.append(Console.getCyanString(jsonFormat(body.getBodyAsString())));
            } else if (isXmlBody(body.getContentType().getMimeType())) {
                logBuilder.append("\n\t").append(Console.getCyanString(xmlFormat(body.getBodyAsString()).replace("\n", "\n\t")));
            } else if (body.getContentType().getMimeType().equalsIgnoreCase("application/x-www-form-urlencoded")) {
                logBuilder.append("\n\t").append(Console.getCyanString((body.getBodyAsString().replace("&", "&\n\t"))));
            }else {
                logBuilder.append("\n\t").append(Console.getCyanString(body.getBodyAsString().replace("\n", "\n\t")));
            }

        } else if (ContainerUtils.isNotEmptyMap(request.getMultipartFormParameters())) {
            logBuilder.append("\n\t").append(Console.getRedString("Content-Type: ")).append("multipart/form-data; boundary=LuckyBoundary\n");
            for (Map.Entry<String, Object> entry : request.getMultipartFormParameters().entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                if (HttpExecutor.isResourceParam(value)) {
                    HttpFile[] httpFiles = HttpExecutor.toHttpFiles(value);
                    for (HttpFile httpFile : httpFiles) {
                        String descriptor = httpFile.getDescriptor();
                        logBuilder.append("\n\t").append(Console.getYellowString("--LuckyBoundary"));
                        logBuilder.append("\n\t").append(Console.getRedString("Content-Disposition: ")).append("form-data; name=\"").append(name).append("\"").append("; filename=\"").append(httpFile.getFileName()).append("\"");
                        String mimeType = ContentTypeUtils.getMimeTypeOrDefault(descriptor.endsWith("]") ? descriptor.substring(0, descriptor.length() - 1) : descriptor, "text/plain");
                        logBuilder.append("\n\t").append(Console.getRedString("Content-Type: ")).append(mimeType);

                        logBuilder.append("\n\n\t").append(Console.getBlueString("< " + descriptor));
                    }
                } else {
                    logBuilder.append("\n\t").append(Console.getYellowString("--LuckyBoundary"));
                    logBuilder.append("\n\t").append(Console.getRedString("Content-Disposition:")).append(" form-data; name=\"").append(name).append("\"");
                    logBuilder.append("\n\t").append(Console.getRedString("Content-Type:")).append(" text/plain");
                    logBuilder.append("\n\n\t").append(Console.getCyanString(value));
                }
            }
            logBuilder.append("\n\t").append(Console.getYellowString("--LuckyBoundary--"));

        } else if (!ContainerUtils.isEmptyMap(request.getFormParameters())) {
            logBuilder.append("\n\t").append(Console.getRedString("Content-Type: ")).append("application/x-www-form-urlencoded");
            logBuilder.append("\n");
            StringBuilder reqBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : request.getFormParameters().entrySet()) {
                reqBuilder.append("\n\t").append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            logBuilder.append(Console.getCyanString(reqBuilder.toString().endsWith("&") ? reqBuilder.substring(0, reqBuilder.length() - 1) : reqBuilder.toString()));
        }
        logBuilder.append("\n>>");
        return logBuilder.toString();
    }

    private void appendAnnotationInfo(MethodContext methodContext, Class<? extends Annotation> annotationType, String title, StringBuilder logBuilder, boolean printAll) {
        List<? extends Annotation> classAnnSet = methodContext.getClassContext().findNestCombinationAnnotations(annotationType);
        List<? extends Annotation> methodAnnSet = methodContext.findNestCombinationAnnotations(annotationType);

        if (ContainerUtils.isNotEmptyCollection(classAnnSet) || ContainerUtils.isNotEmptyCollection(methodAnnSet)) {
            logBuilder.append("\n\t").append(getWhiteString(title));
            if (printAll) {

                for (Annotation ann : classAnnSet) {
                    logBuilder.append("\n\t").append("[class ] ").append(ann.toString());
                }
                for (Annotation ann : methodAnnSet) {
                    logBuilder.append("\n\t").append("[method] ").append(ann.toString());
                }
            } else {
                if (ContainerUtils.isNotEmptyCollection(methodAnnSet)) {
                    for (Annotation ann : methodAnnSet) {
                        logBuilder.append("\n\t").append("[method] ").append(ann.toString());
                    }
                } else {
                    for (Annotation ann : classAnnSet) {
                        logBuilder.append("\n\t").append("[class ] ").append(ann.toString());
                    }
                }
            }

        }
    }

    private String getResponseLogInfo(int status, Request request, HttpHeaderManager responseHeader, Response response, InterceptorContext context) throws Exception {
        StringBuilder logBuilder = new StringBuilder("\n");
        String color;
        int pr = status / 100;
        switch (pr) {
            case 5:
                color = "31";
                break;
            case 4:
                color = "35";
                break;
            case 3:
                color = "33";
                break;
            case 2:
                color = "32";
                break;
            default:
                color = "36";
        }

        String title = isAsync(context) ? (isMock(context.getContext()) ? " ⚡ MOCK-RESPONSE ⚡ " : " ⚡ RESPONSE ⚡ ") : (isMock(context.getContext()) ? "  MOCK-RESPONSE  " : "  RESPONSE  ");
        logBuilder.append("<<");
        logBuilder.append("\n\t").append(getColorString(color, title));

        logBuilder.append("\n\t").append(getColorString(color, request.getRequestMethod().toString(), false)).append(" ").append(getUnderlineColorString(color, request.getUrl()));

        if (pr != 2) {
            logBuilder.append("\n\t").append(getColorString(color, "API", false)).append(" ").append(getUnderlineColorString(color, context.getContext().getCurrentAnnotatedElement().toString()));
        }

        logBuilder.append("\n\n\t").append(request.getURL().getProtocol().toUpperCase()).append(" ").append(getColorString(color, "" + status, false)).append(" (").append(UnitUtils.millisToTime(context.getRootVar($_EXE_TIME_$, long.class))).append(")");

        if (isPrintRespHeader(context)) {
            for (Map.Entry<String, List<Header>> entry : responseHeader.getHeaderMap().entrySet()) {
                for (Header header : entry.getValue()) {
                    logBuilder.append("\n\t").append(getStandardHeader(entry.getKey())).append(": ").append(header.getValue());
                }
            }
        }

        MethodContext methodContext = context.getContext();
        if (methodContext.isVoidMethod()) {
            if (methodContext.canApplyResultHandler() || isForcePrintBody(context)) {
                appendResponseBody(logBuilder, response, color, context);
            } else {
                logBuilder.append("\n\n\t").append(getColorString(color, "Methods for printing response bodies are not supported.", false));
            }
        } else {
            appendResponseBody(logBuilder, response, color, context);
        }

        logBuilder.append("\n<<");
        return logBuilder.toString();
    }

    private void appendResponseBody(StringBuilder logBuilder, Response response, String color, InterceptorContext context) throws Exception {
        String mimeType = response.getContentType().getMimeType();
        long resultLength = response.getContentLength();
        Set<String> allowPrintLogBodyMimeTypes = getAllowPrintLogBodyMimeTypes(context);
        long maxLength = getAllowPrintLogBodyMaxLength(context);
        boolean isAllowMimeType = allowPrintLogBodyMimeTypes.contains("*/*") || allowPrintLogBodyMimeTypes.contains(mimeType.toLowerCase());
        logBuilder.append("\n");

        if (isAllowMimeType) {
            if (mimeType.equalsIgnoreCase("application/json")) {
                logBuilder.append(getColorString(color, contextTruncation(jsonFormat(response.getStringResult()), maxLength), false));
            } else if (isXmlBody(mimeType)) {
                logBuilder.append("\n\t").append(getColorString(color, contextTruncation(xmlFormat(response.getStringResult()).replace("\n", "\n\t"), maxLength), false));
            } else if (mimeType.equalsIgnoreCase("application/x-java-serialized-object")) {
                logBuilder.append("\n\t").append(getColorString(color, contextTruncation(javaBodyToString(response), maxLength), false));
            } else {
                logBuilder.append("\n\t").append(getColorString(color, contextTruncation(response.getStringResult().replace("\n", "\n\t"), maxLength), false));
            }
        } else {
            String msg;
            if (ContentType.NON.getMimeType().equals(mimeType)) {
                msg = "Result of unknown type, size: " + resultLength;
            } else {
                msg = StringUtils.format("Is a '{}' result, size: {}", mimeType, resultLength);
            }
            logBuilder.append("\n\t").append(getColorString(color, msg, false));
        }
    }

    private String contextTruncation(String text, long maxLength) {
        if (maxLength < 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, (int) maxLength) + "\n\n\t⇡......allow-print-max-length=" + maxLength + "......⇡";
    }

    private void appendReqHeaders(StringBuilder logBuilder, HttpHeaderManager httpHeaderManager) {
        for (Map.Entry<String, List<Header>> entry : httpHeaderManager.getHeaderMap().entrySet()) {
            StringBuilder headerValueBuilder = new StringBuilder();
            List<Header> headerList = filterHeader(entry.getValue());
            for (Header header : headerList) {
                headerValueBuilder.append(header.getValue()).append("; ");
            }
            logBuilder.append("\n\t").append(Console.getRedString(getStandardHeader(entry.getKey()) + ": ")).append(headerValueBuilder.toString().endsWith("; ") ? headerValueBuilder.substring(0, headerValueBuilder.length() - 2) : headerValueBuilder.toString());
        }
    }

    private List<Header> filterHeader(List<Header> list) {
        List<Header> resultHeaders = new ArrayList<>();
        for (Header header : list) {
            if (header.getHeaderType() == Header.HeaderType.SET) {
                resultHeaders.clear();
            }
            resultHeaders.add(header);
        }
        return resultHeaders;
    }

    private String getColorString(String colorCore, String text) {
        return getColorString(colorCore, text, true);
    }

    private String getColorString(String colorCore, String text, boolean isReversal) {
        String reversalCore = isReversal ? "7" : "1";
        return "\033[" + reversalCore + ";" + colorCore + "m" + text + "\033[0m";
    }

    private String getUnderlineColorString(String colorCore, String text) {
        return "\033[4;1;" + colorCore + "m" + text + "\033[0m";
    }

    private String getStandardHeader(String name) {
        String s = "-";
        List<String> strings = Stream.of(name.split(s)).map(StringUtils::capitalize).collect(Collectors.toList());
        return StringUtils.join(strings, s);
    }

    public boolean isAsync(InterceptorContext context) {
        return context.getContext().isAsyncMethod() || context.getContext().isFutureMethod();
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
            json = json.replace("\n", "\n\t");
            return "\n\t" + json;
        } catch (Exception e) {
            return "\n\t" + jsonStr;
        }
    }

    private boolean hasPrintLogAnnotation(InterceptorContext context) {
        return context.getAnnotation() != null && context.isAnnotatedCheckParent(PrintLog.class);
    }

    private boolean isMock(MethodContext methodContext) {
        if (methodContext.getVar(__$MOCK_RESPONSE_FACTORY$__) != null) {
            return true;
        }
        return Objects.equals(Boolean.TRUE, methodContext.getVar(__$IS_MOCK$__));
    }

    private boolean isXmlBody(String mimeType) {
        return mimeType.equalsIgnoreCase("application/xml") ||
                mimeType.equalsIgnoreCase("text/xml") ||
                mimeType.equalsIgnoreCase("text/html");
    }

    private String javaBodyToString(Response response) {
        try {
            return JDK_SCHEME.deserialization(response.getStringResult(), Object.class).toString();
        } catch (Exception e) {
            return response.getStringResult();
        }
    }
}
