package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;
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
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$HTTP_EXECUTE_TIME$_;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$REQUEST_END_TIME$;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$REQUEST_START_TIME$;

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
    private boolean printRespHeader = true;
    private long warnTime = -1L;
    private long slowTime = -1L;
    private String uniqueId = "#{$unique_id$}";
    private SlowResponseHandler slowResponseHandler;


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

    public void setWarnTime(long warnTime) {
        this.warnTime = warnTime;
    }

    public void setSlowTime(long slowTime) {
        this.slowTime = slowTime;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setSlowResponseHandler(SlowResponseHandler slowResponseHandler) {
        this.slowResponseHandler = slowResponseHandler;
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
            boolean _printRespHeader = ann.printRespHeader();
            return Objects.equals(defValue, _printRespHeader) ? printRespHeader : _printRespHeader;
        }
        return printRespHeader;
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

    public long getWarnTime(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "warnTime");
            long _warnedTime = ann.warnTime();
            return Objects.equals(defValue, _warnedTime) ? warnTime : _warnedTime;
        }
        return warnTime;
    }

    public long getSlowTime(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "slowTime");
            long _slowTime = ann.slowTime();
            return Objects.equals(defValue, _slowTime) ? slowTime : _slowTime;
        }
        return slowTime;
    }

    public SlowResponseInfo getSlowResponseInfo(MethodContext context, Response response) {
        long startTime = context.getRootVar(_$REQUEST_START_TIME$, long.class);
        long endTime = context.getRootVar(_$REQUEST_END_TIME$, long.class);
        long exeTime = context.getRootVar(_$HTTP_EXECUTE_TIME$_, long.class);
        return new SlowResponseInfo(getUniqueId(context), response, startTime, endTime, exeTime);
    }

    public SlowResponseHandler getSlowResponseHandler(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            ObjectGenerate objectGenerate = ann.slowHandler();
            if (objectGenerate.clazz() != SlowResponseHandler.class) {
                return context.generateObject(objectGenerate);
            }
            Class<? extends SlowResponseHandler> slowHandlerClass = ann.slowHandlerClass();
            if (slowHandlerClass != SlowResponseHandler.class) {
                return context.generateObject(slowHandlerClass, Scope.SINGLETON);
            }
        }
        return slowResponseHandler;
    }

    private boolean hasPrintLogAnnotation(MethodContext context) {
        return context.isAnnotatedCheckParent(PrintLog.class);
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
                doRecordRequestLog(context, request);
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
                doRecordMetaResponseLog(context, response);
            } catch (Exception e) {
                logger.error("An exception occurred while printing the response log. However, this exception does not affect the normal response of the interface.", e);
            }

        }
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

    protected String getUniqueId(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            PrintLog ann = context.getMergedAnnotationCheckParent(PrintLog.class);
            Object defValue = AnnotationUtils.getDefaultValue(ann, "uniqueId");
            String _uniqueId = ann.uniqueId();
            String finalUniqueId = Objects.equals(defValue, _uniqueId) ? uniqueId : _uniqueId;
            return context.parseExpression(finalUniqueId, String.class);
        }
        return context.parseExpression(uniqueId, String.class);
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

    protected boolean isSlow(MethodContext context, SlowResponseInfo slowResponseInfo) {
        long slowTime = getSlowTime(context);
        if (slowTime < 0) {
            return false;
        }

        boolean isSlow = slowResponseInfo.getExeTime() > slowTime;
        if (isSlow) {
            runSlowHandler(context, slowResponseInfo, slowTime, true);
        }
        return isSlow;
    }

    protected boolean isWarn(MethodContext context, SlowResponseInfo slowResponseInfo) {
        long warnTime = getWarnTime(context);
        if (warnTime < 0) {
            return false;
        }

        boolean isWarn = slowResponseInfo.getExeTime() > warnTime;
        if (isWarn) {
            long slowTime = getSlowTime(context);
            if (slowTime < 0 || slowTime > slowResponseInfo.getExeTime()) {
                runSlowHandler(context, slowResponseInfo, warnTime, false);
            }
        }
        return isWarn;
    }

    protected void runSlowHandler(MethodContext context, SlowResponseInfo slowResponseInfo, long configSlowTime, boolean isSlow) {
        SlowResponseHandler slowHandler = getSlowResponseHandler(context);
        if (slowHandler != null) {
            if (isSlow) {
                slowHandler.handleSlowResponse(context, slowResponseInfo, configSlowTime);
            } else {
                slowHandler.handleWarnResponse(context, slowResponseInfo, configSlowTime);
            }
        }
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
