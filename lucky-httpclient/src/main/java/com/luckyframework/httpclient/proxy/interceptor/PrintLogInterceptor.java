package com.luckyframework.httpclient.proxy.interceptor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.luckyframework.common.Console;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.Table;
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
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.setter.QueryParameterSetter;
import com.luckyframework.web.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    {
        allowPrintLogBodyMimeTypes.add("application/json");
        allowPrintLogBodyMimeTypes.add("application/xml");
        allowPrintLogBodyMimeTypes.add("text/xml");
        allowPrintLogBodyMimeTypes.add("text/plain");
        allowPrintLogBodyMimeTypes.add("text/html");
    }

    public void setAllowPrintLogBodyMimeTypes(Set<String> mimeTypes) {
        allowPrintLogBodyMimeTypes.clear();
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
    public void beforeExecute(Request request, InterceptorContext context) {
        boolean printLog;
        String reqCondition = getReqCondition(context);
        if (!StringUtils.hasText(reqCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(reqCondition, boolean.class, arg -> arg.extractRequest(request));
        }
        if (printLog) {
            log.info(getRequestLogInfo(request, context.getContext()));
        }
        initStartTime();
    }

    @Override
    public VoidResponse afterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        initEndTime();
        boolean printLog;
        String respCondition = getRespCondition(context);
        if (!StringUtils.hasText(respCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(respCondition, boolean.class, arg -> arg.extractVoidResponse(voidResponse).extractRequest(voidResponse.getRequest()));
        }
        if (printLog) {
            log.info(getResponseLogInfo(voidResponse.getStatus(), voidResponse.getProtocol(), voidResponse.getRequest(), voidResponse.getHeaderManager(), null, context));
        }

        return voidResponse;
    }

    @Override
    public Response afterExecute(Response response, InterceptorContext context) {
        initEndTime();
        boolean printLog;
        String respCondition = getRespCondition(context);
        if (!StringUtils.hasText(respCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(respCondition, boolean.class, arg -> arg.extractResponse(response).extractRequest(response.getRequest()));
        }
        if (printLog) {
            log.info(getResponseLogInfo(response.getStatus(), response.getProtocol(), response.getRequest(), response.getHeaderManager(), response, context));
        }
        return response;
    }

    private String getRequestLogInfo(Request request, MethodContext context) {
        StringBuilder logBuilder = new StringBuilder("\n>>");
        logBuilder.append("\n\t").append(getColorString("34", "  REQUEST  "));
        logBuilder.append("\n\t").append(Console.getWhiteString("Executor & Method"));
        logBuilder.append("\n\t").append(context.getHttpProxyFactory().getHttpExecutor().getClass().getName());
        logBuilder.append("\n\t").append(context.getCurrentAnnotatedElement().toString());

        // @StaticParam
        Set<Annotation> classAnnSet = context.getClassContext().getContainCombinationAnnotationsIgnoreSource(StaticParam.class);
        Set<Annotation> methodAnnSet = context.getContainCombinationAnnotationsIgnoreSource(StaticParam.class);
        if (ContainerUtils.isNotEmptyCollection(classAnnSet) || ContainerUtils.isNotEmptyCollection(methodAnnSet)) {
            logBuilder.append("\n\t").append(Console.getWhiteString("@StaticParam"));
            for (Annotation ann : classAnnSet) {
                logBuilder.append("\n\t").append("[class ] ").append(ann.toString());
            }
            for (Annotation ann : methodAnnSet) {
                logBuilder.append("\n\t").append("[method] ").append(ann.toString());
            }
        }


        // @InterceptorRegister
        List<InterceptorPerformer> performerList = context.getHttpProxyFactory().getInterceptorPerformerList(context);
        Set<Annotation> interClassAnn = context.getClassContext().getContainCombinationAnnotationsIgnoreSource(InterceptorRegister.class);
        Set<Annotation> interMethodAnn = context.getContainCombinationAnnotationsIgnoreSource(InterceptorRegister.class);
        if (ContainerUtils.isNotEmptyCollection(interClassAnn) || ContainerUtils.isNotEmptyCollection(interMethodAnn) || ContainerUtils.isNotEmptyCollection(performerList)) {
            logBuilder.append("\n\t").append(Console.getWhiteString("Interceptors"));

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
                InterceptorRegister interAnn = context.toAnnotation(ann, InterceptorRegister.class);
                sortEntryList.add(new SortEntry(interAnn.priority(), "[class ] (" + interAnn.priority() + ")" + ann.toString()));
            }
            for (Annotation ann : interMethodAnn) {
                InterceptorRegister interAnn = context.toAnnotation(ann, InterceptorRegister.class);
                sortEntryList.add(new SortEntry(interAnn.priority(), "[method ] (" + interAnn.priority() + ")" + ann.toString()));
            }
            sortEntryList.stream().sorted(Comparator.comparing(SortEntry::getPriority)).forEach(s -> logBuilder.append("\n\t").append(s.getString()));
        }

        // @ResultConvert
        Set<Annotation> convertClassAnnSet = context.getClassContext().getContainCombinationAnnotationsIgnoreSource(ResultConvert.class);
        Set<Annotation> convertMethodAnnSet = context.getContainCombinationAnnotationsIgnoreSource(ResultConvert.class);
        if (ContainerUtils.isNotEmptyCollection(convertClassAnnSet) || ContainerUtils.isNotEmptyCollection(convertMethodAnnSet)) {
            logBuilder.append("\n\t").append(Console.getWhiteString("@ResultConvert"));
            if (ContainerUtils.isNotEmptyCollection(convertMethodAnnSet)) {
                for (Annotation ann : convertMethodAnnSet) {
                    logBuilder.append("\n\t").append("[method] ").append(ann.toString());
                }
            } else {
                for (Annotation ann : convertClassAnnSet) {
                    logBuilder.append("\n\t").append("[class ] ").append(ann.toString());
                }
            }
        }

        // Timeout
        logBuilder.append("\n\t").append(Console.getWhiteString("Timeout"));
        logBuilder.append("\n\t")
                .append("connect-timeout=").append(request.getConnectTimeout() == null ? "not set" : request.getConnectTimeout())
                .append(", read-timeout=").append(request.getReadTimeout() == null ? "not set" : request.getReadTimeout())
                .append(", writer-timeout=").append(request.getWriterTimeout() == null ? "not set" : request.getWriterTimeout());


        // Args
        if (context.getParameterContexts().isEmpty()) {
            logBuilder.append("\n");
        } else {
            logBuilder.append("\n\t").append(Console.getWhiteString("Args\n"));
            Table table = new Table();
            table.styleThree();
            table.addHeader("index", "arg-name", "req-name", "value", "setter", "resolver");
            for (ParameterContext parameterContext : context.getParameterContexts()) {
                DynamicParam byAnn = parameterContext.getSameAnnotationCombined(DynamicParam.class);
                table.addDataRow(
                        parameterContext.getIndex(),
                        parameterContext.getName(),
                        !parameterContext.notHttpParam() ? ((byAnn != null && StringUtils.hasText(byAnn.name())) ? byAnn.name() : parameterContext.getName()) : "-",
                        "(" + parameterContext.getType().getRawClass().getSimpleName() + ")" + StringUtils.toString(parameterContext.getValue()),
                        !parameterContext.notHttpParam() ? (byAnn != null ? byAnn.setter().clazz().getSimpleName() : "QueryParameterSetter") : "-",
                        !parameterContext.notHttpParam() ? (byAnn != null ? byAnn.resolver().clazz().getSimpleName() : "LookUpSpecialAnnotationDynamicParamResolver") : "-"
                );
            }
            logBuilder.append(table.formatAndRightShift(1));
        }

        logBuilder.append("\n\t").append(Console.getMulberryString(request.getRequestMethod() + " ")).append(request.getUrl());
        if (request.getProxy() != null) {
            logBuilder.append("\n\t").append(Console.getRedString("proxy: ")).append(request.getProxy());
        }
        appendHeaders(logBuilder, request.getHeaderManager());

        BodyObject body = request.getBody();
        if (body != null) {
            logBuilder.append("\n");
            if (body.getContentType().getMimeType().equalsIgnoreCase("application/json")) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonElement je = JsonParser.parseString(body.getBodyAsString());
                String json = gson.toJson(je);
                String first = json.substring(0, 1);
                String last = json.substring(json.length() - 1);
                logBuilder.append("\n\t").append(Console.getCyanString(first + json.substring(1, json.length() - 1).replace("\n ", "\n\t") + "\t" + last));
            } else if (body.getContentType().getMimeType().equalsIgnoreCase("application/x-www-form-urlencoded")) {
                logBuilder.append("\n\t").append(Console.getCyanString((body.getBodyAsString().replaceAll("&", "&\n\t"))));
            } else {
                logBuilder.append("\n\t").append(Console.getCyanString(body.getBodyAsString()));
            }

        } else if (HttpExecutor.isFileRequest(request.getRequestParameters())) {
            logBuilder.append("\n\t").append(Console.getRedString("content-type: ")).append("multipart/form-data; boundary=LuckyBoundary\n");
            for (Map.Entry<String, Object> entry : request.getRequestParameters().entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                if (HttpExecutor.isResourceParam(value)) {
                    HttpFile[] httpFiles = HttpExecutor.toHttpFiles(value);
                    for (HttpFile httpFile : httpFiles) {
                        String descriptor = httpFile.getDescriptor();
                        logBuilder.append("\n\t").append(Console.getYellowString("--LuckyBoundary"));
                        logBuilder.append("\n\t").append(Console.getRedString("content-disposition: ")).append("form-data; name=\"").append(name).append("\"");
                        String mimeType = ContentTypeUtils.getMimeTypeOrDefault(descriptor.endsWith("]") ? descriptor.substring(0, descriptor.length() - 1) : descriptor, "text/plain");
                        logBuilder.append("\n\t").append(Console.getRedString("content-type: ")).append(mimeType);

                        logBuilder.append("\n\n\t").append(Console.getBlueString("< " + descriptor));
                    }
                } else {
                    logBuilder.append("\n\t").append(Console.getRedString("content-disposition:")).append(" form-data; name=\"").append(name).append("\"");
                    logBuilder.append("\n\t").append(Console.getRedString("content-type:")).append(" text/plain");
                    logBuilder.append("\n\n\t").append(Console.getCyanString(value));
                }
            }
            logBuilder.append("\n\t").append(Console.getYellowString("--LuckyBoundary--"));

        } else if (!ContainerUtils.isEmptyMap(request.getRequestParameters())) {
            logBuilder.append("\n\t").append(Console.getRedString("content-type: ")).append("application/x-www-form-urlencoded");
            logBuilder.append("\n");
            StringBuilder reqBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : request.getRequestParameters().entrySet()) {
                reqBuilder.append("\n\t").append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            logBuilder.append(Console.getCyanString(reqBuilder.toString().endsWith("&") ? reqBuilder.substring(0, reqBuilder.length() - 1) : reqBuilder.toString()));
        }
        logBuilder.append("\n>>");
        return logBuilder.toString();
    }

    private String getResponseLogInfo(int status, String protocol, Request request, HttpHeaderManager responseHeader, Response response, InterceptorContext context) {
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

        logBuilder.append("<<");
        logBuilder.append("\n\t").append(getColorString(color, "  RESPONSE  "));

        logBuilder.append("\n\t").append(request.getRequestMethod()).append(" ").append(request.getUrl());
        logBuilder.append("\n\n\t").append(protocol).append(" ").append(getColorString(color, "" + status, false)).append(" (").append(endTime - startTime).append("ms)");
        for (Map.Entry<String, List<Header>> entry : responseHeader.getHeaderMap().entrySet()) {
            StringBuilder headerValueBuilder = new StringBuilder();
            for (Header header : entry.getValue()) {
                headerValueBuilder.append(header.getValue()).append("; ");
            }
            logBuilder.append("\n\t").append(entry.getKey()).append(": ").append(headerValueBuilder.toString().endsWith("; ") ? headerValueBuilder.substring(0, headerValueBuilder.length() - 2) : headerValueBuilder.toString());
        }
        if (response != null) {
            appendResponseBody(logBuilder, response, color, context);
        } else {
            logBuilder.append("\n\n\t").append(getColorString(color, "The void response method does not support displaying the request body.", false));
        }

        logBuilder.append("\n<<");
        return logBuilder.toString();
    }

    private void appendResponseBody(StringBuilder logBuilder, Response response, String color, InterceptorContext context) {
        String mimeType = response.getContentType().getMimeType();
        int resultLength = response.getResult().length;
        Set<String> allowPrintLogBodyMimeTypes = getAllowPrintLogBodyMimeTypes(context);
        long allowPrintLogBodyMaxLength = getAllowPrintLogBodyMaxLength(context);
        boolean isAllowMimeType = allowPrintLogBodyMimeTypes.contains("*/*") || allowPrintLogBodyMimeTypes.contains(mimeType.toLowerCase());
        boolean isAllowSize = allowPrintLogBodyMaxLength <= 0 || resultLength <= allowPrintLogBodyMaxLength;
        logBuilder.append("\n");
        if (isAllowMimeType && isAllowSize) {
            if (mimeType.equalsIgnoreCase("application/json")) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonElement je = JsonParser.parseString(response.getStringResult());
                String json = gson.toJson(je);
                String first = json.substring(0, 1);
                String last = json.substring(json.length() - 1);
                logBuilder.append("\n\t").append(getColorString(color, first + json.substring(1, json.length() - 1).replace("\n ", "\n\t") + "\t" + last, false));
            } else {
                logBuilder.append("\n\t").append(getColorString(color, response.getStringResult(), false));
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

    private void appendHeaders(StringBuilder logBuilder, HttpHeaderManager httpHeaderManager) {
        for (Map.Entry<String, List<Header>> entry : httpHeaderManager.getHeaderMap().entrySet()) {
            StringBuilder headerValueBuilder = new StringBuilder();
            for (Header header : entry.getValue()) {
                headerValueBuilder.append(header.getValue()).append("; ");
            }
            logBuilder.append("\n\t").append(Console.getRedString(entry.getKey() + ": ")).append(headerValueBuilder.toString().endsWith("; ") ? headerValueBuilder.substring(0, headerValueBuilder.length() - 2) : headerValueBuilder.toString());
        }
    }

    private String getColorString(String colorCore, String text) {
        return getColorString(colorCore, text, true);
    }

    private String getColorString(String colorCore, String text, boolean isReversal) {
        String reversalCore = isReversal ? "7" : "1";
        return "\033[" + reversalCore + ";" + colorCore + "m" + text + "\033[0m";
    }
}
