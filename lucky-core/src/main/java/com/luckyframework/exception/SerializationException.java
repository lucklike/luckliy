package com.luckyframework.exception;

/**
 * 序列化异常
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 11:09 下午
 */
public class SerializationException extends RuntimeException{

    public SerializationException(Throwable e){
        super(e);
    }
}
