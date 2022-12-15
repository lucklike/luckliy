package com.luckyframework.aop.pointcut;

import java.lang.reflect.Method;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 11:33
 */
public interface Pointcut {

    boolean matchClass(String currentBeanName,Class<?> targetClass);

    boolean matchMethod(Class<?> targetClass, Method method,Object...args);

}
