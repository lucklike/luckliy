package com.luckyframework.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.util.ClassUtils;

/**
 * SpEL表达式上下文工厂
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/19 15:21
 */
@FunctionalInterface
public interface EvaluationContextFactory {

    EvaluationContextFactory DEFAULT_FACTORY = new DefaultEvaluationContextFactory();

    /**
     * 创建一个SpEL表达式上下文
     *
     * @param paramWrapper 参数包装器
     * @return SpEL表达式上下文
     */
    EvaluationContext getEvaluationContext(ParamWrapper paramWrapper);



    class DefaultEvaluationContextFactory implements EvaluationContextFactory {
        @Override
        public EvaluationContext getEvaluationContext(ParamWrapper paramWrapper) {
            StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
            evaluationContext.addPropertyAccessor(new LazyValueAccessor());
            evaluationContext.addPropertyAccessor(new NotExistReturnNullMapAccessor());
            evaluationContext.addPropertyAccessor(new ClassFieldAccessor());
            evaluationContext.addPropertyAccessor(new AnnotationAccessor());

            evaluationContext.setTypeLocator(paramWrapper.getTypeLocator());
            evaluationContext.setVariables(paramWrapper.getVariables());
            evaluationContext.setRootObject(paramWrapper.getRootObject());
            return evaluationContext;
        }
    }
}
