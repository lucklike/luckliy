package com.luckyframework.exception;

/**
 * 组件创建异常
 * @author fk7075
 * @version 1.0.0
 * @date 2020/11/14 1:44 下午
 */
public class LuckyBeanCreateException extends RuntimeException{

    public LuckyBeanCreateException(String msg){
        super(msg);
    }

    public LuckyBeanCreateException(Throwable e){
        super(e);
    }
}
