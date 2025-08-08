package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 静态工具类实例，用于从某个工具类中提取出所有的公有静态方法实例
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:21
 */
public class ClassStaticElement {


    private static final Logger log = LoggerFactory.getLogger(ClassStaticElement.class);

    /**
     * 工具类Class
     */
    private Class<?> clazz;

    /**
     * 命名空间
     */
    private String namespace;

    public static ClassStaticElement create(String namespace, Class<?> clazz) {
        ClassStaticElement entry = new ClassStaticElement();
        if (!StringUtils.hasText(namespace)) {
            Namespace prefixAnn = AnnotationUtils.findMergedAnnotation(clazz, Namespace.class);
            if (prefixAnn != null && StringUtils.hasText(prefixAnn.value())) {
                namespace = prefixAnn.value();
            }
        }
        entry.setNamespace(namespace);
        entry.setClazz(clazz);
        return entry;
    }

    public static ClassStaticElement create(Class<?> clazz) {
        return create(null, clazz);
    }

    /**
     * 获取工具类的Class
     *
     * @return 工具类的Class
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * 设置工具类Class
     *
     * @param clazz 工具类Class
     */
    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * 获取方法的命名空间
     *
     * @return 方法名前缀
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 设置方法的命名空间
     *
     * @param namespace 命名空间
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * 获取所有公有静态方法名和方法实例所组成的Map
     *
     * @return 所有公有静态方法名和方法实例所组成的Map
     */
    public Map<String, Object> getAllStaticMethods() {
        Assert.notNull(clazz, "clazz cannot be null");
        List<Method> allStaticMethod = ClassUtils.getAllStaticMethod(clazz);

        Map<String, Object> methodMap = new HashMap<>();
        for (Method method : allStaticMethod) {
            if (method.isSynthetic()) {
                continue;
            }
            if (AnnotationUtils.isAnnotated(method, FunctionFilter.class)) {
                continue;
            }

            String methodName = FunctionAlias.MethodNameUtils.getMethodName(method);
            if (methodMap.containsKey(methodName)) {
                throw new SpELFunctionRegisterException("There are several static methods named '{}' in class '{}', It is recommended to declare an alias for the method using the '@FunctionAlias' annotation.", methodName, method.getDeclaringClass().getName()).error(log);
            }
            methodMap.put(methodName, method);
        }

        return StringUtils.hasText(namespace)
                ? Collections.singletonMap(namespace, methodMap)
                : methodMap;
    }
}
