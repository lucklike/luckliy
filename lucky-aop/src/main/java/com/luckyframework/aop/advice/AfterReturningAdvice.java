package com.luckyframework.aop.advice;

import org.aspectj.lang.JoinPoint;

/**
 * 正常执行后的后置增强通知
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 11:24
 */
public interface AfterReturningAdvice extends Advice {

    void afterReturning(JoinPoint joinPoint,Object returning);

}
