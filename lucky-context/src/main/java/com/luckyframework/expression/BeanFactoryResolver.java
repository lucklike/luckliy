package com.luckyframework.expression;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.exception.BeansException;
import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/18 00:37
 */
public class BeanFactoryResolver implements BeanResolver {

    private final BeanFactory beanFactory;

    public BeanFactoryResolver(BeanFactory beanFactory) {
        Assert.notNull(beanFactory, "BeanFactory must not be null");
        this.beanFactory = beanFactory;
    }

    @Override
    public Object resolve(EvaluationContext context, String beanName) throws AccessException {
        try {
            return beanFactory.getBean(beanName);
        }catch (BeansException ex){
            throw new AccessException("Could not resolve bean reference against BeanFactory", ex);
        }
    }
}
