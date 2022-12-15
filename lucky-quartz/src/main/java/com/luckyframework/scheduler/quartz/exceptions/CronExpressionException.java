package com.luckyframework.scheduler.quartz.exceptions;

/**
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/3 00:25
 */
public class CronExpressionException extends RuntimeException{

    public CronExpressionException(String message){
        super(message);
    }
}
