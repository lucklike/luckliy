package com.luckyframework.exception;

import com.luckyframework.common.StringUtils;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/6/7 23:01
 */
public class FormatRuntimeException extends RuntimeException {

    public FormatRuntimeException() {
    }

    public FormatRuntimeException(String msgTemp, Object... params) {
        super(StringUtils.format(msgTemp, params));
    }

    public FormatRuntimeException(Throwable cause, String msgTemp, Object... params) {
        super(StringUtils.format(msgTemp, params), cause);
    }

    public FormatRuntimeException(Throwable cause) {
        super(cause);
    }

    public FormatRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
