package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.spel.EvaluationContextFactory;
import com.luckyframework.spel.ParamWrapper;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class LuckyHttpClientEvaluationContextFactory implements EvaluationContextFactory {

    @Override
    public EvaluationContext getEvaluationContext(ParamWrapper paramWrapper) {
        StandardEvaluationContext evaluationContext = (StandardEvaluationContext) EvaluationContextFactory.DEFAULT_FACTORY.getEvaluationContext(paramWrapper);
        List<PropertyAccessor> propertyAccessors = evaluationContext.getPropertyAccessors();
        propertyAccessors.add(0, new ValueSpacePropertyAccessor(getAllFieldNameOrder(ValueSpaceConstant.class)));
        evaluationContext.addMethodResolver(new MethodSpaceMethodResolver(getAllFieldNameOrder(MethodSpaceConstant.class)));
        return evaluationContext;
    }

    private List<String> getAllFieldNameOrder(Class<?> clazz) {
        List<Field> allStaticFieldOrder = ClassUtils.getAllStaticFieldOrder(clazz);
        List<String> staticFieldNameList = new ArrayList<>(allStaticFieldOrder.size());
        for (Field field : allStaticFieldOrder) {
            staticFieldNameList.add((String) FieldUtils.getValue(null, field));
        }
        return staticFieldNameList;
    }
}
