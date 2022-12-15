package com.luckyframework.exception;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/5/15 15:00
 */
public class CglibObjectCreatorException extends RuntimeException{

    public CglibObjectCreatorException(){
        super();
    }

    public CglibObjectCreatorException(String message){
        super(message);
    }

    public CglibObjectCreatorException(String message,Throwable th){
        super(message,th);
    }
}
