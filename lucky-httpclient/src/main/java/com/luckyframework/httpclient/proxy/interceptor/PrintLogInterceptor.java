package com.luckyframework.httpclient.proxy.interceptor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.luckyframework.common.Console;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
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
import com.luckyframework.httpclient.proxy.SpELUtils;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.web.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Arrays;
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

    @Override
    public void beforeExecute(Request request, InterceptorContext context) {
        if (!context.isNullAnnotated()) {
            setReqCondition(context.toAnnotation(PrintLog.class).reqCondition());
        }
        boolean printLog;
        if (!StringUtils.hasText(reqCondition)) {
            printLog = true;

        } else {
            printLog = context.parseExpression(reqCondition, boolean.class, arg -> arg.extractRequest(request));
        }
        if (printLog) {
            log.info(getRequestLogInfo(request, context.getContext()));
        }
    }

    @Override
    public VoidResponse afterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        if (!context.isNullAnnotated()) {
            setRespCondition(context.toAnnotation(PrintLog.class).respCondition());
        }

        boolean printLog;
        if (!StringUtils.hasText(respCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(respCondition, boolean.class, arg -> arg.extractVoidResponse(voidResponse).extractRequest(voidResponse.getRequest()));
        }
        if (printLog) {
            log.info(getResponseLogInfo(voidResponse.getStatus(), voidResponse.getRequest(), voidResponse.getHeaderManager(), null));
        }
        return voidResponse;
    }

    @Override
    public Response afterExecute(Response response, InterceptorContext context) {
        if (!context.isNullAnnotated()) {
            PrintLog printLogAnn = context.toAnnotation(PrintLog.class);
            setAllowPrintLogBodyMaxLength(printLogAnn.allowBodyMaxLength());
            setAllowPrintLogBodyMimeTypes(new HashSet<>(Arrays.asList(printLogAnn.allowMimeTypes())));
            setRespCondition(printLogAnn.respCondition());
        }
        boolean printLog;
        if (!StringUtils.hasText(respCondition)) {
            printLog = true;
        } else {
            printLog = context.parseExpression(respCondition, boolean.class, arg -> arg.extractResponse(response).extractRequest(response.getRequest()));
        }
        if (printLog) {
            log.info(getResponseLogInfo(response.getStatus(), response.getRequest(), response.getHeaderManager(), response));
        }
        return response;
    }

    private String getRequestLogInfo(Request request, MethodContext context) {
        StringBuilder logBuilder = new StringBuilder("\n>>");
        logBuilder.append("\n\t").append(getColorString("34", "  REQUEST  "));
        logBuilder.append("\n\t").append(Console.getWhiteString("HttpExecutor: ")).append("\n\t\t").append(Console.getWhiteString(context.getHttpExecutor().getClass().getName()));
        logBuilder.append("\n\t").append(Console.getWhiteString("Method-Args: "));
        Set<Annotation> classAnnSet = context.getClassContext().getContainCombinationAnnotationsIgnoreSource(StaticParam.class);
        for (Annotation ann : classAnnSet) {
            logBuilder.append("\n\t\t").append(Console.getWhiteString("[C]" + ann.toString()));
        }
        Set<Annotation> methodAnnSet = context.getContainCombinationAnnotationsIgnoreSource(StaticParam.class);
        for (Annotation ann : methodAnnSet) {
            logBuilder.append("\n\t\t").append(Console.getWhiteString("[M]" + ann.toString()));
        }
        logBuilder.append("\n\t\t").append(Console.getWhiteString(context.getCurrentAnnotatedElement().toString()));
        if (context.getParameterContexts().isEmpty()) {
            logBuilder.append("\n\t").append(Console.getWhiteString("no parameter")).append("\n");
        } else {
            logBuilder.append("\n\t\t").append(Console.getWhiteString("["));
            for (ParameterContext parameterContext : context.getParameterContexts()) {
                DynamicParam dynamicParamAnn = parameterContext.getSameAnnotationCombined(DynamicParam.class);
                logBuilder
                        .append("\n\t\t ").append(Console.getWhiteString("{"))
                        .append("\n\t\t  ").append("index: ").append(Console.getWhiteString(parameterContext.getIndex()))
                        .append("\n\t\t  ").append("name: ").append(Console.getWhiteString(parameterContext.getName()))
                        .append("\n\t\t  ").append("type: ").append(Console.getWhiteString(parameterContext.getType().getRawClass().getName()));
                if (dynamicParamAnn != null) {
                    logBuilder.append("\n\t\t  ").append("dyAnn: ").append(Console.getWhiteString(dynamicParamAnn.toString()));
                }

                logBuilder.append("\n\t\t  ").append("value: ").append(Console.getWhiteString(StringUtils.toString(parameterContext.getValue())));
                logBuilder.append("\n\t\t ").append(Console.getWhiteString("}"));
                if (context.getParameterContexts().size() != parameterContext.getIndex() + 1) {
                    logBuilder.append(Console.getWhiteString(","));
                }
            }
            logBuilder.append("\n\t\t").append(Console.getWhiteString("]"));
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
                JsonElement je = JsonParser.parseString(body.getBody());
                String json = gson.toJson(je);
                String first = json.substring(0, 1);
                String last = json.substring(json.length() - 1);
                logBuilder.append("\n\t").append(Console.getCyanString(first + json.substring(1, json.length() - 1).replace("\n ", "\n\t") + "\t" + last));
            } else {
                logBuilder.append("\n\t").append(Console.getCyanString(body.getBody()));
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

    private String getResponseLogInfo(int status, Request request, HttpHeaderManager responseHeader, Response response) {
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
        logBuilder.append("\n\n\t").append("HTTP/1.1 ").append(getColorString(color, "" + status, false));
        for (Map.Entry<String, List<Header>> entry : responseHeader.getHeaderMap().entrySet()) {
            StringBuilder headerValueBuilder = new StringBuilder();
            for (Header header : entry.getValue()) {
                headerValueBuilder.append(header.getValue()).append("; ");
            }
            logBuilder.append("\n\t").append(entry.getKey()).append(": ").append(headerValueBuilder.toString().endsWith("; ") ? headerValueBuilder.substring(0, headerValueBuilder.length() - 2) : headerValueBuilder.toString());
        }
        if (response != null) {
            appendResponseBody(logBuilder, response, color);
        } else {
            logBuilder.append("\n\n\t").append(getColorString(color, "The void method does not support displaying the request body.", false));
        }

        logBuilder.append("\n<<");
        return logBuilder.toString();
    }

    private void appendResponseBody(StringBuilder logBuilder, Response response, String color) {
        String mimeType = response.getContentType().getMimeType();
        int resultLength = response.getResult().length;
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
