package com.luckyframework.httpclient.exception;

/**
 * 响应处理异常
 * @author fk7075
 * @version 1.0
 * @date 2021/9/15 11:30 下午
 */
public class ResponseProcessException extends RuntimeException {
    public ResponseProcessException(String message){
        super(message);
    }
}
