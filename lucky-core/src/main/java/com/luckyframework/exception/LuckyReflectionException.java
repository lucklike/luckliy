package com.luckyframework.exception;

/**
 * Lucky反射异常
 * @author fk7075
 * @version 1.0.0
 * @date 2020/11/14 7:19 上午
 */
public class LuckyReflectionException extends RuntimeException{

    public LuckyReflectionException(String msg, Throwable e){
        super(msg,e);
    }

    public LuckyReflectionException(Throwable e){
        super(e);
    }

    public LuckyReflectionException(String msg){
        super(msg);
    }
}
