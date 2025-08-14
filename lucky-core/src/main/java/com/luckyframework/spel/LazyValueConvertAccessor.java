package com.luckyframework.spel;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * 支持将{@link LazyValue}结果转换出来的PropertyAccessor
 */
public abstract class LazyValueConvertAccessor implements PropertyAccessor {

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        Object value = readValue(context, target, name);
        return new TypedValue((value instanceof LazyValue) ? ((LazyValue) value).getValue() : value);
    }

    protected abstract Object readValue(EvaluationContext context, Object target, String name) throws AccessException;
}
