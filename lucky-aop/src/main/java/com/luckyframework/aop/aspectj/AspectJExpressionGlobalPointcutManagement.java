package com.luckyframework.aop.aspectj;


import com.luckyframework.aop.pointcut.Pointcut;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/14 0014 11:00
 */
public class AspectJExpressionGlobalPointcutManagement implements ExpressionGlobalPointcutManagement {

    private final Map<String, AspectJExpressionPointcut> pointcutMap = new ConcurrentHashMap<>(32);

    @Override
    public Pointcut getPointcutByExpression(String prefix, String standbyExpression) {
        if(pointcutMap.containsKey(prefix)){
            return pointcutMap.get(prefix);
        }
        return new AspectJExpressionPointcut(standbyExpression);
    }

    @Override
    public void addExpressionPointcut(String prefix, String expression) {
        pointcutMap.put(prefix,new AspectJExpressionPointcut(expression));
    }

    @Override
    public void addPointcut(Pointcut pointcut) {
        throw new RuntimeException("AspectJExpressionGlobalPointcutManagement does not support the 'addPointcut' method");
    }

    @Override
    public Collection<? extends Pointcut> getAllPointcut() {
        return pointcutMap.values();
    }
}
