package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    public String getReqCondition(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).reqCondition();
        }
        return reqCondition;
    }

    public String getRespCondition(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).respCondition();
        }
        return respCondition;
    }

    public boolean isPrintRespHeader(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).printRespHeader();
        }
        return printRespHeader;
    }

    public Set<String> getAllowPrintLogBodyMimeTypes(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            return new HashSet<>(Arrays.asList(context.getMergedAnnotationCheckParent(PrintLog.class).allowMimeTypes()));
        }
        return allowPrintLogBodyMimeTypes;
    }

    public long getAllowPrintLogRespBodyMaxLength(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).allowRespBodyMaxLength();
        }
        return allowPrintLogRespBodyMaxLength;
    }

    public long getAllowPrintLogReqBodyMaxLength(MethodContext context) {
        if (hasPrintLogAnnotation(context)) {
            return context.getMergedAnnotationCheckParent(PrintLog.class).allowReqBodyMaxLength();
        }
        return allowPrintLogReqBodyMaxLength;
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

    protected abstract void doRecordRequestLog(MethodContext context, Request request) throws Exception;


    protected abstract void doRecordMetaResponseLog(MethodContext context, Response response) throws Exception;

}
