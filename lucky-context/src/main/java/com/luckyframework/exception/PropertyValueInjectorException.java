package com.luckyframework.exception;

public class PropertyValueInjectorException extends RuntimeException{

    public PropertyValueInjectorException(Throwable e,String message){
        super(message,e);
    }

}
