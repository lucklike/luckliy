package com.luckyframework.exception;

import java.io.IOException;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/25 0025 18:43
 */
public class LuckyIOException extends LuckyRuntimeException{

    public LuckyIOException(IOException ioe){
        super(ioe);
    }

    public LuckyIOException(String msg,IOException ioe){
        super(msg,ioe);
    }

    public LuckyIOException(String message) {
        super(message);
    }

    public LuckyIOException(Throwable ex) {
        super(ex);
    }

    public LuckyIOException(String message, Throwable ex) {
        super(message, ex);
    }

    public LuckyIOException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public LuckyIOException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}