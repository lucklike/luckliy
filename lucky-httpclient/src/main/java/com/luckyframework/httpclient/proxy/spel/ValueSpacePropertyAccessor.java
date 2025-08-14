package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.spel.LazyValueConvertAccessor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;

import java.util.List;
import java.util.Map;

public class ValueSpacePropertyAccessor extends LazyValueConvertAccessor {

    private final List<String> namespaces;

    public ValueSpacePropertyAccessor(List<String> namespaces) {
        this.namespaces = namespaces;
    }


    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[]{Map.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        Map<?, ?> map = (Map<?, ?>) target;
        for (String namespace : namespaces) {
            Object namespaceValue = map.get(namespace);
            if (namespaceValue instanceof Map) {
                if (((Map<?, ?>) namespaceValue).containsKey(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Object readValue(EvaluationContext context, Object target, String name) throws AccessException {
        Map<?, ?> map = (Map<?, ?>) target;
        for (String namespace : namespaces) {
            Object namespaceValue = map.get(namespace);
            if (namespaceValue instanceof Map) {
                if (((Map<?, ?>) namespaceValue).containsKey(name)) {
                    return ((Map<?, ?>) namespaceValue).get(name);
                }
            }
        }
        return null;
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new UnsupportedOperationException("Should not be called on an ValueSpacePropertyAccessor");
    }
}
