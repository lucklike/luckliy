package com.luckyframework.aop.advice;

import org.aspectj.lang.JoinPoint;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 11:28
 */
public interface BeforeAdvice extends Advice {

    void before(JoinPoint joinPoint);
}
