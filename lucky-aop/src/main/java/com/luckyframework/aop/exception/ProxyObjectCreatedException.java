package com.luckyframework.aop.exception;

public class ProxyObjectCreatedException extends RuntimeException{

    public ProxyObjectCreatedException(){
        super();
    }

    public ProxyObjectCreatedException(String message){
        super(message);
    }

    public ProxyObjectCreatedException(String message,Throwable e){
        super(message,e);
    }

}
