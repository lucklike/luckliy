package com.luckyframework.aop.aspectj;

import com.luckyframework.aop.advice.Advice;
import com.luckyframework.aop.advisor.Advisor;
import com.luckyframework.aop.pointcut.Pointcut;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/13 0013 14:39
 */
public class DefaultAdvisor implements Advisor {

    private Advice advice;
    private Pointcut pointcut;

    public DefaultAdvisor(Advice advice, Pointcut pointcut) {
        this.advice = advice;
        this.pointcut = pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    @Override
    public void setAdvice(Advice advice) {
        this.advice = advice;
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public void setPointcut(Pointcut pointcut) {
        this.pointcut = pointcut;
    }

}
