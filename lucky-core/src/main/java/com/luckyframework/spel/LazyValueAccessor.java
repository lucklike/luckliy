package com.luckyframework.spel;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

public class LazyValueAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[] {LazyValue.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        return target instanceof LazyValue;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        return new TypedValue(((LazyValue) target).getValue());
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new UnsupportedOperationException("Should not be called on an LazyValueAccessor");
    }
}
