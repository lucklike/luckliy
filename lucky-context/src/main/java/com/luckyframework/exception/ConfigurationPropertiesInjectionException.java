package com.luckyframework.exception;

public class ConfigurationPropertiesInjectionException extends RuntimeException{

    public ConfigurationPropertiesInjectionException(String msg){
        super(msg);
    }

    public ConfigurationPropertiesInjectionException(String msg,Throwable e){
        super(msg,e);
    }
}
