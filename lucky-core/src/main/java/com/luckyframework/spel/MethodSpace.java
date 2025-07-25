package com.luckyframework.spel;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 方法空间
 */
public class MethodSpace extends LinkedHashMap<String, Method> {

    public MethodSpace(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public MethodSpace(int initialCapacity) {
        super(initialCapacity);
    }

    public MethodSpace() {
    }

    public MethodSpace(Map<? extends String, ? extends Method> m) {
        super(m);
    }

    public MethodSpace(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }
}
