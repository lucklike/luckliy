package com.luckyframework.httpclient.exception;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 4:38 下午
 */
public class NotFindRequestException extends RuntimeException{

    public NotFindRequestException(String message){
        super(message);
    }
}
