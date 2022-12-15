package com.luckyframework.exception;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/3/14 上午1:26
 */
public class BeanCurrentlyInCreationException extends RuntimeException {

    public BeanCurrentlyInCreationException(String message){
        super(message);
    }


}
