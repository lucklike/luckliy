package com.luckyframework.httpclient.proxy.impl.interceptor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.luckyframework.common.Console;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.Header;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.ParameterContext;
import com.luckyframework.httpclient.proxy.RequestInterceptor;
import com.luckyframework.httpclient.proxy.ResponseInterceptor;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.web.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
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
public class PrintLogInterceptor implements RequestInterceptor, ResponseInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PrintLogInterceptor.class);

    @Override
    public void requestProcess(Request request, MethodContext context, Annotation requestAfterHandleAnn) {
        log.info(getRequestLogInfo(request, context));
    }

    @Override
    public void responseProcess(Response response, MethodContext context, Annotation responseInterceptorHandleAnn) {
        log.info(getResponseLogInfo(response));
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
                if (context.getParameterContexts().size() != parameterContext.getIndex() + 1){
                    logBuilder.append(Console.getWhiteString(","));
                }
            }
            logBuilder.append("\n\t\t").append(Console.getWhiteString("]"));
        }

        logBuilder.append("\n\n\t").append(Console.getMulberryString(request.getRequestMethod() + " ")).append(request.getUrl());
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

    private String getResponseLogInfo(Response response) {
        StringBuilder logBuilder = new StringBuilder("\n");
        String color;
        int state = response.getState();
        int pr = state / 100;
        switch (pr) {
            case 5 : color = "31"; break;
            case 4 : color = "35"; break;
            case 3 : color = "33"; break;
            case 2 : color = "32"; break;
            default: color = "36";
        }

        logBuilder.append("<<");
        logBuilder.append("\n\t").append(getColorString(color, "  RESPONSE  "));

        logBuilder.append("\n\t").append(response.getRequest().getRequestMethod()).append(" ").append(response.getRequest().getUrl());
        logBuilder.append("\n\n\t").append("HTTP/1.1 ").append(getColorString(color, "" + state, false));
        for (Map.Entry<String, List<Header>> entry : response.getHeaderManager().getHeaderMap().entrySet()) {
            StringBuilder headerValueBuilder = new StringBuilder();
            for (Header header : entry.getValue()) {
                headerValueBuilder.append(header.getValue()).append("; ");
            }
            logBuilder.append("\n\t").append(entry.getKey()).append(": ").append(headerValueBuilder.toString().endsWith("; ") ? headerValueBuilder.substring(0, headerValueBuilder.length() - 2) : headerValueBuilder.toString());
        }
        logBuilder.append("\n");
        if (response.getContentType().getMimeType().equalsIgnoreCase("application/json")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement je = JsonParser.parseString(response.getStringResult());
            String json = gson.toJson(je);
            String first = json.substring(0, 1);
            String last = json.substring(json.length() - 1);
            logBuilder.append("\n\t").append(getColorString(color, first + json.substring(1, json.length() - 1).replace("\n ", "\n\t") + "\t" + last, false));
        } else {
            logBuilder.append("\n\t").append(getColorString(color, response.getStringResult(), false));
        }
        logBuilder.append("\n<<");
        return logBuilder.toString();
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
        return "\033["+reversalCore+";" + colorCore + "m" + text + "\033[0m";
    }
}