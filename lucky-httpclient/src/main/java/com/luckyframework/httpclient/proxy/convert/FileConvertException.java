package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 文件转换异常
 * @author fukang
 * @version 1.0.0
 * @date 2024/5/31 00:09
 */
public class FileConvertException extends LuckyRuntimeException {

    public FileConvertException(String message) {
        super(message);
    }

    public FileConvertException(Throwable ex) {
        super(ex);
    }

    public FileConvertException(String message, Throwable ex) {
        super(message, ex);
    }

    public FileConvertException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FileConvertException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
