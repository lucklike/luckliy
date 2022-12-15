package com.luckyframework.aop.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fk7075
 * @version 1.0
 * @date 2020/9/25 15:33
 */
public class AopParamsConfigurationException extends RuntimeException {

    private static final Logger log= LoggerFactory.getLogger(AopParamsConfigurationException.class);

    public AopParamsConfigurationException(String msg){
        super(msg);
        log.error(msg,this);
    }

    public AopParamsConfigurationException(String msg, Throwable e){
        super(msg,e);
        log.error(msg,this);
    }
}
