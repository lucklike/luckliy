package com.luckyframework.aop.proxy;

import com.luckyframework.context.ApplicationContext;
import com.luckyframework.definition.BeanFactoryCglibObjectCreator;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/5/15 06:12
 */
public class ApplicationContextCglibObjectCreator extends BeanFactoryCglibObjectCreator {

    public ApplicationContextCglibObjectCreator(Class<?> targetClass, ApplicationContext applicationContext) {
        super(targetClass,applicationContext,applicationContext.getEnvironment());
    }

}
