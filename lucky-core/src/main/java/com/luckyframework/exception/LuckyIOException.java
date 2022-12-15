package com.luckyframework.exception;

import java.io.IOException;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/25 0025 18:43
 */
public class LuckyIOException extends RuntimeException{

    public LuckyIOException(IOException ioe){
        super(ioe);
    }

    public LuckyIOException(String msg, IOException ioe){
        super(msg,ioe);
    }

}