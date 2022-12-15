package com.luckyframework.aop.exception;

import java.lang.reflect.Method;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/13 0013 16:31
 */
public class PositionExpressionConfigException extends RuntimeException {

    public PositionExpressionConfigException(String adviceType, Method method){
        super(String.format("%s-advice configuration exception, i.e. 'pointcut' and 'value' are not configured. Error location :%s",adviceType,method));
    }

}
