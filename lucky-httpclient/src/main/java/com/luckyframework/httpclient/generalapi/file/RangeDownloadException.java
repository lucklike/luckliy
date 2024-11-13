package com.luckyframework.httpclient.generalapi.file;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 分片文件下载异常
 */
public class RangeDownloadException extends LuckyRuntimeException {
    public RangeDownloadException(String message) {
        super(message);
    }

    public RangeDownloadException(Throwable ex) {
        super(ex);
    }

    public RangeDownloadException(String message, Throwable ex) {
        super(message, ex);
    }

    public RangeDownloadException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public RangeDownloadException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
