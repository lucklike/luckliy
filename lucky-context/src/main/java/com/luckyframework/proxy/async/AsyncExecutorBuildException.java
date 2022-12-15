package com.luckyframework.proxy.async;

public class AsyncExecutorBuildException extends RuntimeException{
    public AsyncExecutorBuildException(Throwable e,String msg){
        super(msg,e);
    }
}
