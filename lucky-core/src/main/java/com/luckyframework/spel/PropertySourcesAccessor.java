package com.luckyframework.spel;

import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/10/22 00:59
 */
public class PropertySourcesAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[] {PropertySources.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        return target instanceof PropertySources;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        Assert.state(target instanceof PropertySources, "Target must be of type PropertySource");

        Object value = new PropertySourcesPropertyResolver(((PropertySources) target)).getProperty(name, Object.class);
        value = (value instanceof LazyValue) ? ((LazyValue<?>) value).getValue() : value;
        return new TypedValue(value);
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new UnsupportedOperationException("Should not be called on an PropertySourcesAccessor");
    }
}
