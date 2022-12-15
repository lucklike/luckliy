package com.luckyframework.exception;

/**
 * 工厂bean构建异常
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/4 下午11:19
 */
public class FactoryBeanCreateException extends RuntimeException{
    public FactoryBeanCreateException(String msg){
        super(msg);
    }
}
