package com.luckyframework.conversion;

public class TypeConversionException extends RuntimeException{

    public TypeConversionException(String msg){
        super(msg);
    }

    public TypeConversionException(String msg,Throwable ex){
        super(msg,ex);
    }

}
