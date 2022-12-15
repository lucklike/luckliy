package com.luckyframework.expression;

import com.luckyframework.environment.LuckyStandardEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/17 22:53
 */
public class EnvironmentPropertyAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[]{Environment.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        return ((Environment)target).containsProperty(name);
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        return new TypedValue(((LuckyStandardEnvironment)target).getPropertyForObject(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new UnsupportedOperationException("Should not be called on an EnvironmentPropertyAccessor");
    }
}
