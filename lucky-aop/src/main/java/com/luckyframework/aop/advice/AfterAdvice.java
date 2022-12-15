package com.luckyframework.aop.advice;

import org.aspectj.lang.JoinPoint;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 11:27
 */
public interface AfterAdvice extends Advice {

    void after(JoinPoint joinPoint);

}
