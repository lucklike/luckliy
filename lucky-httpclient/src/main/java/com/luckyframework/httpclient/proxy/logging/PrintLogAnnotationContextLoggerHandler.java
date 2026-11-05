package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.Color;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.convert.ProtobufAutoConvert;
import com.luckyframework.httpclient.core.meta.ContentType;
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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$RESPONSE_TIME_SPENT$_;

/**
 * 基于{@link PrintLog @PrintLog}注解实现的日志处理器
 */
public abstract class PrintLogAnnotationContextLoggerHandler implements LoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(PrintLogAnnotationContextLoggerHandler.class);

    private final Set<String> allowPrintLogBodyMimeTypes = new HashSet<>();
    private final Map<CustomMasker, Set<String>> commonMaskers = new HashMap<>();
    private final Map<AnnotatedElement, Map<String, CustomMasker>> maskerCacheMap = new ConcurrentHashMap<>();
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
        return resolveStringValue(findPrintLogAnnotation(context), "reqCondition", PrintLog::reqCondition, reqCondition);
    }

    public String getRespCondition(MethodContext context) {
        return resolveStringValue(findPrintLogAnnotation(context), "respCondition", PrintLog::respCondition, respCondition);
    }

    public boolean enableRequestMask(MethodContext context) {
        PrintLog ann = findPrintLogAnnotation(context);
        String expression = resolveStringValue(ann, "maskRequest", PrintLog::maskRequest, enableRequestMask);
        return StringUtils.hasText(expression) ? context.parseExpression(expression, boolean.class) : isMaskerConfigured(context, ann);
    }

    public boolean enableResponseMask(MethodContext context) {
        PrintLog ann = findPrintLogAnnotation(context);
        String expression = resolveStringValue(ann, "maskResponse", PrintLog::maskResponse, enableResponseMask);
        return StringUtils.hasText(expression) ? context.parseExpression(expression, boolean.class) : isMaskerConfigured(context, ann);
    }

    /**
     * 是否配置了脱敏器（全局公共配置、{@code @PrintLog.maskers} 或独立标注的 {@code @Masker} 任一存在）
     * <p>
     * 用于在未显式配置脱敏开关时隐式启用脱敏：只要配置了脱敏器就默认启用，
     * 避免“配置了脱敏器却因忘记打开开关而不生效”的问题
     */
    protected boolean isMaskerConfigured(MethodContext context, PrintLog ann) {
        if (!commonMaskers.isEmpty()) {
            return true;
        }
        if (ann != null && ann.maskers().length > 0) {
            return true;
        }
        return !getStandaloneMaskers(context).isEmpty();
    }

    /**
     * 获取独立标注的 {@code @Masker} 配置（含组合注解中的、可重复标注展开后的、沿父级查找的）
     */
    protected List<Masker> getStandaloneMaskers(MethodContext context) {
        return context.findNestCombinationAnnotationsCheckParent(Masker.class);
    }


    public boolean isPrintRespHeader(MethodContext context) {
        String expression = resolveStringValue(findPrintLogAnnotation(context), "printRespHeader", PrintLog::printRespHeader, printRespHeader);
        return !StringUtils.hasText(expression) || context.parseExpression(expression, boolean.class);
    }

    public Set<String> getAllowPrintLogBodyMimeTypes(MethodContext context) {
        String[] mimeTypes = getCustomizedValue(findPrintLogAnnotation(context), "allowMimeTypes", PrintLog::allowMimeTypes);
        return mimeTypes == null ? allowPrintLogBodyMimeTypes : new HashSet<>(Arrays.asList(mimeTypes));
    }

    public long getAllowPrintLogRespBodyMaxLength(MethodContext context) {
        return resolveLongValue(findPrintLogAnnotation(context), "allowRespBodyMaxLength", PrintLog::allowRespBodyMaxLength, allowPrintLogRespBodyMaxLength);
    }

    public long getAllowPrintLogReqBodyMaxLength(MethodContext context) {
        return resolveLongValue(findPrintLogAnnotation(context), "allowReqBodyMaxLength", PrintLog::allowReqBodyMaxLength, allowPrintLogReqBodyMaxLength);
    }

    public ResponseTimeSpent getSlowResponseInfo(MethodContext context) {
        return context.getRootVar(_$RESPONSE_TIME_SPENT$_, ResponseTimeSpent.class);
    }


    private boolean hasPrintLogAnnotation(MethodContext context) {
        return context.isAnnotatedCheckParent(PrintLog.class);
    }

    /**
     * 获取参与配置覆盖判断的{@link PrintLog}注解实例
     *
     * @param context 方法上下文
     * @return 标注了{@code @PrintLog}（含父级）时返回注解实例，否则返回null
     */
    private PrintLog findPrintLogAnnotation(MethodContext context) {
        return hasPrintLogAnnotation(context) ? context.getMergedAnnotationCheckParent(PrintLog.class) : null;
    }

    /**
     * 读取注解上被显式修改的配置值
     *
     * @param ann            {@link PrintLog}注解实例，未标注时传null
     * @param attributeName  注解上的属性名
     * @param valueGetter    从注解中读取属性值
     * @param <T>            注解属性的类型
     * @return 属性被显式修改时返回注解上的值，方法未标注注解或该属性保持默认值时返回null
     */
    private <T> T getCustomizedValue(PrintLog ann, String attributeName, Function<PrintLog, T> valueGetter) {
        if (ann == null) {
            return null;
        }
        T annotationValue = valueGetter.apply(ann);
        Object defaultValue = AnnotationUtils.getDefaultValue(ann, attributeName);
        return Objects.equals(defaultValue, annotationValue) ? null : annotationValue;
    }

    /**
     * 按「注解显式配置优先于全局配置」的规则解析字符串类型的配置值
     *
     * @param ann          {@link PrintLog}注解实例，未标注时传null
     * @param attributeName 注解上的属性名
     * @param valueGetter  从注解中读取属性值
     * @param globalValue  兜底的全局配置
     * @return 最终生效的配置值
     */
    private String resolveStringValue(PrintLog ann, String attributeName, Function<PrintLog, String> valueGetter, String globalValue) {
        String customizedValue = getCustomizedValue(ann, attributeName, valueGetter);
        return customizedValue == null ? globalValue : customizedValue;
    }

    /**
     * 按「注解显式配置优先于全局配置」的规则解析长整型的配置值
     *
     * @param ann          {@link PrintLog}注解实例，未标注时传null
     * @param attributeName 注解上的属性名
     * @param valueGetter  从注解中读取属性值
     * @param globalValue  兜底的全局配置
     * @return 最终生效的配置值
     */
    private long resolveLongValue(PrintLog ann, String attributeName, Function<PrintLog, Long> valueGetter, long globalValue) {
        Long customizedValue = getCustomizedValue(ann, attributeName, valueGetter);
        return customizedValue == null ? globalValue : customizedValue;
    }


    @Override
    public void recordRequestLog(MethodContext context, Request request) {
        try {
            if (!prohibition(context) && isPrintRequestLog(context)) {
                doRecordRequestLog(context, request);
            }
        } catch (Exception e) {
            logPrintException("request", e);
        }
    }

    @Override
    public void recordMetaResponseLog(MethodContext context, Response response) {
        try {
            if (!prohibition(context) && isPrintResponseLog(context)) {
                doRecordMetaResponseLog(context, response);
            }
        } catch (Exception e) {
            logPrintException("response", e);
        }
    }

    /**
     * 打印日志过程中抛出的异常不应影响接口的正常响应，仅在开启详细错误日志时附带异常堆栈
     *
     * @param logPhase 日志阶段（request[请求日志]/response[响应日志]）
     * @param e        打印过程中抛出的异常
     */
    private void logPrintException(String logPhase, Exception e) {
        String message = "An exception occurred while printing the " + logPhase + " log. However, this exception does not affect the normal response of the interface.";
        if (isLogErrorWithDetails()) {
            logger.error(message, e);
        } else {
            logger.error(message);
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
                return Color.RED.getColorCore();
            case 4:
                return Color.MULBERRY.getColorCore();
            case 3:
                return Color.YELLOW.getColorCore();
            case 2:
                return Color.GREEN.getColorCore();
            default:
                return Color.WHITE.getColorCore();
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

    /**
     * 将Protobuf响应体转换为可读字符串，转换失败时降级为原始响应体文本
     *
     * @param context  方法上下文
     * @param response 响应实例
     * @return Protobuf响应体的可读文本
     */
    protected String getProtobufBodyAsString(MethodContext context, Response response) {
        try {
            Type convertMetaType = context.getConvertMetaType().getMetaType();
            if (convertMetaType == Object.class) {
                convertMetaType = context.getMethodConvertReturnResolvableType().toClass();
            }
            return String.valueOf((Object) ProtobufAutoConvert.convertProtobuf(response, convertMetaType));
        } catch (Exception e) {
            return getLogResponseBody(context, response);
        }
    }

    /**
     * 响应体不属于允许打印的MimeType时，仅描述响应的类型与大小
     *
     * @param response 响应实例
     * @return 响应体描述文本
     */
    protected String getDiscardedBodyDescription(Response response) {
        Long contentLength = response.getContentLength();
        String sizeInfo = contentLength == null ? "" : ", Size: " + contentLength;
        if (response.getContentType() == ContentType.NON) {
            return StringUtils.format("Result of unknown type{}", sizeInfo);
        }
        return StringUtils.format("Is a '{}' result{}", response.getContentType().getMimeType(), sizeInfo);
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

    /**
     * 对单个字段值执行脱敏（供日志处理器在 multipart 等非标准文本场景中使用）
     *
     * @param context   方法上下文
     * @param fieldName 字段名
     * @param value     原始值
     * @param request   true[请求数据]/false[响应数据]
     * @return 脱敏后的值
     */
    protected String tryMaskFieldValue(MethodContext context, String fieldName, String value, boolean request) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        boolean enableMask = request ? enableRequestMask(context) : enableResponseMask(context);
        if (!enableMask) {
            return value;
        }
        PrintLog ann = context.getSameAnnotationCombined(PrintLog.class);
        return DataMasker.maskFieldValue(maskerToMap(context, ann), fieldName, value);
    }

    /**
     * 对请求/响应头的值按头名做整体脱敏：命中头名脱敏器时脱敏整个值，未命中时原样返回。
     * <p>与{@link #tryMaskFieldValue}的区别在于不做裸值兜底，未配置的头名交由整体文本工序处理其内部嵌套结构。</p>
     *
     * @param context    方法上下文
     * @param headerName 头名称
     * @param value      原始值
     * @param request    true[请求头]/false[响应头]
     * @return 脱敏后的值
     */
    protected String tryMaskHeaderByName(MethodContext context, String headerName, String value, boolean request) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        boolean enableMask = request ? enableRequestMask(context) : enableResponseMask(context);
        if (!enableMask) {
            return value;
        }
        PrintLog ann = context.getSameAnnotationCombined(PrintLog.class);
        return DataMasker.maskByNameOnly(maskerToMap(context, ann), headerName, value);
    }

    private Map<String, CustomMasker> maskerToMap(MethodContext context, PrintLog ann) {
        AnnotatedElement element = context.getCurrentAnnotatedElement();
        Map<String, CustomMasker> maskerMap = maskerCacheMap.get(element);
        if (maskerMap == null) {
            Map<String, CustomMasker> builtMap = buildMaskerMap(context, ann);
            // 并发场景下可能出现两个线程同时构建，此时以先放入缓存的结果为准
            Map<String, CustomMasker> existsMap = maskerCacheMap.putIfAbsent(element, builtMap);
            maskerMap = existsMap == null ? builtMap : existsMap;
        }
        return maskerMap;
    }

    private Map<String, CustomMasker> buildMaskerMap(MethodContext context, PrintLog ann) {
        Map<String, CustomMasker> maskerMap = new HashMap<>();

        // 1.添加公共的脱敏配置
        for (Map.Entry<CustomMasker, Set<String>> entry : this.commonMaskers.entrySet()) {
            CustomMasker key = entry.getKey();
            Set<String> value = entry.getValue();
            if (ContainerUtils.isNotEmptyCollection(value)) {
                value.forEach(v -> maskerMap.put(v, key));
            }
        }

        // 2.添加 @PrintLog 注解上的脱敏配置
        if (ann != null) {
            for (Masker masker : ann.maskers()) {
                putMasker(context, maskerMap, masker);
            }
        }

        // 3.添加独立标注的 @Masker 配置（优先级最高）
        for (Masker masker : getStandaloneMaskers(context)) {
            putMasker(context, maskerMap, masker);
        }

        return maskerMap;
    }

    private void putMasker(MethodContext context, Map<String, CustomMasker> maskerMap, Masker masker) {
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

    protected abstract void doRecordRequestLog(MethodContext context, Request request) throws Exception;


    protected abstract void doRecordMetaResponseLog(MethodContext context, Response response) throws Exception;

}
