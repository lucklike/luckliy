package com.luckyframework.exception;

/**
 * Lucky反射异常
 * @author fk7075
 * @version 1.0.0
 * @date 2020/11/14 7:19 上午
 */
public class LuckyReflectionException extends LuckyRuntimeException {

    public LuckyReflectionException(String msg, Throwable e){
        super(msg,e);
    }

    public LuckyReflectionException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public LuckyReflectionException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }

    public LuckyReflectionException(Throwable e){
        super(e);
    }

    public LuckyReflectionException(String msg){
        super(msg);
    }

}
