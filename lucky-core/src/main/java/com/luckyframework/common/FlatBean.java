package com.luckyframework.common;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

/**
 * 支持扁平化操作的 Bean 对象
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/27 01:18
 */
@SuppressWarnings("unchecked")
public class FlatBean<T> {
    // 空实例
    public static final FlatBean<?> NULL = new FlatBean<>(null);

    private final T bean;

    private FlatBean(T bean) {
        this.bean = bean;
    }

    public static <T> FlatBean<T> of(T bean) {
        return new FlatBean<>(bean);
    }

    public static FlatBean<?> forProperties(Properties properties) {
        Enumeration<?> enumeration = properties.propertyNames();
        FlatBean<?> flatBean = null;
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            if (flatBean == null) {
                if (ObjectUtils.firstIsArrayKey(key)) {
                    flatBean = FlatBean.of(new ArrayList<>());
                } else {
                    flatBean = FlatBean.of(new LinkedHashMap<>());
                }
            }
            flatBean.set(key, properties.getProperty(key));
        }
        return flatBean == null ? FlatBean.NULL : flatBean;
    }

    public T getBean() {
        return bean;
    }

    public void set(String key, Object value) {
        ObjectUtils.set(bean, key, value);
    }

    //------------------------------------------------------------------------
    //                              to
    //------------------------------------------------------------------------

    public <R> R to(Type type) {
        return ConversionUtils.conversion(this.bean, type);
    }

    public <R> R to(ResolvableType type) {
        return to(type.getType());
    }

    public <R> R to(SerializationTypeToken<R> typeToke) {
        return to(typeToke.getType());
    }

    public <R> R to(Class<R> clazz) {
        return to((Type) clazz);
    }

    //------------------------------------------------------------------------
    //                              get
    //------------------------------------------------------------------------

    public Object get(String key) {
        return ObjectUtils.get(bean, key);
    }

    public <V> V get(String key, Type type) {
        return ConversionUtils.conversion(get(key), type);
    }

    public <V> V get(String key, ResolvableType type) {
        return get(key, type.getType());
    }

    public <V> V get(String key, SerializationTypeToken<V> typeToken) {
        return get(key, typeToken.getType());
    }

    public <V> V get(String key, Class<V> clazz) {
        return get(key, (Type) clazz);
    }

    public <E> List<E> getList(String key, Class<E> elementClass) {
        return get(key, ResolvableType.forClassWithGenerics(List.class, elementClass));
    }

    public List<?> getList(String key) {
        return getList(key, Object.class);
    }

    //------------------------------------------------------------------------
    //                            Basic Types
    //------------------------------------------------------------------------

    public String getString(String key) {
        return get(key, String.class);
    }

    public int getInt(String key) {
        return get(key, int.class);
    }

    public long getLong(String key) {
        return get(key, long.class);
    }

    public double getDouble(String key) {
        return get(key, double.class);
    }

    public boolean getBoolean(String key) {
        return get(key, boolean.class);
    }

    public float getFloat(String key) {
        return get(key, float.class);
    }

    public short getShort(String key) {
        return get(key, short.class);
    }

    public byte getByte(String key) {
        return get(key, byte.class);
    }

    public char getChar(String key) {
        return get(key, char.class);
    }

    //------------------------------------------------------------------------
    //                            Basic Types Array
    //------------------------------------------------------------------------

    public String[] getStringArray(String key) {
        return get(key, String[].class);
    }

    public int[] getIntArray(String key) {
        return get(key, int[].class);
    }

    public long[] getLongArray(String key) {
        return get(key, long[].class);
    }

    public double[] getDoubleArray(String key) {
        return get(key, double[].class);
    }

    public boolean[] getBooleanArray(String key) {
        return get(key, boolean[].class);
    }

    public float[] getFloatArray(String key) {
        return get(key, float[].class);
    }

    public short[] getShortArray(String key) {
        return get(key, short[].class);
    }

    public byte[] getByteArray(String key) {
        return get(key, byte[].class);
    }

    public char[] getCharArray(String key) {
        return get(key, char[].class);
    }

    //------------------------------------------------------------------------
    //                            Basic Types List
    //------------------------------------------------------------------------

    public List<String> getStringList(String key) {
        return getList(key, String.class);
    }

    public List<Integer> getIntList(String key) {
        return getList(key, Integer.class);
    }

    public List<Long> getLongList(String key) {
        return getList(key, Long.class);
    }

    public List<Double> getDoubleList(String key) {
        return getList(key, Double.class);
    }

    public List<Boolean> getBooleanList(String key) {
        return getList(key, Boolean.class);
    }

    public List<Float> getFloatList(String key) {
        return getList(key, Float.class);
    }

    public List<Short> getShortList(String key) {
        return getList(key, Short.class);
    }

    public List<Byte> getByteList(String key) {
        return getList(key, Byte.class);
    }

    public List<Character> getCharList(String key) {
        return getList(key, Character.class);
    }

    //------------------------------------------------------------------------
    //                             Get FlatBean
    //------------------------------------------------------------------------

    public FlatBean<?> getFlatBean(String key) {
        return FlatBean.of(ObjectUtils.get(bean, key));
    }

    public <V> FlatBean<V> getFlatBean(String key, Type type) {
        return FlatBean.of(get(key, type));
    }

    public <V> FlatBean<V> getFlatBean(String key, ResolvableType type) {
        return getFlatBean(key, type.getType());
    }

    public <V> FlatBean<V> getFlatBean(String key, SerializationTypeToken<V> typeToken) {
        return getFlatBean(key, typeToken.getType());
    }

    public <V> FlatBean<V> getFlatBean(String key, Class<V> clazz) {
        return getFlatBean(key, (Type) clazz);
    }


    //------------------------------------------------------------------------
    //                            Try Get
    //------------------------------------------------------------------------

    public TryValue<?> tryGet(String key) {
        return ObjectUtils.tryGet(bean, key);
    }

    public <V> TryValue<V> tryGet(String key, Type type) {
        TryValue<?> tryValue = tryGet(key);
        if (tryValue.isExist()) {
            return TryValue.of(true, ConversionUtils.conversion(tryValue.getValue(), type));
        }
        return (TryValue<V>) tryValue;
    }

    public <V> TryValue<V> tryGet(String key, ResolvableType type) {
        return tryGet(key, type.getType());
    }

    public <V> TryValue<V> tryGet(String key, SerializationTypeToken<V> typeToken) {
        return tryGet(key, typeToken.getType());
    }

    public <V> TryValue<V> tryGet(String key, Class<V> clazz) {
        return tryGet(key, (Type) clazz);
    }

    public <E> TryValue<List<E>> tryGetList(String key, Class<E> elementClass) {
        return tryGet(key, ResolvableType.forClassWithGenerics(List.class, elementClass));
    }

    public TryValue<List<Object>> tryGetList(String key) {
        return tryGetList(key, Object.class);
    }

    //------------------------------------------------------------------------
    //                       Basic Types Try Get
    //------------------------------------------------------------------------

    public TryValue<String> tryGetString(String key) {
        return tryGet(key, String.class);
    }

    public TryValue<Integer> tryGetInt(String key) {
        return tryGet(key, Integer.class);
    }

    public TryValue<Long> tryGetLong(String key) {
        return tryGet(key, Long.class);
    }

    public TryValue<Double> tryGetDouble(String key) {
        return tryGet(key, Double.class);
    }

    public TryValue<Boolean> tryGetBoolean(String key) {
        return tryGet(key, Boolean.class);
    }

    public TryValue<Float> tryGetFloat(String key) {
        return tryGet(key, Float.class);
    }

    public TryValue<Short> tryGetShort(String key) {
        return tryGet(key, Short.class);
    }

    public TryValue<Byte> tryGetByte(String key) {
        return tryGet(key, Byte.class);
    }

    public TryValue<Character> tryGetChar(String key) {
        return tryGet(key, Character.class);
    }

    //------------------------------------------------------------------------
    //                      Basic Types Array Try Get
    //------------------------------------------------------------------------

    public TryValue<String[]> tryGetStringArray(String key) {
        return tryGet(key, String[].class);
    }

    public TryValue<Integer[]> tryGetIntArray(String key) {
        return tryGet(key, Integer[].class);
    }

    public TryValue<Long[]> tryGetLongArray(String key) {
        return tryGet(key, Long[].class);
    }

    public TryValue<Double[]> tryGetDoubleArray(String key) {
        return tryGet(key, Double[].class);
    }

    public TryValue<Boolean[]> tryGetBooleanArray(String key) {
        return tryGet(key, Boolean[].class);
    }

    public TryValue<Float[]> tryGetFloatArray(String key) {
        return tryGet(key, Float[].class);
    }

    public TryValue<Short[]> tryGetShortArray(String key) {
        return tryGet(key, Short[].class);
    }

    public TryValue<Byte[]> tryGetByteArray(String key) {
        return tryGet(key, Byte[].class);
    }

    public TryValue<Character[]> tryGetCharArray(String key) {
        return tryGet(key, Character[].class);
    }

    //------------------------------------------------------------------------
    //                      Basic Types Array Try Get
    //------------------------------------------------------------------------

    public TryValue<List<String>> tryGetStringList(String key) {
        return tryGetList(key, String.class);
    }

    public TryValue<List<Integer>> tryGetIntList(String key) {
        return tryGetList(key, Integer.class);
    }

    public TryValue<List<Long>> tryGetLongList(String key) {
        return tryGetList(key, Long.class);
    }

    public TryValue<List<Double>> tryGetDoubleList(String key) {
        return tryGetList(key, Double.class);
    }

    public TryValue<List<Boolean>> tryGetBooleanList(String key) {
        return tryGetList(key, Boolean.class);
    }

    public TryValue<List<Float>> tryGetFloatList(String key) {
        return tryGetList(key, Float.class);
    }

    public TryValue<List<Short>> tryGetShortList(String key) {
        return tryGetList(key, Short.class);
    }

    public TryValue<List<Byte>> tryGetByteList(String key) {
        return tryGetList(key, Byte.class);
    }

    public TryValue<List<Character>> tryGetCharList(String key) {
        return tryGetList(key, Character.class);
    }
}
