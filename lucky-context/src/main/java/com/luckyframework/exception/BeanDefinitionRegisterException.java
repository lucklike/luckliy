package com.luckyframework.exception;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/12 0012 18:29
 */
public class BeanDefinitionRegisterException extends RuntimeException{

    public BeanDefinitionRegisterException(String mess) {
        super(mess);
    }

    public BeanDefinitionRegisterException(String mess, Throwable e) {
        super(mess, e);
    }
}
//BeanDefinitionIllegal