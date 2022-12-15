package com.luckyframework.serializable;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/2/19 上午2:27
 */
public class SerializationException extends RuntimeException{

    public SerializationException(String msg){
        super(msg);
    }

    public SerializationException(Throwable e,String msg){
        super(msg,e);
    }

    public SerializationException(Throwable e){
        super(e);
    }
}
