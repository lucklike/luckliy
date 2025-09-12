package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.spel.EvaluationContextFactory;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.RestrictedTypeLocator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;

public class LuckyHttpClientEvaluationContextFactory implements EvaluationContextFactory {

    @Override
    public EvaluationContext getEvaluationContext(ParamWrapper paramWrapper) {
        StandardEvaluationContext evaluationContext = (StandardEvaluationContext) EvaluationContextFactory.DEFAULT_FACTORY.getEvaluationContext(paramWrapper);

        // PropertyAccessor
        List<PropertyAccessor> propertyAccessors = evaluationContext.getPropertyAccessors();
        propertyAccessors.add(0, new ValueSpacePropertyAccessor(ValueSpaceConstant.getSpaces()));

        // addMethodResolver
        TypeLocator typeLocator = evaluationContext.getTypeLocator();
        if (typeLocator instanceof RestrictedTypeLocator) {
            evaluationContext.addMethodResolver((RestrictedTypeLocator) typeLocator);
        }
        evaluationContext.addMethodResolver(new MethodSpaceMethodResolver(MethodSpaceConstant.getSpaces()));
        return evaluationContext;
    }
}
