package com.luckyframework.common;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 支持扁平化操作的 Bean 对象
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/27 01:18
 */
@SuppressWarnings("unchecked")
public class FlatBean<T> {

    private final T bean;

    private FlatBean(T bean) {
        this.bean = bean;
    }

    public static <T> FlatBean<T> of(T bean) {
        return new FlatBean<>(bean);
    }

    public T getBean() {
        return bean;
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
        return get(key, new SerializationTypeToken<List<String>>() {});
    }

    public List<Integer> getIntList(String key) {
        return get(key, new SerializationTypeToken<List<Integer>>() {});
    }

    public List<Long> getLongList(String key) {
        return get(key, new SerializationTypeToken<List<Long>>() {});
    }

    public List<Double> getDoubleList(String key) {
        return get(key, new SerializationTypeToken<List<Double>>() {});
    }

    public List<Boolean> getBooleanList(String key) {
        return get(key, new SerializationTypeToken<List<Boolean>>() {});
    }

    public List<Float> getFloatList(String key) {
        return get(key, new SerializationTypeToken<List<Float>>() {});
    }

    public List<Short> getShortList(String key) {
        return get(key, new SerializationTypeToken<List<Short>>() {});
    }

    public List<Byte> getByteList(String key) {
        return get(key, new SerializationTypeToken<List<Byte>>() {});
    }

    public List<Character> getCharList(String key) {
        return get(key, new SerializationTypeToken<List<Character>>() {});
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
        return tryGet(key, new SerializationTypeToken<List<String>>() {});
    }

    public TryValue<List<Integer>> tryGetIntList(String key) {
        return tryGet(key, new SerializationTypeToken<List<Integer>>() {});
    }

    public TryValue<List<Long>> tryGetLongList(String key) {
        return tryGet(key, new SerializationTypeToken<List<Long>>() {});
    }

    public TryValue<List<Double>> tryGetDoubleList(String key) {
        return tryGet(key, new SerializationTypeToken<List<Double>>() {});
    }

    public TryValue<List<Boolean>> tryGetBooleanList(String key) {
        return tryGet(key, new SerializationTypeToken<List<Boolean>>() {});
    }

    public TryValue<List<Float>> tryGetFloatList(String key) {
        return tryGet(key, new SerializationTypeToken<List<Float>>() {});
    }

    public TryValue<List<Short>> tryGetShortList(String key) {
        return tryGet(key, new SerializationTypeToken<List<Short>>() {});
    }

    public TryValue<List<Byte>> tryGetByteList(String key) {
        return tryGet(key, new SerializationTypeToken<List<Byte>>() {});
    }

    public TryValue<List<Character>> tryGetCharList(String key) {
        return tryGet(key, new SerializationTypeToken<List<Character>>() {});
    }
}
