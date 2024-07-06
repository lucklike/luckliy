package com.luckyframework.spel;

import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import java.lang.annotation.Annotation;

/**
 * 注解属性访问器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 03:11
 */
public class AnnotationAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[]{Annotation.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        return target instanceof Annotation;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        return new TypedValue(AnnotationUtils.getValue((Annotation) target, name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new UnsupportedOperationException("Should not be called on an AnnotationAccessor");
    }
}
