package com.luckyframework.exception;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/16 0016 10:28
 */
public class MultipleMatchExceptions extends RuntimeException{

    public MultipleMatchExceptions(Class<?> type){
        super("Matches multiple instances of this '"+type+"'");
    }
}
