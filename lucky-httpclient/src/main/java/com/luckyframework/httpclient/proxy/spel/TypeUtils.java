package com.luckyframework.httpclient.proxy.spel;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2025/8/19 02:12
 */
public class TypeUtils {

    private static final Map<String, Class<?>> typeMap = new LinkedHashMap<>();

    static {
        addBaseAndArrayType(int.class);
        addBaseAndArrayType(long.class);
        addBaseAndArrayType(double.class);
        addBaseAndArrayType(boolean.class);
        addBaseAndArrayType(char.class);
        addBaseAndArrayType(byte.class);
        addBaseAndArrayType(short.class);
        addBaseAndArrayType(float.class);

        addBaseAndArrayType(Integer.class);
        addBaseAndArrayType(Long.class);
        addBaseAndArrayType(Double.class);
        addBaseAndArrayType(Boolean.class);
        addBaseAndArrayType(Character.class);
        addBaseAndArrayType(Byte.class);
        addBaseAndArrayType(Short.class);
        addBaseAndArrayType(Float.class);

        addBaseAndArrayType(String.class);

        addType(Collection.class);
        addType(List.class);
        addType(Map.class);
        addType(Set.class);
    }

    public static Map<String, Class<?>> getTypeMap() {
        return Collections.unmodifiableMap(typeMap);
    }

    public static void addType(String name, Class<?> clazz) {
        name = clazz.isArray() ?  name + "__" : name;
        if (!typeMap.containsKey(name)) {
            typeMap.put(name, clazz);
        }
    }

    public static void addBaseAndArrayType(String name, Class<?> clazz) {
        addType(name, clazz);
        addType(name, Array.newInstance(clazz, 0).getClass());
    }

    public static void addBaseAndArrayType(Class<?> clazz) {
        addBaseAndArrayType(clazz.getSimpleName(), clazz);
    }

    public static void addType(Class<?> clazz) {
        addType(clazz.getSimpleName(), clazz);
    }
}
