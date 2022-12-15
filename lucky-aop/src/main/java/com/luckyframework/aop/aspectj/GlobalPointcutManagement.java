package com.luckyframework.aop.aspectj;


import com.luckyframework.aop.pointcut.Pointcut;

import java.util.Collection;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/13 0013 11:54
 */
public interface GlobalPointcutManagement {

    void addPointcut(Pointcut pointcut);

    Collection<?extends Pointcut> getAllPointcut();
}
