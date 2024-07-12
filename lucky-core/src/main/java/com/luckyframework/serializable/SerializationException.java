package com.luckyframework.serializable;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/2/19 上午2:27
 */
public class SerializationException extends LuckyRuntimeException {

    public SerializationException(String msg){
        super(msg);
    }

    public SerializationException(Throwable e,String msg){
        super(msg,e);
    }

    public SerializationException(Throwable e){
        super(e);
    }

    public SerializationException(String message, Throwable ex) {
        super(message, ex);
    }

    public SerializationException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public SerializationException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
