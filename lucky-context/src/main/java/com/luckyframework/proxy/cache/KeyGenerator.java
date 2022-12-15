package com.luckyframework.proxy.cache;

import java.lang.reflect.Method;

/**
 * 缓存Key生成策略
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 15:07
 */
@FunctionalInterface
public interface KeyGenerator {

    Object generate(Object target, Method method, Object... params);
}
