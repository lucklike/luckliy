package com.luckyframework.exception;

public class LuckyFormatException extends RuntimeException{

    public LuckyFormatException(String msg){
        super(msg);
    }

    public LuckyFormatException(Throwable e,String msg){
        super(msg,e);
    }

    public LuckyFormatException(Throwable e){
        super(e);
    }
}
