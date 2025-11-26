package com.luckyframework.common;

import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.exception.MapEntryMixTogetherException;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObjectUtils {

    private final static String FIELD_BOUNDARY = "'";
    private final static char SEPARATOR = '.';
    private final static char INDEX_EXP_START = '[';
    private final static char INDEX_EXP_END = ']';


    /**
     * 使用取值表达式对象中获取值
     *
     * @param obj    原对象
     * @param keyExp 取值表达式
     * @return 值对象
     */
    public static Object get(@NonNull Object obj, @NonNull String keyExp) {
        nonNullCheck(obj, keyExp);

        String[] keys = splitKeys(keyExp);
        Object value = obj;
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
     * 尝试使用取值表达式对象中获取值，返回{@link Value}对象
     *
     * @param obj    原对象
     * @param keyExp 取值表达式
     * @return 值对象
     */
    public static Value tryGet(@NonNull Object obj, @NonNull String keyExp) {
        try {
            return Value.of(true, get(obj, keyExp));
        } catch (GetValueException e) {
            return Value.of(false, null);
        }
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
     * 尝试获取对象中指定属性的值或者数组中指定索引的元素，返回{@link Value}对象
     *
     * @param obj 目标对象
     * @param key 指定的属性名/数组下标
     * @return Key 对应的值
     */
    private static Value tryGetValue(Object obj, String key) {
        try {
            return Value.of(true, getValue(obj, key));
        } catch (FieldNotExistException | IllegalArgumentException e) {
            return Value.of(false, null);
        }
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
        if (!ContainerUtils.isIterable(iterator)) {
            throw new IllegalArgumentException("Array expressions cannot be applied to non-iterator objects: Type['" + ClassUtils.getClassName(iterator) + "'], Key['" + index + "']");
        }
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

    /**
     * 使用表达式为对象的某个属性赋值
     *
     * @param obj    对象
     * @param keyExp 表达式
     * @param value  设置的值
     */
    public static void set(@NonNull Object obj, @NonNull String keyExp, Object value) {
        nonNullCheck(obj, keyExp);

        String[] keys = splitKeys(keyExp);
        Object _value = obj;
        int lastIndex = keys.length - 1;

        for (int i = 0; i < lastIndex; i++) {
            Value kValue = tryGetValue(_value, keys[i]);
            if (kValue.isExist() && !kValue.isNull()) {
                _value = kValue.getValue();
            } else {
                Object initValue = initValue(keys[i + 1]);
                setValue(_value, keys[i], initValue, false);
                _value = initValue;
            }
        }
        setValue(_value, keys[lastIndex], value, false);
    }

    /**
     * 根据Key来初始化值
     * <pre>
     *     1.数组的索引表达式 -> 初始化为{@link ArrayList}
     *     2.非数组索引表达式 -> 初始化为{@link LinkedHashMap}
     * </pre>
     *
     * @param key key
     * @return 对应类型的值
     */
    private static Object initValue(String key) {
        if (isIndexExp(key)) {
            Key _key = Key.of(key);
            if (_key.isArrayKey()) {
                return new ArrayList<>();
            }
        }
        return new LinkedHashMap<>();
    }


    /**
     * 使用表达式为对象的某个属性赋值
     *
     * @param obj   对象
     * @param key   表达式
     * @param value 设置的值
     */
    private static void setValue(Object obj, String key, Object value, boolean isRemove) {
        checkKeyExp(obj, key);
        if (isIndexExp(key)) {
            Key _key = Key.of(key);
            if (_key.isArrayKey()) {
                setIteratorElement(obj, _key.getIndex(), value);
            } else if (obj instanceof Map) {
                setMapValue((Map<?, ?>) obj, _key.getField(), value, isRemove);
            } else {
                setPojoValue(obj, _key.getField(), value);
            }
        } else if (obj instanceof Map) {
            setMapValue((Map<?, ?>) obj, key, value, isRemove);
        } else {
            setPojoValue(obj, key, value);
        }
    }

    /**
     * Map值设置
     *
     * @param map   Map对象
     * @param key   待设置的Key
     * @param value 待设置的值
     */
    @SuppressWarnings("all")
    private static void setMapValue(Map map, String key, Object value, boolean isRemove) {
        if (value == null && isRemove) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    /**
     * Pojo对象值设置
     *
     * @param pojo  Pojo对象
     * @param key   待设置的Key
     * @param value 待设置的值
     */
    private static void setPojoValue(Object pojo, String key, Object value) {
        Field field;
        try {
            field = FieldUtils.getField(pojo.getClass(), key);
        } catch (LuckyReflectionException e) {
            throw new FieldNotExistException(pojo.getClass(), key);
        }
        FieldUtils.setValue(pojo, field, value);
    }

    /**
     * 迭代器对象值设置
     *
     * @param iterator 迭代器对象
     * @param index    待设置的Key
     * @param value    待设置的值
     */
    private static void setIteratorElement(Object iterator, int index, Object value) {
        if (!ContainerUtils.isIterable(iterator)) {
            throw new IllegalArgumentException("Array expressions cannot be applied to non-iterator objects: Type['" + ClassUtils.getClassName(iterator) + "'], Key['" + index + "']");
        }
        try {
            ContainerUtils.setIteratorElement(iterator, index, value);
        } catch (IndexOutOfBoundsException e) {
            throw new FieldNotExistException(e);
        }
    }

    //------------------------------------------------------------------------------
    //                              Remove Value
    //------------------------------------------------------------------------------

    /**
     * 移除对象中的某个属性
     *
     * @param obj 对象
     * @param key Key
     * @return 旧属性值
     */
    public static Object remove(Object obj, String key) {
        nonNullCheck(obj, key);
        Value value = tryGet(obj, key);
        if (value.isExist()) {
            setValue(obj, key, null, true);
            return value.getValue();
        } else {
            return null;
        }
    }


    //------------------------------------------------------------------------------
    //                              Weak Fusion Map
    //------------------------------------------------------------------------------


    /**
     * 融合两个Map<br/>
     * 1.inputMap中存在souecMap没有的key时，这些key以及对应的value会被直接put到sourceMap中<br/>
     * 2.当inputMap和sourceMap存在相同的key时<br/>
     * 2.1 key对应的value为简单java类型时(基本类型以及其包装类型)，inputMap的value直接put到sourceMap中<br/>
     * 2.2 除去2.1的情况，两个value的类型不兼容时会抛出异常<br/>
     * 2.3 当value类型为Collection时，input集合会覆盖source集合<br/>
     * 2.4 当value的类型为Map时，会重复上面的流程<br/>
     *
     * @param source 原Map
     * @param input  输入的Map
     */
    @SuppressWarnings("all")
    public static <K, V> void weakFusionMap(Map<K, V> source, Map<K, V> input) {
        if (input == null || input.isEmpty()) {
            return;
        }
        for (Map.Entry<K, V> entry : input.entrySet()) {
            K inputEntryKey = entry.getKey();
            V inputEntryValue = entry.getValue();
            //当前key在source中不存在，直接put
            if (!source.containsKey(inputEntryKey)) {
                source.put(inputEntryKey, inputEntryValue);
            } else {
                V sourceEntryValue = source.get(inputEntryKey);
                //inputEntry == null ,不做任何操作
                if (inputEntryValue == null) {
                    continue;
                }
                //sourceEntry == null ,直接用inputEntryValue替换掉
                if (sourceEntryValue == null) {
                    source.put(inputEntryKey, inputEntryValue);
                    continue;
                }

                Class<?> sourceEntryClass = sourceEntryValue.getClass();
                Class<?> inputEntryClass = inputEntryValue.getClass();

                // 两个都是简单基本类型，则使用inputEntrytValue替代sourceEntryValue
                if (ClassUtils.isSimpleBaseType(inputEntryClass) && ClassUtils.isSimpleBaseType(sourceEntryClass)) {
                    source.put(inputEntryKey, inputEntryValue);
                    continue;
                }

                //类型不兼容
                if (!sourceEntryClass.isAssignableFrom(inputEntryClass) && !inputEntryClass.isAssignableFrom(sourceEntryClass)) {
                    throw new MapEntryMixTogetherException("Map元素融合异常，'" + inputEntryKey + "' => sourceEntry:[" + sourceEntryClass.getName() + "] , inputEntry:[" + inputEntryClass.getName() + "]");
                }

                //两个都是Map
                if ((sourceEntryValue instanceof Map) && (inputEntryValue instanceof Map)) {
                    weakFusionMap((Map) sourceEntryValue, (Map) inputEntryValue);
                }
                //两个都是集合，则用将input集合替代source集合
                else if ((sourceEntryValue instanceof Collection) && (inputEntryValue instanceof Collection)) {
                    source.put(inputEntryKey, (V) inputEntryValue);
                } else {
                    source.put(inputEntryKey, inputEntryValue);
                }
            }
        }
    }

    /**
     * 非空校验
     *
     * @param obj 对象
     * @param key Key
     */
    private static void nonNullCheck(Object obj, String key) {
        Assert.notNull(obj, "source object is null");
        Assert.notNull(key, "keyExp is null");
    }

    /**
     * 表达式校验
     *
     * @param obj 对象
     * @param key Key
     */
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
         *     [0]
         *     ['name']
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

        public boolean isNull() {
            return value == null;
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

    //------------------------------------------------------------------------------
    //                                  Test
    //------------------------------------------------------------------------------

    public static void main(String[] args) {
//        test1();
//        test2();
        test3();
    }

    private static void test1() {
        Map<String, Object> map = new HashMap<>();
        map.put("array", Arrays.asList(1, 2, 3, 4));
        map.put("string", "Hello String");
        map.put("map", Collections.singletonMap("key", "MapValue"));
        map.put("listMap", Arrays.asList(Collections.singletonMap("m1", "LM-1"), Collections.singletonMap("m2", "LM-2")));

        System.out.println(map);
        System.out.println(get(map, "listMap[1]['m2']"));
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

    public static void test3() {
        Map<String, Object> map = new HashMap<>();
        set(map, "lucky.datasource.defaultDB.url", "jdbc.url");
        set(map, "lucky.datasource.defaultDB.driver", "jdbc.driver");
        set(map, "lucky.datasource.defaultDB.root", "root");
        set(map, "lucky.datasource.defaultDB.password", "123456");
        set(map, "lucky.datasource.defaultDB['max.age']", 13);
        set(map, "lucky.import[0]", "data.json");
        set(map, "lucky.import[1]", "source.properties");
        set(map, "lucky.import[2][0]", "1-test");
        set(map, "lucky.import[2][1]", "2-test");
        set(map, "lucky.import[3][1]", "2-test");

        set(map, "$[0].id", 1);
        set(map, "$[0].name", "邪恶小绵羊");
        set(map, "$[1].id", 2);
        set(map, "$[1].name", "啵里啵气大菠菜");
        set(map, "$[2].id", 3);
        set(map, "$[2].name", "NAME-3");
        String[] strArr = {"one", "two"};
        set(map, "lucky.import[2][2]", strArr);
        set(map, "lucky.import[2][2][1]", "three");

        System.out.println(map);
        System.out.println(get(map, "['lucky']['datasource']['defaultDB']['max.age']"));

        Object importList = get(map, "lucky.import");
        Object o = get(importList, "[2][2]");
        System.out.println(o);
    }
}
