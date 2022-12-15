package com.luckyframework.aop.advisor;


import com.luckyframework.aop.advice.Advice;
import com.luckyframework.aop.pointcut.Pointcut;

/**
 * 切面，用于组织Advice和Pointcut
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 11:36
 */
public interface Advisor {

    Advice getAdvice();

    void setAdvice(Advice advice);

    Pointcut getPointcut();

    void setPointcut(Pointcut pointcut);

}
