package com.luckyframework.spel;

import com.luckyframework.common.ExpressionBean;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;

public class ExpressionBeanAccessor implements PropertyAccessor {
    @Nullable
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[]{ExpressionBean.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return target instanceof ExpressionBean;
    }

    @Override
    public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return new TypedValue(((ExpressionBean<?>) target).get(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return target instanceof ExpressionBean;
    }

    @Override
    public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) throws AccessException {
        ((ExpressionBean<?>) target).set(name, newValue);
    }
}
