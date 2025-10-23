package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.luckyframework.common.FontUtil.COLOR_CYAN;
import static com.luckyframework.common.FontUtil.COLOR_GREEN;
import static com.luckyframework.common.FontUtil.COLOR_MULBERRY;
import static com.luckyframework.common.FontUtil.COLOR_RED;
import static com.luckyframework.common.FontUtil.COLOR_YELLOW;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_API_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_UNIQUE_ID_$;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$HTTP_EXE_TIME_$;

/**
 * 基于{@link PrintLog @PrintLog}注解实现的日志处理器
 */
public abstract class PrintLogAnnotationContextLoggerHandler implements LoggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(PrintLogAnnotationContextLoggerHandler.class);

    private final Set<String> allowPrintLogBodyMimeTypes = new HashSet<>();
    private long allowPrintLogRespBodyMaxLength = -1L;
    private long allowPrintLogReqBodyMaxLength = -1L;
    private String respCondition;
    private String reqCondition;
    private boolean printRespHeader = true;
    private long warnTime = -1L;
    private long slowTime = -1L;

    {
        // json
        allowPrintLogBodyMimeTypes.add("application/json");
        allowPrintLogBodyMimeTypes.add("application/*+json");

        // xml
        allowPrintLogBodyMimeTypes.add("application/xml");
        allowPrintLogBodyMimeTypes.add("application/*+xml");
        allowPrintLogBodyMimeTypes.add("text/xml");

        // protobuf
        allowPrintLogBodyMimeTypes.add("application/x-protobuf");

        // java
        allowPrintLogBodyMimeTypes.add("application/x-java-serialized-object");

        // text
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

    public void setWarnTime(long warnTime) {
        this.warnTime = warnTime;
    }

    public void setSlowTime(long slowTime) {
        this.slowTime = slowTime;
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
                logger.error("An exception occurred while printing the response log.", e);
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
        return context.getClassContext().getCurrentAnnotatedElement().getSimpleName() + "." + context.getCurrentAnnotatedElement().getName();
    }

    protected String getApiDesc(MethodContext context) {
        return context.getRootVar(StringUtils.format("{}.name", $_API_$), String.class);
    }

    protected boolean nameDesNotSame(MethodContext context) {
        return !Objects.equals(getApiName(context), getApiDesc(context));
    }

    protected String getThreadName() {
        return Thread.currentThread().getName();
    }

    protected String getUniqueId(MethodContext context) {
        return context.getRootVar($_UNIQUE_ID_$, String.class);
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

    protected long getExeTime(MethodContext context) {
        return context.getRootVar(_$HTTP_EXE_TIME_$, long.class);
    }

    protected boolean isSlow(MethodContext context) {
        long slowTime = getSlowTime(context);
        if (slowTime < 0) {
            return false;
        }

        return getExeTime(context) > slowTime;
    }

    protected boolean isWarn(MethodContext context) {
        long warnTime = getWarnTime(context);
        if (warnTime < 0) {
            return false;
        }

        return getExeTime(context) > warnTime;
    }

    protected String getBaseUrl(Request request) {
        String url = request.getUrl();
        int i = url.indexOf("?");
        if (i != -1) {
            return url.substring(0, i);
        }
        return url;
    }

    protected abstract void doRecordRequestLog(MethodContext context, Request request) throws Exception;


    protected abstract void doRecordMetaResponseLog(MethodContext context, Response response) throws Exception;

}
