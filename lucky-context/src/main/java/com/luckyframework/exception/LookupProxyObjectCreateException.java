package com.luckyframework.exception;

/**
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/14 20:13
 */
public class LookupProxyObjectCreateException extends RuntimeException{
    public LookupProxyObjectCreateException(String message){
        super(message);
    }

    public LookupProxyObjectCreateException(Throwable th,String message){
        super(message,th);
    }
}
