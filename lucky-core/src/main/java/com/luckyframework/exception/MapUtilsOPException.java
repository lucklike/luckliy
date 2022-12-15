package com.luckyframework.exception;

/**
 * MapUtils操作异常
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/29 上午10:38
 */
public class MapUtilsOPException extends RuntimeException{

    public MapUtilsOPException(String msg){
        super(msg);
    }

    public MapUtilsOPException(Throwable e){
        super(e);
    }

    public MapUtilsOPException(String msg,Throwable e){
        super(msg,e);
    }
}
