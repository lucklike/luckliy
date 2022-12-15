package com.luckyframework.scheduler.quartz.exceptions;

import com.luckyframework.scheduler.quartz.annotations.Scheduled;

/**
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/3 01:48
 */
public class ScheduledConfigurationException extends RuntimeException{

    public ScheduledConfigurationException(Scheduled scheduled){
        super("ScheduledConfigurationException :"+scheduled.toString());
    }

    public ScheduledConfigurationException(String message){
        super(message);
    }

}
