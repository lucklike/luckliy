package com.luckyframework.expression;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.bean.factory.StandardSingletonBeanFactory;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.environment.LuckyStandardEnvironment;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/17 22:49
 */
public class BeanFactoryPropertyAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[]{BeanFactory.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        if(target instanceof BeanFactory){
            if(((BeanFactory) target).containsBean(name)){
                return true;
            }
            if(target instanceof StandardSingletonBeanFactory){
                return ((StandardSingletonBeanFactory)target).getEnvironment().containsProperty(name);
            }
            if(target instanceof ApplicationContext){
                return ((ApplicationContext)target).getEnvironment().containsProperty(name);
            }
        }
        return false;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        Object value = null;
        BeanFactory beanFactory = (BeanFactory) target;
        if(beanFactory.containsBean(name)){
            value = beanFactory.getBean(name);
        } else if(target instanceof StandardSingletonBeanFactory){
            value = ((LuckyStandardEnvironment)((StandardSingletonBeanFactory)target).getEnvironment()).getPropertyForObject(name);
        }else if(target instanceof ApplicationContext){
            value = ((LuckyStandardEnvironment)((ApplicationContext)target).getEnvironment()).getPropertyForObject(name);
        }
        return new TypedValue(value);
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        if(target instanceof StandardSingletonBeanFactory){
            return ((StandardSingletonBeanFactory)target).getEnvironment().containsProperty(name);
        }
        if(target instanceof ApplicationContext){
            return ((ApplicationContext)target).getEnvironment().containsProperty(name);
        }
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new UnsupportedOperationException("Should not be called on an BeanFactoryPropertyAccessor");
    }
}
