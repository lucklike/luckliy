package com.luckyframework.common;

import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapUtilsV2 {
    private final static String FIELD_BOUNDARY = "'";
    private final static char SEPARATOR = '.';
    private final static char INDEX_EXP_START = '[';
    private final static char INDEX_EXP_END = ']';


    /**
     * 使用取值表达式从 Map 中取值
     *
     * @param map    Map 对象
     * @param keyExp 取值表达式
     * @return 值对象
     */
    public static Object get(@NonNull Map<?, ?> map, @NonNull String keyExp) {
        Assert.notNull(map, "source map is null");
        Assert.notNull(keyExp, "keyExp is null");

        String[] keys = splitKeys(keyExp);
        Object value = map;
        StringBuilder expSb = new StringBuilder();
        for (String key : keys) {
            expSb.append(key);
            try {
                value = getValue(value, key);
            } catch (FieldNotExistException | IllegalArgumentException e) {
                throw new GetValueException("The value cannot be retrieved from the original data through the specified Key: '" + expSb + "'", e);
            }
        }
        return value;
    }

    /**
     * 尝试使用取值表达式从 Map 中取值，返回{@link Value}对象
     *
     * @param map    Map 对象
     * @param keyExp 取值表达式
     * @return 值对象
     */
    public static Value tryGet(@NonNull Map<?, ?> map, @NonNull String keyExp) {
        try {
            return Value.of(true, get(map, keyExp));
        } catch (GetValueException e) {
            return Value.of(false, null);
        }
    }

    public static void set(@NonNull Map<?, ?> map, @NonNull String keyExp, Object value) {
        Assert.notNull(map, "source map is null");
        Assert.notNull(keyExp, "keyExp is null");

        String[] keys = splitKeys(keyExp);

    }

    /**
     * 是否为索引取值表达式
     * <pre>
     *     eg:
     *      array[0]
     *      map['name']
     * </pre>
     *
     * @param exp 待判断的 Key
     * @return 是否为索引取值表达式
     */
    private static boolean isIndexExp(String exp) {
        int start = exp.indexOf(INDEX_EXP_START);
        int end = exp.lastIndexOf(INDEX_EXP_END);
        return start != -1 && end != -1 && end > start;
    }

    /**
     * 将长Key拆分为单独的Key
     *
     * @param keys 待拆分的长Key
     * @return 单独的Key
     */
    private static String[] splitKeys(String keys) {
        if (keys == null || keys.isEmpty()) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBrackets = false;
        int bracketDepth = 0;

        for (int i = 0; i < keys.length(); i++) {
            char c = keys.charAt(i);

            if (c == INDEX_EXP_START) {
                // 如果不在中括号内，且当前有内容，先保存当前内容
                if (!inBrackets && current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                bracketDepth++;
                inBrackets = true;
                current.append(c);
            } else if (c == INDEX_EXP_END) {
                current.append(c);
                bracketDepth--;
                if (bracketDepth == 0) {
                    // 中括号结束，保存当前中括号表达式
                    result.add(current.toString());
                    current.setLength(0);
                    inBrackets = false;
                }
            } else if (c == SEPARATOR && !inBrackets) {
                // 不在中括号内遇到点号，如果当前有内容则保存
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        // 处理最后剩余的内容
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result.toArray(new String[0]);

    }

    //------------------------------------------------------------------------------
    //                              Get Value
    //------------------------------------------------------------------------------

    /**
     * 获取对象中指定属性的值或者数组中指定索引的元素
     *
     * @param obj 目标对象
     * @param key 指定的属性名/数组下标
     * @return Key 对应的值
     */
    private static Object getValue(Object obj, String key) {
        checkKeyExp(obj, key);

        // 索引取值表达式
        if (isIndexExp(key)) {
            Key iKey = Key.of(key);
            // 数组取值表达式s
            if (iKey.isArrayKey()) {
                if (!ContainerUtils.isIterable(obj)) {
                    throw new IllegalArgumentException("Array expressions cannot be applied to non-iterator objects: Type['" + ClassUtils.getClassName(obj) + "'], Key['" + key + "']");
                }
                return getIteratorValue(obj, iKey.getIndex());
            }
            // Map
            if (obj instanceof Map) {
                return getMapValue((Map<?, ?>) obj, iKey.getField());
            }

            //对象
            return getPojoValue(obj, iKey.getField());
        }

        // Map
        if (obj instanceof Map) {
            return getMapValue((Map<?, ?>) obj, key);
        }

        // Pojo
        return getPojoValue(obj, key);
    }


    /**
     * 从 Map 中获取指定 Key 的值
     *
     * @param map Map 对象
     * @param key Key
     * @return Key 对应的值
     */
    private static Object getMapValue(Map<?, ?> map, String key) {
        if (!map.containsKey(key)) {
            throw new FieldNotExistException(Map.class, key);
        }
        return map.get(key);
    }

    /**
     * 从迭代器中获取指定索引位置的元素值
     *
     * @param iterator 迭代器对对象
     * @param index    索引
     * @return 指定索引对应的元素值
     */
    private static Object getIteratorValue(Object iterator, int index) {
        try {
            return ContainerUtils.getIteratorElement(iterator, index);
        } catch (IndexOutOfBoundsException e) {
            throw new FieldNotExistException(e);
        }
    }

    /**
     * 从自定义的 Pojo 类型对象中取值
     *
     * @param pojo Pojo 对象
     * @param key  对象属性名称
     * @return 对应的值
     */
    public static Object getPojoValue(Object pojo, String key) {
        Field field;
        try {
            field = FieldUtils.getField(pojo.getClass(), key);
        } catch (LuckyReflectionException e) {
            throw new FieldNotExistException(pojo.getClass(), key);
        }
        return FieldUtils.getValue(pojo, field);
    }

    //------------------------------------------------------------------------------
    //                              Set Value
    //------------------------------------------------------------------------------

    private static void setValue(Object obj, String key, Object value) {
        checkKeyExp(obj, key);
    }

    private static void setMapValue(Map<?, ?> map, String key, Object value) {

    }


    private static void checkKeyExp(Object obj, String key) {
        // null
        if (obj == null) {
            throw new IllegalArgumentException("Expressions cannot operate on null values：Type['null'], Key['" + key + "']");
        }
        // 基本类型
        if (ClassUtils.isSimpleBaseType(obj.getClass())) {
            throw new IllegalArgumentException("Expressions cannot operate on basic types: Type['" + ClassUtils.getClassName(obj) + "'], Key['" + key + "']");
        }
    }

    //------------------------------------------------------------------------------
    //                              Inner Class
    //------------------------------------------------------------------------------

    /**
     * Key信息
     */
    static class Key {
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
         * @param field 属性
         */
        private Key(String field) {
            if (field.startsWith(FIELD_BOUNDARY) && field.endsWith(FIELD_BOUNDARY)) {
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
            int start = exp.indexOf(INDEX_EXP_START);
            int end = exp.lastIndexOf(INDEX_EXP_END);
            if (start == -1 || end == -1 || end <= start) {
                throw new IllegalArgumentException("Invalid array index expression: " + exp);
            }
            try {
                String field = exp.substring(start + 1, end);
                return new Key(field);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid array index expression: '" + exp + "'");
            }
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

    /**
     * 值对象
     */
    public static class Value {
        private final boolean exist;
        private final Object value;

        private Value(boolean exist, Object value) {
            this.exist = exist;
            this.value = value;
        }

        public static Value of(boolean exist, Object value) {
            return new Value(exist, value);
        }

        public boolean isExist() {
            return exist;
        }

        public Object getValue() {
            return value;
        }
    }

    /**
     * 属性不存在异常
     */
    public static class FieldNotExistException extends RuntimeException {
        public FieldNotExistException(Class<?> clazz, String field) {
            super("Field ['" + field + "'] of type [" + clazz.getName() + "] is not exists!");
        }

        public FieldNotExistException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * 取值异常
     */
    public static class GetValueException extends RuntimeException {
        public GetValueException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 值设置异常
     */
    public static class SetValueException extends RuntimeException {
        public SetValueException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void main(String[] args) {
        test1();
    }

    private static void test1() {
        Map<String, Object> map = new HashMap<>();
        map.put("array", Arrays.asList(1, 2, 3, 4));
        map.put("string", "Hello String");
        map.put("map", Collections.singletonMap("key", "MapValue"));
        map.put("listMap", Arrays.asList(Collections.singletonMap("m1", "LM-1"), Collections.singletonMap("m2", "LM-2")));

        System.out.println(map);
        System.out.println(get(map, "listMap[8]['m']"));
        System.out.println(get(map, "listMap[0].m1"));
        System.out.println(get(map, "array"));
        System.out.println(get(map, "array[2]"));
    }

    public static void test2() {
        // 测试用例
        String[] testCases = {
                "object.user.name",
                "object.map['user.name']",
                "object.map['array'][9]",
                "listMap[1]['m2']",
                "a.b.c",
                "object",
                "object[1].property",
                "object.map['test1']['test2'].property",
                "single",
                "array[0][1][2]",
                "obj.prop1[0].prop2['key']"
        };

        for (String testCase : testCases) {
            String[] result = splitKeys(testCase);
            System.out.println("输入: \"" + testCase + "\"");
            System.out.println("输出: " + java.util.Arrays.toString(result));
            System.out.println();
        }
    }
}
