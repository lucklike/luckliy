package com.luckyframework.expression;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.spel.ClassFieldAccessor;
import com.luckyframework.spel.EvaluationContextFactory;
import com.luckyframework.spel.MapAccessor;
import com.luckyframework.spel.ParamWrapper;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.util.ClassUtils;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/19 18:04
 */
public class BeanFactoryEvaluationContextFactory implements EvaluationContextFactory {

    private final BeanFactory beanFactory;

    public BeanFactoryEvaluationContextFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public EvaluationContext getEvaluationContext(ParamWrapper paramWrapper) {
        StandardEvaluationContext evaluationContext = (StandardEvaluationContext)getDefaultEvaluationContext();
        evaluationContext.setTypeLocator(createStandardTypeLocator(paramWrapper));
        evaluationContext.setVariables(paramWrapper.getVariables());
        evaluationContext.setRootObject(paramWrapper.getRootObject());
        return evaluationContext;
    }

    public EvaluationContext getDefaultEvaluationContext() {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        evaluationContext.addPropertyAccessor(new BeanFactoryPropertyAccessor());
        evaluationContext.addPropertyAccessor(new MapAccessor());
        evaluationContext.addPropertyAccessor(new EnvironmentPropertyAccessor());
        evaluationContext.addPropertyAccessor(new ClassFieldAccessor());
        evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
        evaluationContext.setTypeLocator(new StandardTypeLocator(ClassUtils.getDefaultClassLoader()));
        evaluationContext.setTypeConverter(new StandardTypeConverter(new LuckyConversionService()));
        return evaluationContext;
    }
}
