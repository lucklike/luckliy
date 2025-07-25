package com.luckyframework.spel;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.support.ReflectiveMethodExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 支持从Map中获取方法执行的{@link MethodResolver}
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/07/25 11:07
 */
public class MethodSpaceMethodResolver implements MethodResolver {

    private final List<String> namespaceList;

    public MethodSpaceMethodResolver(List<String> namespacesList) {
        this.namespaceList = namespacesList;
    }

    public MethodSpaceMethodResolver(String... namespaces) {
        this(Arrays.asList(namespaces));
    }

    public MethodSpaceMethodResolver() {
        this(new ArrayList<>());
    }

    public void addNamespace(String... namespaces) {
        addNamespaces(Arrays.asList(namespaces));
    }

    public void addNamespaces(Collection<String> namespaces) {
        this.namespaceList.addAll(namespaces);
    }

    @Override
    public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> argumentTypes) throws AccessException {
        if (targetObject instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) targetObject;
            Object mapValue = map.get(name);
            if (mapValue instanceof Method) {
                return new ReflectiveMethodExecutor((Method) mapValue);
            }
            if (mapValue instanceof Map) {
                for (String namespace : namespaceList) {
                    Object namespaceValue = ((Map<?, ?>) mapValue).get(namespace);
                    if (namespaceValue instanceof Map) {
                        Object namespaceValueValue = ((Map<?, ?>) namespaceValue).get(name);
                        if (namespaceValueValue instanceof Method) {
                            return new ReflectiveMethodExecutor((Method) namespaceValueValue);
                        }
                    }
                }
            }
        }
        return null;
    }
}
