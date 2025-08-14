package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.support.ReflectiveMethodExecutor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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

        // 非命名空间函数调用直接返回null
        if (!isNamespaceMethodInvoke(targetObject, name)) {
            return null;
        }

        // 优先从指定的命名空间中进行查找
        for (String namespace : namespaceList) {
            Object namespaceValue = context.lookupVariable(namespace);
            if (namespaceValue instanceof Map) {
                Object namespaceValueValue = ((Map<?, ?>) namespaceValue).get(name);
                if (isNamespaceMethod(namespaceValueValue)) {
                    return new ReflectiveMethodExecutor((Method) namespaceValueValue);
                }
            }
        }

        // 再从targetObject中查找
        Map<?, ?> map = (Map<?, ?>) targetObject;
        Object mapValue = map.get(name);
        if (isNamespaceMethod(mapValue)) {
            return new ReflectiveMethodExecutor((Method) mapValue);
        }

        // 再尝试从变量列表中查找
        Object varObj = context.lookupVariable(name);
        if (isNamespaceMethod(varObj)) {
            return new ReflectiveMethodExecutor((Method) varObj);
        }

        return null;
    }

    /**
     * 是否为命名空间方法
     *
     * @param methodObj 方法对象
     * @return 是否为命名空间方法
     */
    private boolean isNamespaceMethod(Object methodObj) {
        return (methodObj instanceof Method) && Modifier.isStatic(((Method) methodObj).getModifiers());
    }

    /**
     * 是否为命名空间函数调用
     * <pre>
     *     1.targetObject必须为Map类型
     *     2.name并不是targetObject上的方法
     * </pre>
     *
     * @param targetObject Target对象
     * @param name         方法名称
     * @return 是否为命名空间函数调用
     */
    private boolean isNamespaceMethodInvoke(Object targetObject, String name) {
        if (!(targetObject instanceof Map)) {
            return false;
        }

        return !Arrays.stream(ClassUtils.getAllMethod(targetObject.getClass()))
                .map(Method::getName)
                .collect(Collectors.toSet())
                .contains(name);
    }
}
