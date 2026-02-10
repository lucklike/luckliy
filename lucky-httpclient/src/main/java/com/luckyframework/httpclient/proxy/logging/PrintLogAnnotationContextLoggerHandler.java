package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.slow.ResponseTimeSpent;
import com.luckyframework.httpclient.proxy.slow.SlowResponseHandler;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.web.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeType;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.luckyframework.common.FontUtil.COLOR_CYAN;
import static com.luckyframework.common.FontUtil.COLOR_GREEN;
import static com.luckyframework.common.FontUtil.COLOR_MULBERRY;
import static com.luckyframework.common.FontUtil.COLOR_RED;
import static com.luckyframework.common.FontUtil.COLOR_YELLOW;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$RESPONSE_TIME_SPENT$_;

/**
 * 基于{@link PrintLog @PrintLog}注解实现的日志处理器
 */
public abstract class PrintLogAnnotationContextLoggerHandler implements LoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(PrintLogAnnotationContextLoggerHandler.class);

    private final Set<String> allowPrintLogBodyMimeTypes = new HashSet<>();
    private final Map<CustomMasker, Set<String>> commonMaskers = new HashMap<>();
    private final Map<Method, Map<String, CustomMasker>> maskerCacheMap = new HashMap<>();
    private long allowPrintLogRespBodyMaxLength = -1L;
    private long allowPrintLogReqBodyMaxLength = -1L;
    private String respCondition;
    private String reqCondition;
    private String enableRequestMask;
    private String enableResponseMask;
    private String printRespHeader;
    private boolean logErrorWithDetails = false;


    {
        // json
        allowPrintLogBodyMimeTypes.add("application/json");
        allowPrintLogBodyMimeTypes.add("application/x-ndjson");
        allowPrintLogBodyMimeTypes.add("application/*+json");

        // xml
        allowPrintLogBodyMimeTypes.add("application/xml");
        allowPrintLogBodyMimeTypes.add("application/*+xml");

        // protobuf
        allowPrintLogBodyMimeTypes.add("application/x-protobuf");

        // java
        allowPrintLogBodyMimeTypes.add("application/x-java-serialized-object");

        // urlencoded
        allowPrintLogBodyMimeTypes.add("application/x-www-form-urlencoded");

        // YAML
        allowPrintLogBodyMimeTypes.add("application/x-yaml");

        // 文本类型
        allowPrintLogBodyMimeTypes.add("text/plain");
        allowPrintLogBodyMimeTypes.add("text/html");
        allowPrintLogBodyMimeTypes.add("text/css");
        allowPrintLogBodyMimeTypes.add("text/javascript");
        allowPrintLogBodyMimeTypes.add("text/markdown");
        allowPrintLogBodyMimeTypes.add("text/csv");
        allowPrintLogBodyMimeTypes.add("text/xml");

        // JavaScript
        allowPrintLogBodyMimeTypes.add("application/javascript");
        allowPrintLogBodyMimeTypes.add("application/x-javascript");
    }

    public boolean isLogErrorWithDetails() {
        return logErrorWithDetails;
    }

    public void setLogErrorWithDetails(boolean logErrorWithDetails) {
        this.logErrorWithDetails = logErrorWithDetails;
    }

    public void setPrintRespHeader(String printRespHeader) {
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

    public void setEnableRequestMask(String enableRequestMask) {
        this.enableRequestMask = enableRequestMask;
    }

    public void setEnableResponseMask(String enableResponseMask) {
        this.enableResponseMask = enableResponseMask;
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

    public void addCommonMaskers(Map<CustomMasker, Set<String>> commonMaskers) {
        this.commonMaskers.putAll(commonMaskers);
    }

    public String getReqCondition(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "reqCondition");
            String _reqCondition = ann.reqCondition();
            return Objects.equals(defValue, _reqCondition) ? reqCondition : _reqCondition;
        }
        return reqCondition;
    }

    public String getRespCondition(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "respCondition");
            String _respCondition = ann.respCondition();
            return Objects.equals(defValue, _respCondition) ? respCondition : _respCondition;
        }
        return respCondition;
    }

    public boolean enableRequestMask(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "enableRequestMask");
            String _enableRequestMask = ann.maskRequest();
            String exp = Objects.equals(defValue, _enableRequestMask) ? enableRequestMask : _enableRequestMask;
            return StringUtils.hasText(exp) && context.parseExpression(exp, boolean.class);
        }
        return StringUtils.hasText(enableRequestMask) && context.parseExpression(enableRequestMask, boolean.class);
    }

    public boolean enableResponseMask(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "enableResponseMask");
            String _enableResponseMask = ann.maskResponse();
            String exp = Objects.equals(defValue, _enableResponseMask) ? enableResponseMask : _enableResponseMask;
            return StringUtils.hasText(exp) && context.parseExpression(exp, boolean.class);
        }
        return StringUtils.hasText(enableResponseMask) && context.parseExpression(enableResponseMask, boolean.class);
    }


    public boolean isPrintRespHeader(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "printRespHeader");
            String _printRespHeader = ann.printRespHeader();
            String finalPrintRespHeader = Objects.equals(defValue, _printRespHeader) ? printRespHeader : _printRespHeader;
            return !StringUtils.hasText(finalPrintRespHeader) || context.parseExpression(finalPrintRespHeader, boolean.class);
        }
        return !StringUtils.hasText(printRespHeader) || context.parseExpression(printRespHeader, boolean.class);
    }

    public Set<String> getAllowPrintLogBodyMimeTypes(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "allowMimeTypes");
            String[] _allowMimeTypes = ann.allowMimeTypes();
            return Objects.equals(defValue, _allowMimeTypes) ? allowPrintLogBodyMimeTypes : new HashSet<>(Arrays.asList(_allowMimeTypes));
        }
        return allowPrintLogBodyMimeTypes;
    }

    public long getAllowPrintLogRespBodyMaxLength(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "allowRespBodyMaxLength");
            long _allowRespBodyMaxLength = ann.allowRespBodyMaxLength();
            return Objects.equals(defValue, _allowRespBodyMaxLength) ? allowPrintLogRespBodyMaxLength : _allowRespBodyMaxLength;
        }
        return allowPrintLogRespBodyMaxLength;
    }

    public long getAllowPrintLogReqBodyMaxLength(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "allowReqBodyMaxLength");
            long _allowReqBodyMaxLength = ann.allowReqBodyMaxLength();
            return Objects.equals(defValue, _allowReqBodyMaxLength) ? allowPrintLogReqBodyMaxLength : _allowReqBodyMaxLength;
        }
        return allowPrintLogReqBodyMaxLength;
    }

    public ResponseTimeSpent getSlowResponseInfo(MethodContext context) {
        return context.getRootVar(_$RESPONSE_TIME_SPENT$_, ResponseTimeSpent.class);
    }


    private boolean hasPrintLogAnnotation(MethodContext context) {
        return context.isAnnotatedCheckParent(PrintLog.class);
    }


    @Override
    public void recordRequestLog(MethodContext context, Request request) {
        try {
            if (!prohibition(context) && isPrintRequestLog(context)) {
                doRecordRequestLog(context, request);
            }
        } catch (Exception e) {
            if (isLogErrorWithDetails()) {
                logger.error("An exception occurred while printing the request log. However, this exception does not affect the normal response of the interface.", e);
            } else {
                logger.error("An exception occurred while printing the request log. However, this exception does not affect the normal response of the interface.");
            }

        }
    }

    @Override
    public void recordMetaResponseLog(MethodContext context, Response response) {
        try {
            if (!prohibition(context) && isPrintResponseLog(context)) {
                doRecordMetaResponseLog(context, response);
            }
        } catch (Exception e) {
            if (isLogErrorWithDetails()) {
                logger.error("An exception occurred while printing the response log. However, this exception does not affect the normal response of the interface.", e);
            } else {
                logger.error("An exception occurred while printing the response log. However, this exception does not affect the normal response of the interface.");
            }
        }
    }


    private boolean isPrintRequestLog(MethodContext context) {
        String reqCondition = getReqCondition(context);
        return !StringUtils.hasText(reqCondition) || context.parseExpression(reqCondition, boolean.class);
    }

    private boolean isPrintResponseLog(MethodContext context) {
        String respCondition = getRespCondition(context);
        return !StringUtils.hasText(respCondition) || context.parseExpression(respCondition, boolean.class);
    }

    private boolean prohibition(MethodContext context) {
        return context.isAnnotatedCheckParent(PrintLogProhibition.class);
    }

    protected String getMethodName(MethodContext context) {
        return MethodUtils.getLocation(context.getCurrentAnnotatedElement());
    }

    protected String getApiName(MethodContext context) {
        return context.getMethodString();
    }

    protected String getApiDesc(MethodContext context) {
        return context.getApiDescribe().getName();
    }

    protected boolean nameDesNotSame(MethodContext context) {
        return !Objects.equals(getApiName(context), getApiDesc(context));
    }

    protected String getThreadName() {
        return Thread.currentThread().getName();
    }

    protected String getRespColor(int status) {
        int pr = status / 100;
        switch (pr) {
            case 5:
                return COLOR_RED;
            case 4:
                return COLOR_MULBERRY;
            case 3:
                return COLOR_YELLOW;
            case 2:
                return COLOR_GREEN;
            default:
                return COLOR_CYAN;
        }
    }

    protected boolean isSlow(MethodContext context, ResponseTimeSpent responseTimeSpent) {
        SlowResponseHandler slowResponseHandler = context.getSlowResponseHandler();
        return slowResponseHandler != null && slowResponseHandler.isSlowResponse(context, responseTimeSpent);
    }

    protected String getBaseUrl(Request request) {
        String url = request.getUrl();
        int i = url.indexOf("?");
        if (i != -1) {
            return url.substring(0, i);
        }
        return url;
    }

    protected boolean isAllowMimeType(MethodContext context, Response response) {
        Set<MimeType> allowPrintLogBodyMimeTypes = getAllowPrintLogBodyMimeTypes(context).stream().map(MimeType::valueOf).collect(Collectors.toSet());
        return ContentTypeUtils.isCompatibleWith(allowPrintLogBodyMimeTypes, response.getContentType().getMimeType());
    }


    protected String getLogRequestBody(MethodContext context, Request request) {
        if (!hasPrintLogAnnotation(context)) {
            return request.getBody().getBodyAsString();
        }

        PrintLog ann = context.getSameAnnotationCombined(PrintLog.class);
        String reqBodyExp = ann.reqBodyExp();
        if (!StringUtils.hasText(reqBodyExp)) {
            return request.getBody().getBodyAsString();
        }

        return context.parseExpression(reqBodyExp, String.class);
    }


    protected String getLogResponseBody(MethodContext context, Response response) {
        if (!hasPrintLogAnnotation(context)) {
            return response.getStringResult();
        }

        PrintLog ann = context.getSameAnnotationCombined(PrintLog.class);
        String respBodyExp = ann.respBodyExp();
        if (!StringUtils.hasText(respBodyExp)) {
            return response.getStringResult();
        }
        return context.parseExpression(respBodyExp, String.class);
    }

    protected String tryRequestDataMask(MethodContext context, String sourceData) {
        if (enableRequestMask(context)) {
            PrintLog ann = context.getSameAnnotationCombined(PrintLog.class);
            return DataMasker.maskSensitiveData(maskerToMap(context, ann), sourceData);
        }
        return sourceData;
    }

    protected String tryResponseDataMask(MethodContext context, String sourceData) {
        if (enableResponseMask(context)) {
            PrintLog ann = context.getSameAnnotationCombined(PrintLog.class);
            return DataMasker.maskSensitiveData(maskerToMap(context, ann), sourceData);
        }
        return sourceData;
    }

    private Map<String, CustomMasker> maskerToMap(MethodContext context, PrintLog ann) {
        if (!maskerCacheMap.containsKey(context.getCurrentAnnotatedElement())) {
            Map<String, CustomMasker> maskerMap = new HashMap<>();

            // 添加公共的脱敏配置
            for (Map.Entry<CustomMasker, Set<String>> entry : this.commonMaskers.entrySet()) {
                CustomMasker key = entry.getKey();
                Set<String> value = entry.getValue();
                if (ContainerUtils.isNotEmptyCollection(value)) {
                    value.forEach(v -> maskerMap.put(v, key));
                }
            }

            // 添加注解脱敏配置
            if (ann != null) {
                for (Masker masker : ann.maskers()) {
                    Class<? extends CustomMasker> maskerClass = masker.maskerHandler();
                    CustomMasker customMasker;
                    if (maskerClass != CustomMasker.class) {
                        customMasker = context.generateObject(maskerClass, Scope.SINGLETON);
                    } else {
                        customMasker = masker.type();
                    }
                    for (String key : masker.keys()) {
                        maskerMap.put(key, customMasker);
                    }
                }
            }

            maskerCacheMap.put(context.getCurrentAnnotatedElement(), maskerMap);
        }

        return maskerCacheMap.get(context.getCurrentAnnotatedElement());
    }

    protected abstract void doRecordRequestLog(MethodContext context, Request request) throws Exception;


    protected abstract void doRecordMetaResponseLog(MethodContext context, Response response) throws Exception;

}
