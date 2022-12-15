package com.luckyframework.aop.aspectj;


import com.luckyframework.aop.pointcut.Pointcut;

/**
 * 基于表达式的全局Pointcut管理器
 * @author fk
 * @version 1.0
 * @date 2021/4/13 0013 11:58
 */
public interface ExpressionGlobalPointcutManagement extends GlobalPointcutManagement {

    String CONNECTOR = "#";
    String PARENTHESES ="()";

    Pointcut getPointcutByExpression(String prefix, String standbyExpression);

    void addExpressionPointcut(String prefix,String expression);


}
