package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.spel.EvaluationContextFactory;
import com.luckyframework.spel.MethodSpaceMethodResolver;
import com.luckyframework.spel.ParamWrapper;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class LuckyHttpClientEvaluationContextFactory implements EvaluationContextFactory {

    @Override
    public EvaluationContext getEvaluationContext(ParamWrapper paramWrapper) {
        StandardEvaluationContext evaluationContext = (StandardEvaluationContext) EvaluationContextFactory.DEFAULT_FACTORY.getEvaluationContext(paramWrapper);
        evaluationContext.addMethodResolver(new MethodSpaceMethodResolver(getFunctionSpace()));
        return evaluationContext;
    }

    private List<String> getFunctionSpace() {
        List<Field> allStaticFieldOrder = ClassUtils.getAllStaticFieldOrder(MethodSpaceConstant.class);
        List<String> functionSpace = new ArrayList<>(allStaticFieldOrder.size());
        for (Field field : allStaticFieldOrder) {
            functionSpace.add((String) FieldUtils.getValue(null, field));
        }
        return functionSpace;
    }
}
