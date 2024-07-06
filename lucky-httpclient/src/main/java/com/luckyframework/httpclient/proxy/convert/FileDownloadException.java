package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 文件下载异常
 * @author fukang
 * @version 1.0.0
 * @date 2024/5/31 00:09
 */
public class FileDownloadException extends LuckyRuntimeException {

    public FileDownloadException(String message) {
        super(message);
    }

    public FileDownloadException(Throwable ex) {
        super(ex);
    }

    public FileDownloadException(String message, Throwable ex) {
        super(message, ex);
    }

    public FileDownloadException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FileDownloadException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
