package com.luckyframework.scheduler.quartz.exceptions;

/**
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/3 01:37
 */
public class QuartzJobExecuteComponentAssembleException extends RuntimeException{

    public QuartzJobExecuteComponentAssembleException(String message){
        super(message);
    }

    public QuartzJobExecuteComponentAssembleException(Throwable th){
       super(th);
    }
}
