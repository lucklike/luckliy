package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.common.Console;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.Table;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.ContentType;
import com.luckyframework.httpclient.core.Header;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.serializable.GsonSerializationScheme;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.common.Console.getWhiteString;
import static com.luckyframework.httpclient.core.SerializationConstant.JDK_SCHEME;

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
    private long startTime;
    private long endTime;

    private boolean printAnnotationInfo = false;
    private boolean printArgsInfo = false;


    public boolean isPrintAnnotationInfo(InterceptorContext context) {
        if (context.notNullAnnotated()) {
            setPrintAnnotationInfo(context.toAnnotation(PrintLog.class).printAnnotationInfo());
        }
        return printAnnotationInfo;
    }

    public void setPrintAnnotationInfo(boolean printAnnotationInfo) {
        this.printAnnotationInfo = printAnnotationInfo;
    }

    public boolean isPrintArgsInfo(InterceptorContext context) {
        if (context.notNullAnnotated()) {
            setPrintArgsInfo(context.toAnnotation(PrintLog.class).printArgsInfo());
        }
        return printArgsInfo;
    }

    public void setPrintArgsInfo(boolean printArgsInfo) {
        this.printArgsInfo = printArgsInfo;
    }

    {
        allowPrintLogBodyMimeTypes.add("application/json");
        allowPrintLogBodyMimeTypes.add("application/xml");
        allowPrintLogBodyMimeTypes.add("application/x-java-serialized-object");
        allowPrintLogBodyMimeTypes.add("text/xml");
        allowPrintLogBodyMimeTypes.add("text/plain");
        allowPrintLogBodyMimeTypes.add("text/html");
    }

    public void setAllowPrintLogBodyMimeTypes(Set<String> mimeTypes) {
        allowPrintLogBodyMimeTypes.clear();
        addAllowPrintLogBodyMimeTypes(mimeTypes);
    }

    public void addAllowPrintLogBodyMimeTypes(Set<String> mimeTypes){
        for (String mimeType : mimeTypes) {
            allowPrintLogBodyMimeTypes.add(mimeType.toLowerCase());
        }
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

    public Set<String> getAllowPrintLogBodyMimeTypes(InterceptorContext context) {
        if (context.notNullAnnotated()) {
            setAllowPrintLogBodyMimeTypes(new HashSet<>(Arrays.asList(context.toAnnotation(PrintLog.class).allowMimeTypes())));
        }
        return allowPrintLogBodyMimeTypes;
    }

    public long getAllowPrintLogBodyMaxLength(InterceptorContext context) {

        if (context.notNullAnnotated()) {
            setAllowPrintLogBodyMaxLength(context.toAnnotation(PrintLog.class).allowBodyMaxLength());
        }

        return allowPrintLogBodyMaxLength;
    }

    public String getRespCondition(InterceptorContext context) {

        if (context.notNullAnnotated()) {
            setRespCondition(context.toAnnotation(PrintLog.class).respCondition());
        }

        return respCondition;
    }

    public String getReqCondition(InterceptorContext context) {

        if (context.notNullAnnotated()) {
            setReqCondition(context.toAnnotation(PrintLog.class).reqCondition());
        }

        return reqCondition;
    }

    public void initStartTime() {
        startTime = System.currentTimeMillis();
    }

    public void initEndTime() {
        endTime = System.currentTimeMillis();
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
                throw new LuckyRuntimeException("An exception occurred while printing the request log.", e).printException(log);
            }

        }
        initStartTime();
    }

    @Override
    public VoidResponse doAfterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        initEndTime();
        boolean printLog;
        String respCondition = getRespCondition(context);
        if (!StringUtils.hasText(respCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(respCondition, boolean.class);
        }
        if (printLog) {
            try {
                log.info(getResponseLogInfo(voidResponse.getStatus(), voidResponse.getRequest(), voidResponse.getHeaderManager(), null, context));
            } catch (Exception e) {
                throw new LogPrintException("An exception occurred while printing the response log.", e).printException(log);
            }

        }

        return voidResponse;
    }

    @Override
    public Response doAfterExecute(Response response, InterceptorContext context) {
        initEndTime();
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
                throw new LogPrintException("An exception occurred while printing the response log.", e).printException(log);
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
        logBuilder.append("\n\t").append(methodContext.getCurrentAnnotatedElement().toGenericString());

        boolean isPrintAnnotationInfo = isPrintAnnotationInfo(context);
        boolean isPrintArgsInfo = isPrintArgsInfo(context);

        if (isPrintAnnotationInfo) {

            // @SSLMeta
            HostnameVerifier hostnameVerifier = request.getHostnameVerifier();
            SSLSocketFactory sslSocketFactory = request.getSSLSocketFactory();
            if (hostnameVerifier != null || sslSocketFactory != null) {
                logBuilder.append("\n\t").append(getWhiteString("@SSLMeta"));
                if (hostnameVerifier != null) {
                    logBuilder.append("\n\t").append("[using ] ").append(hostnameVerifier.getClass().getName());
                }
                if (sslSocketFactory != null) {
                    logBuilder.append("\n\t").append("[using ] ").append(sslSocketFactory.getClass().getName());
                }
            }

            // @StaticParam
            appendAnnotationInfo(methodContext, StaticParam.class, "@StaticParam", logBuilder, true);

            // @InterceptorRegister
            List<InterceptorPerformer> performerList = methodContext.getHttpProxyFactory().getInterceptorPerformerList(methodContext);
            Set<Annotation> interClassAnn = methodContext.getClassContext().getContainCombinationAnnotations(InterceptorRegister.class);
            Set<Annotation> interMethodAnn = methodContext.getContainCombinationAnnotations(InterceptorRegister.class);
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
                    sortEntryList.add(new SortEntry(performer.getPriority(), "[using ] (" + performer.getPriority() + ")" + performer.getInterceptor().getClass().getName()));
                }
                for (Annotation ann : interClassAnn) {
                    InterceptorRegister interAnn = methodContext.toAnnotation(ann, InterceptorRegister.class);
                    sortEntryList.add(new SortEntry(interAnn.priority(), "[class ] (" + interAnn.priority() + ")" + ann.toString()));
                }
                for (Annotation ann : interMethodAnn) {
                    InterceptorRegister interAnn = methodContext.toAnnotation(ann, InterceptorRegister.class);
                    sortEntryList.add(new SortEntry(interAnn.priority(), "[method ] (" + interAnn.priority() + ")" + ann.toString()));
                }
                sortEntryList.stream().sorted(Comparator.comparing(SortEntry::getPriority)).forEach(s -> logBuilder.append("\n\t").append(s.getString()));
            }

            // @ResultConvert
            appendAnnotationInfo(methodContext, ResultConvert.class, "@ResultConvert", logBuilder, false);

            // @ExceptionHandleMeta
            appendAnnotationInfo(methodContext, ExceptionHandleMeta.class, "@ExceptionHandleMeta", logBuilder, false);

            // Timeout
            logBuilder.append("\n\t").append(getWhiteString("@Timeout"));
            logBuilder.append("\n\t")
                    .append("connect-timeout=").append(request.getConnectTimeout() == null ? Request.DEF_CONNECTION_TIME_OUT : request.getConnectTimeout())
                    .append(", read-timeout=").append(request.getReadTimeout() == null ? Request.DEF_READ_TIME_OUT : request.getReadTimeout())
                    .append(", writer-timeout=").append(request.getWriterTimeout() == null ? Request.DEF_WRITER_TIME_OUT : request.getWriterTimeout());

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
                    table.addDataRow(
                            parameterContext.getIndex(),
                            parameterContext.getName(),
                            !parameterContext.notHttpParam() ? ((byAnn != null && StringUtils.hasText(byAnn.name())) ? byAnn.name() : parameterContext.getName()) : "-",
                            "(" + parameterContext.getType().getRawClass().getSimpleName() + ")" + StringUtils.toString(parameterContext.doGetValue()),
                            "(" + (parameterContext.getValue() == null ? "null" : parameterContext.getValue().getClass().getSimpleName()) + ")" + StringUtils.toString(parameterContext.getValue()),
                            !parameterContext.notHttpParam() ? (byAnn != null ? byAnn.setter().clazz().getSimpleName() : "QueryParameterSetter") : "-",
                            !parameterContext.notHttpParam() ? (byAnn != null ? byAnn.resolver().clazz().getSimpleName() : "LookUpSpecialAnnotationDynamicParamResolver") : "-"
                    );
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
                if (body.getBodyAsString().length() == 1) {
                    logBuilder.append("\n\t").append(Console.getCyanString(body.getBodyAsString()));
                } else {
                    String json = GsonSerializationScheme.prettyPrinting(body.getBodyAsString());
                    String first = json.substring(0, 1);
                    String last = json.substring(json.length() - 1);
                    logBuilder.append("\n\t").append(Console.getCyanString(first + json.substring(1, json.length() - 1).replace("\n ", "\n\t") + "\t" + last));
                }
            } else if (body.getContentType().getMimeType().equalsIgnoreCase("application/xml") || body.getContentType().getMimeType().equalsIgnoreCase("text/xml")) {
                logBuilder.append("\n\t").append(Console.getCyanString(JaxbXmlSerializationScheme.prettyPrintByTransformer(body.getBodyAsString()).replace("\n", "\n\t")));
            } else if (body.getContentType().getMimeType().equalsIgnoreCase("application/x-www-form-urlencoded")) {
                logBuilder.append("\n\t").append(Console.getCyanString((body.getBodyAsString().replace("&", "&\n\t"))));
            } else if (body.getContentType().getMimeType().equalsIgnoreCase("application/x-java-serialized-object")) {
                logBuilder.append("\n\t").append(Console.getCyanString(String.valueOf(JDK_SCHEME.fromByte(body.getBody()))));
            } else if (body.getContentType().getMimeType().equalsIgnoreCase("application/octet-stream")) {
                String fileType = null;
                String mimeType = ContentTypeUtils.getMimeType(body.getBody());
                if (mimeType != null) {
                    fileType = ContentTypeUtils.getFileExtension(mimeType);
                }
                if (fileType == null) {
                    logBuilder.append("\n\t").append(Console.getCyanString("Binary data request body. Size: " + body.getBody().length));
                } else {
                    logBuilder.append("\n\t").append(Console.getCyanString("Binary data request body. [" + fileType.toUpperCase() + " (" + body.getBody().length + ")]"));
                }

            } else {
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
                        logBuilder.append("\n\t").append(Console.getRedString("Content-Disposition: ")).append("form-data; name=\"").append(name).append("\"");
                        String mimeType = ContentTypeUtils.getMimeTypeOrDefault(descriptor.endsWith("]") ? descriptor.substring(0, descriptor.length() - 1) : descriptor, "text/plain");
                        logBuilder.append("\n\t").append(Console.getRedString("Content-Type: ")).append(mimeType);

                        logBuilder.append("\n\n\t").append(Console.getBlueString("< " + descriptor));
                    }
                } else {
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
        Set<Annotation> classAnnSet = methodContext.getClassContext().getContainCombinationAnnotations(annotationType);
        Set<Annotation> methodAnnSet = methodContext.getContainCombinationAnnotations(annotationType);

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

        String title = isAsync(context) ? " ⚡ RESPONSE ⚡ " : "  RESPONSE  ";
        logBuilder.append("<<");
        logBuilder.append("\n\t").append(getColorString(color, title));

        logBuilder.append("\n\t").append(getColorString(color, request.getRequestMethod().toString(), false)).append(" ").append(getUnderlineColorString(color, request.getUrl()));

        if (pr != 2) {
            logBuilder.append("\n\t").append(getColorString(color, "API", false)).append(" ").append(getUnderlineColorString(color, context.getContext().getCurrentAnnotatedElement().toGenericString()));
        }

        logBuilder.append("\n\n\t").append(request.getURL().getProtocol().toUpperCase()).append(" ").append(getColorString(color, "" + status, false)).append(" (").append(endTime - startTime).append("ms)");
        for (Map.Entry<String, List<Header>> entry : responseHeader.getHeaderMap().entrySet()) {
            for (Header header : entry.getValue()) {
                logBuilder.append("\n\t").append(getStandardHeader(entry.getKey())).append(": ").append(header.getValue());
            }
        }
        if (response != null) {
            appendResponseBody(logBuilder, response, color, context);
        } else {
            logBuilder.append("\n\n\t").append(getColorString(color, "The void response method does not support displaying the request body.", false));
        }

        logBuilder.append("\n<<");
        return logBuilder.toString();
    }

    private void appendResponseBody(StringBuilder logBuilder, Response response, String color, InterceptorContext context) throws Exception {
        String mimeType = response.getContentType().getMimeType();
        int resultLength = response.getResult().length;
        Set<String> allowPrintLogBodyMimeTypes = getAllowPrintLogBodyMimeTypes(context);
        long allowPrintLogBodyMaxLength = getAllowPrintLogBodyMaxLength(context);
        boolean isAllowMimeType = allowPrintLogBodyMimeTypes.contains("*/*") || allowPrintLogBodyMimeTypes.contains(mimeType.toLowerCase());
        boolean isAllowSize = allowPrintLogBodyMaxLength <= 0 || resultLength <= allowPrintLogBodyMaxLength;
        logBuilder.append("\n");
        if (isAllowMimeType && isAllowSize) {
            if (mimeType.equalsIgnoreCase("application/json")) {
                if (response.getStringResult().length() == 1) {
                    logBuilder.append("\n\t").append(getColorString(color, response.getStringResult(), false));
                }else {
                    String json = GsonSerializationScheme.prettyPrinting(response.getStringResult());
                    String first = json.substring(0, 1);
                    String last = json.substring(json.length() - 1);
                    logBuilder.append("\n\t").append(getColorString(color, first + json.substring(1, json.length() - 1).replace("\n ", "\n\t") + "\t" + last, false));
                }

            } else if (mimeType.equalsIgnoreCase("application/xml") || mimeType.equalsIgnoreCase("text/xml")) {
                logBuilder.append("\n\t").append(getColorString(color, JaxbXmlSerializationScheme.prettyPrintByTransformer(response.getStringResult()).replace("\n", "\n\t"), false));
            } else if (mimeType.equalsIgnoreCase("application/x-java-serialized-object")) {
                logBuilder.append("\n\t").append(getColorString(color, String.valueOf(JDK_SCHEME.fromByte(response.getResult())), false));
            } else {
                logBuilder.append("\n\t").append(getColorString(color, response.getStringResult().replace("\n", "\n\t"), false));
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
}
