package com.luckyframework.httpclient.exception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 响应处理异常
 * @author fk7075
 * @version 1.0
 * @date 2021/9/15 11:30 下午
 */
public class ResponseProcessException extends LuckyRuntimeException {
    public ResponseProcessException(String message){
        super(message);
    }

    public ResponseProcessException(Throwable ex) {
        super(ex);
    }

    public ResponseProcessException(String message, Throwable ex) {
        super(message, ex);
    }

    public ResponseProcessException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ResponseProcessException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }

}
