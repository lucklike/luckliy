package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * 静态方法实体
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 03:34
 */
public class StaticMethodEntry {

    private static final Logger log = LoggerFactory.getLogger(StaticMethodEntry.class);

    /**
     * 方法别名
     */
    private String alias;

    /**
     * 方法所在的Class
     */
    private Class<?> clazz;

    /**
     * 方法名
     */
    private String method;

    /**
     * 方法参数列表类型数组
     */
    private Class<?>[] paramTypes;

    public static StaticMethodEntry create(String alias, Class<?> clazz, String methodName, Class<?> ... paramTypes) {
        StaticMethodEntry entry = new StaticMethodEntry();
        entry.setAlias(alias);
        entry.setClazz(clazz);
        entry.setMethod(methodName);
        entry.setParamTypes(paramTypes);
        return entry;
    }

    public static StaticMethodEntry create(Class<?> clazz, String methodName, Class<?> ... paramTypes) {
        return create(null, clazz, methodName, paramTypes);
    }

    /**
     * 获取方法别名
     * @return 方法别名
     */
    public String getAlias() {
        return alias;
    }

    /**
     * 设置方法别名
     * @param alias 方法别名
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * 获取方法所在的Class实例
     * @return 方法所在的Class实例
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * 设置方法所在的Class实例
     * @param clazz 方法所在的Class实例
     */
    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * 获取方法名称
     * @return 方法名称
     */
    public String getMethod() {
        return method;
    }

    /**
     * 设置方法名称
     * @param method 方法名称
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * 获取方法参数列表类型数组
     * @return  方法参数列表类型数组
     */
    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    /**
     * 设置方法参数列表类型数组
     * @param paramTypes 方法参数列表类型数组
     */
    public void setParamTypes(Class<?>[] paramTypes) {
        this.paramTypes = paramTypes;
    }

    /**
     * 必要参数的校验
     * 1.clazz不可为null
     * 2.method不可为null
     */
    public void check() {
        Assert.notNull(this.clazz, "Class must not be null");
        Assert.notNull(this.method, "Method must not be null");
    }

    /**
     * 获取方式实例
     * @return 方式实例
     */
    public Method getMethodInstance() {
        try {
            check();
            Method declaredMethod = MethodUtils.getDeclaredMethod(clazz, method, paramTypes);
            if (ClassUtils.isStaticMethod(declaredMethod) && ClassUtils.isPublicMethod(declaredMethod)) {
                return declaredMethod;
            }
            throw new SpELFunctionRegisterException("Registered SpEL functions must be public static. method: {}", method);
        }catch (LuckyReflectionException e) {
            throw new SpELFunctionRegisterException("SpEL function registration exception. ", e).printException(log);
        }
    }

    /**
     * 根据方法实例获取方法名称
     * @param method 方法实例
     * @return 方法名称
     */
    public String getName(Method method) {
        if (StringUtils.hasText(this.alias)) {
            return this.alias;
        }
        return FunctionAlias.MethodNameUtils.getMethodName(method);
    }
}
