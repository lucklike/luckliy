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

    /**
     * 创建类型定位器
     *
     * @param pw 执行参数
     * @return 类型定位器
     */
    default StandardTypeLocator createStandardTypeLocator(ParamWrapper pw) {
        StandardTypeLocator typeLocator = new StandardTypeLocator(com.luckyframework.reflect.ClassUtils.getDefaultClassLoader());
        pw.getKnownPackagePrefixes().forEach(typeLocator::registerImport);
        return typeLocator;
    }

    class DefaultEvaluationContextFactory implements EvaluationContextFactory {
        @Override
        public EvaluationContext getEvaluationContext(ParamWrapper paramWrapper) {
            StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
            evaluationContext.addPropertyAccessor(new NotExistReturnNullMapAccessor());
            evaluationContext.addPropertyAccessor(new ClassFieldAccessor());
            evaluationContext.addPropertyAccessor(new AnnotationAccessor());
            evaluationContext.setTypeLocator(new StandardTypeLocator(ClassUtils.getDefaultClassLoader()));

            evaluationContext.setTypeLocator(createStandardTypeLocator(paramWrapper));
            evaluationContext.setVariables(paramWrapper.getVariables());
            evaluationContext.setRootObject(paramWrapper.getRootObject());
            return evaluationContext;
        }
    }
}
