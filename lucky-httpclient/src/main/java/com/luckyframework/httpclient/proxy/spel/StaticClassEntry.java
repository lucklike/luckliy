package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
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
public class StaticClassEntry {


    private static final Logger log = LoggerFactory.getLogger(StaticClassEntry.class);

    /**
     * 工具类Class
     */
    private Class<?> clazz;

    /**
     * 方法名前缀
     */
    private String prefix;

    public static StaticClassEntry create(String prefix, Class<?> clazz) {
        StaticClassEntry entry = new StaticClassEntry();
        if (!StringUtils.hasText(prefix)) {
            FunctionPrefix prefixAnn = AnnotationUtils.findMergedAnnotation(clazz, FunctionPrefix.class);
            if (prefixAnn != null && StringUtils.hasText(prefixAnn.prefix())) {
                prefix = prefixAnn.prefix();
            }
        }
        entry.setPrefix(prefix);
        entry.setClazz(clazz);
        return entry;
    }

    public static StaticClassEntry create(Class<?> clazz) {
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
     * 获取方法名前缀
     *
     * @return 方法名前缀
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 设置方法名前缀
     *
     * @param prefix 方法名前缀
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
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
            if (!ClassUtils.isPublicMethod(method)) {
                continue;
            }
            if (AnnotationUtils.isAnnotated(method, FunctionFilter.class)) {
                continue;
            }

            String methodName = getMethodName(method);
            if (methodMap.containsKey(methodName)) {
                throw new SpELFunctionRegisterException("There are several static methods named '{}' in class '{}', It is recommended to declare an alias for the method using the '@FunctionAlias' annotation.", methodName, method.getDeclaringClass().getName())
                        .printException(log);
            }
            methodMap.put(methodName, method);
        }

        return methodMap;
    }

    /**
     * 获取方法名称
     *
     * @param method 方法实例
     * @return 方法名称
     */
    private String getMethodName(Method method) {
        String methodName = FunctionAlias.MethodNameUtils.getMethodName(method);
        return StringUtils.hasText(prefix) ? prefix + methodName : methodName;
    }
}
