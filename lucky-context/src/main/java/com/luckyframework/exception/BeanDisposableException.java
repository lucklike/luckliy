package com.luckyframework.exception;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/26 0026 11:33
 */
public class BeanDisposableException extends  RuntimeException{

    public BeanDisposableException(String msg,Throwable e){
        super(msg,e);
    }
}
