package com.luckyframework.exception;

import com.luckyframework.definition.BeanDefinition;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/23 0023 10:15
 */
public class BeanDefinitionIllegalException extends RuntimeException {

    public BeanDefinitionIllegalException(String beanName, BeanDefinition beanDefinition){
        super(String.format("The bean definition information with the name '%s' is illegal. {%s}",beanName,beanDefinition));
    }
}
