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

    private String alias;
    private Class<?> clazz;
    private String method;
    private Class<?>[] paramTypes;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(Class<?>[] paramTypes) {
        this.paramTypes = paramTypes;
    }

    public void check() {
        Assert.notNull(this.clazz, "Class must not be null");
        Assert.notNull(this.method, "Method must not be null");
    }

    public Method getMethodInstance() {
        try {
            check();
            Method declaredMethod = MethodUtils.getDeclaredMethod(clazz, method, paramTypes);
            if (ClassUtils.isStaticMethod(declaredMethod)) {
                return declaredMethod;
            }
            throw new SpELFunctionRegisterException("Registered SpEL functions must be static. method: {}", method);
        }catch (LuckyReflectionException e) {
            throw new SpELFunctionRegisterException("SpEL function registration exception. ", e).printException(log);
        }
    }

    public String getName(Method method) {
        if (StringUtils.hasText(this.alias)) {
            return this.alias;
        }
        return StaticMethodAlias.MethodNameUtils.getMethodName(method);
    }
}
