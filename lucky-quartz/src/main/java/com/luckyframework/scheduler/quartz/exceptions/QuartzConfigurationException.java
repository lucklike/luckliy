package com.luckyframework.scheduler.quartz.exceptions;

/**
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/4 23:02
 */
public class QuartzConfigurationException extends RuntimeException{

    public QuartzConfigurationException(){
        super();
    }

    public QuartzConfigurationException(String message){
        super(message);
    }

    public QuartzConfigurationException(Throwable th){
        super(th);
    }
}
