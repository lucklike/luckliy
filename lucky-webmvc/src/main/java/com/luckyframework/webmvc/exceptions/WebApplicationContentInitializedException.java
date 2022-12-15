package com.luckyframework.webmvc.exceptions;

public class WebApplicationContentInitializedException extends RuntimeException{

    public WebApplicationContentInitializedException(String errorMessage){
        super(errorMessage);
    }

    public WebApplicationContentInitializedException(String errorMessage , Throwable e){
        super(errorMessage,e);
    }
}
