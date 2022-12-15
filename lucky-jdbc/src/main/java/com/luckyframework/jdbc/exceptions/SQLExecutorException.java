package com.luckyframework.jdbc.exceptions;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/30 11:38 上午
 */
public class SQLExecutorException extends RuntimeException {

    public SQLExecutorException(Exception e){
        super(e);
    }

    public SQLExecutorException(String message){
        super(message);
    }
}
