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

    public LuckyIOException(String msg, IOException ioe){
        super(msg,ioe);
    }

    public LuckyIOException(String msg){
        super(msg);
    }

    public LuckyIOException(IOException ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }

    public LuckyIOException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public LuckyIOException(String message, Throwable ex) {
        super(message, ex);
    }
}