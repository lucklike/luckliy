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

    /**
     * <pre>
     *     1.如果是targetObject对象上的方法时，不处理
     *     2.尝试从特定的命名空间中查找
     *     3.尝试从变量变中直接查找
     *     4.最后如果TargetObject为Map时，才会尝试从TargetObject上去查找
     * </pre>
     *
     * @param context the current evaluation context
     * @param targetObject the object upon which the method is being called
     * @param name
     * @param argumentTypes the arguments that the constructor must be able to handle
     * @return
     * @throws AccessException
     */
    @Override
    public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> argumentTypes) throws AccessException {

        // name为TargetObject上的方法时，本解析器不处理
        if (isTargetObjectMethod(targetObject, name)) {
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

        // 再尝试从变量列表中查找
        Object varObj = context.lookupVariable(name);
        if (isNamespaceMethod(varObj)) {
            return new ReflectiveMethodExecutor((Method) varObj);
        }

        // 最后如果TargetObject为Map时，才会尝试从TargetObject上去查找
        if (!(targetObject instanceof Map)) {
            return null;
        }

        // 再从targetObject中查找
        Map<?, ?> map = (Map<?, ?>) targetObject;
        Object mapValue = map.get(name);
        if (isNamespaceMethod(mapValue)) {
            return new ReflectiveMethodExecutor((Method) mapValue);
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
     * 是否为TargetObject上的方法
     *
     * @param targetObject TargetObject
     * @param methodName   方法名
     * @return 是否为TargetObject上的方法
     */
    private boolean isTargetObjectMethod(Object targetObject, String methodName) {
        return Arrays.stream(ClassUtils.getAllMethod(targetObject.getClass()))
                .map(Method::getName)
                .collect(Collectors.toSet())
                .contains(methodName);
    }
}
