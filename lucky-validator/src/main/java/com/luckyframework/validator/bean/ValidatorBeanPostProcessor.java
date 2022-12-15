package com.luckyframework.validator.bean;

import com.luckyframework.bean.aware.BeanFactoryAware;
import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.bean.factory.BeanPostProcessor;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.exception.BeanCreationException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/18 16:16
 */
public class ValidatorBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private Validator validator;

    @Override
    public Object postProcessAfterInitialization(String beanName, FactoryBean factoryBean, Object bean) {
        Set<ConstraintViolation<Object>> validate =validator.validate(bean);
        if(!validate.isEmpty()){
            throw new BeanCreationException(beanName, "Attribute check exception!",new ConstraintViolationException(validate));
        }
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        ValidatorFactory validatorFactory = beanFactory.getBean(ValidatorFactory.class);
        if(validatorFactory != null){
            this.validator = validatorFactory.getValidator();
        }
        else{
            this.validator = beanFactory.getBean(Validator.class);
        }
        if(this.validator == null){
            throw new BeanCreationException("Neither 'Validator' nor 'ValidatorFactory' was found, unable to create a 'ValidatorBeanPostProcessor' instance");
        }
    }
}
