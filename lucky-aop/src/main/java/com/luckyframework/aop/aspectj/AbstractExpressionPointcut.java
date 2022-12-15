package com.luckyframework.aop.aspectj;


import com.luckyframework.aop.pointcut.Pointcut;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/18 0018 14:27
 */
public abstract class AbstractExpressionPointcut implements Pointcut {

    //表达式
    private String expression;

    public AbstractExpressionPointcut(String expression){
        this.expression=expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getExpression(){
        return this.expression;
    }

}
