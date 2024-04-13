package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 静态工具类实例
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:21
 */
public class StaticClassEntry {


    private static final Logger log = LoggerFactory.getLogger(StaticClassEntry.class);

    private Class<?> clazz;
    private String prefix;

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Map<String, Object> getAllStaticMethods() {
        Assert.notNull(clazz, "clazz cannot be null");
        List<Method> allStaticMethod = ClassUtils.getAllStaticMethod(clazz);

        Map<String, Object> methodMap = new HashMap<>();
        for (Method method : allStaticMethod) {
            String methodName = getMethodName(method);
            if (methodMap.containsKey(methodName)) {
                throw new SpELFunctionRegisterException("If there are several static methods named '{}' in the same class, it is recommended to use the '@StaticMethodAlias' annotation to define an alias for the method with the same name.", methodName)
                        .printException(log);
            }
            methodMap.put(methodName, method);
        }

        return methodMap;
    }

    private String getMethodName(Method method) {
        String methodName = StaticMethodAlias.MethodNameUtils.getMethodName(method);
        return StringUtils.hasText(prefix) ? prefix + methodName : methodName;
    }
}
