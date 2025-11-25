package com.luckyframework.common;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;

import java.util.Map;
import java.util.regex.Pattern;

public class MapUtilsV2 {
    private final static Pattern ARRAY_IDENTIFICATION_PATTERN = Pattern.compile("^[\\S\\s]+?\\[[0-9]\\d*]$");
    private final static String ARRAY_INDEX = "\\[[0-9]\\d*]";
    private final static Pattern ARRAY_INDEX_PATTERN = Pattern.compile(ARRAY_INDEX);
    private final static String SEPARATOR = ".";
    private final static String FIELD_SEPARATOR = "'";
    private final static String ARRAY_INDEX_START = "[";
    private final static String ARRAY_INDEX_END = "]";


    /**
     * 判断一个Key的最后一个结构是否为数组
     *
     * @param key 带判断的Key
     * @return Key的最后一个结构是否为数组
     */
    private static boolean lastIsArray(String key) {
        return ARRAY_IDENTIFICATION_PATTERN.matcher(key).matches();
    }

    /**
     * 将长Key拆分为单独的Key
     *
     * @param keys 待拆分的长Key
     * @return 单独的Key
     */
    private String[] splitKeys(String keys) {
        String[] keysArray = keys.split(SEPARATOR, -1);
        for (String key : keysArray) {
            if (!StringUtils.hasText(key)) {
                throw new IllegalArgumentException("Invalid key expression: " + keys);
            }
        }
        return keysArray;
    }

    private static Object getValue(Object obj, String key) {
        // null
        if (obj == null) {
            throw new IllegalArgumentException("Expressions cannot operate on null values：Type['null'], Key['" + key + "']");
        }
        // 基本类型
        if (ClassUtils.isSimpleBaseType(obj.getClass())) {
            throw new IllegalArgumentException("Expressions cannot operate on basic types：Type['" + ClassUtils.getClassName(obj) + "'], Key['" + key + "']");
        }
        if (obj instanceof Map) {
            return getMapValue((Map<?, ?>) obj, key);
        }
        if (ContainerUtils.isIterable(obj)) {

        }
        return getPojoValue(obj, key);
    }


    private static Object getMapValue(Map<?, ?> map, String key) {
        return map.get(key);
    }

    private static Object getArrayValue(Object obj, int index) {
        return ContainerUtils.getIteratorElement(obj, index);
    }

    public static Object getPojoValue(Object obj, String key) {
        return FieldUtils.getValue(obj, key);
    }


    /**
     * 检验key和Obj是否匹配
     *
     * @param obj 值
     * @param key 取值表达式
     */
    private static void checkKey(Object obj, String key) {
        // null
        if (obj == null) {
            throw new IllegalArgumentException("Expressions cannot operate on null values：Type['null'], Key['" + key + "']");
        }
        // key为Array但是Obj不是迭代器类型
        if (lastIsArray(key) && !ContainerUtils.isIterable(obj)) {
            throw new IllegalArgumentException("Non-iterator type values cannot be manipulated through iterator expressions：Type['" + ClassUtils.getClassName(obj) + "'], Key['" + key + "']");
        }
        // 基本类型
        if (ClassUtils.isSimpleBaseType(obj.getClass())) {
            throw new IllegalArgumentException("Expressions cannot operate on basic types：Type['" + ClassUtils.getClassName(obj) + "'], Key['" + key + "']");
        }
    }


    /**
     * 数组信息
     */
    static class Key {
        /**
         * 名称
         */
        private final String name;
        /**
         * 属性
         */
        private final String field;

        /**
         * 数组元素位置
         */
        private Integer index;

        /**
         * 私有构造函数
         *
         * @param name  名称
         * @param field 属性
         */
        private Key(String name, String field) {
            this.name = name;
            if (field.startsWith(FIELD_SEPARATOR) && field.endsWith(FIELD_SEPARATOR)) {
                this.field = field.substring(1, field.length() - 1);
            } else {
                this.field = field;
                this.index = Integer.parseInt(field);
            }

        }

        /**
         * 将一个表达式解析为一个{@link Key}实例
         * <pre>
         *     eg:
         *     array[0]
         *     obj['name']
         * </pre>
         *
         * @param exp 数组表达式
         * @return Key实例
         */
        public static Key of(String exp) {
            int start = exp.indexOf(ARRAY_INDEX_START);
            int end = exp.lastIndexOf(ARRAY_INDEX_END);
            if (start == -1 || end == -1 || end <= start) {
                throw new IllegalArgumentException("Invalid array index expression: " + exp);
            }
            try {
                String name = exp.substring(0, start);
                String field = exp.substring(start + ARRAY_INDEX_START.length(), end);
                return new Key(name, field);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid array index expression: " + exp);
            }
        }

        public String getName() {
            return name;
        }

        public String getField() {
            return field;
        }

        public Integer getIndex() {
            return index;
        }

        public boolean isArrayKey() {
            return index != null;
        }
    }
}
