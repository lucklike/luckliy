package com.luckyframework.exception;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/17 0017 10:45
 */
public class BeanCreationNotAllowedException extends BeanCreationException {


    /**
     * Create a new BeanCreationNotAllowedException.
     * @param beanName the name of the bean requested
     * @param msg the detail message
     */
    public BeanCreationNotAllowedException(String beanName, String msg) {
        super(beanName, msg);
    }

}
