package com.luckyframework.common;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 支持表达式操作的 Bean 对象接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/12/1 01:25
 */
public interface ExpressionBean<T> {

    /**
     * 获取原始 Bean 对象
     *
     * @return 原始 Bean 对象
     */
    T getBean();

    /**
     * 通过表达式给对象的某个属性赋值
     *
     * @param expression 赋值表达式
     * @param value      值
     */
    void set(String expression, Object value);

    /**
     * 通过表达式来获取对象中的某个属性的值
     *
     * @param expression 取值表达式
     * @param type       结果类型
     * @param <V>        结果类型泛型
     * @return 取值表达式对应的值
     */
    <V> V get(String expression, Type type);

    /**
     * 将自身转化为另一种泛型结构的{@link ExpressionBean}
     *
     * @param type 目标类型
     * @param <R>  目标类型泛型
     * @return 目标类型的{@link ExpressionBean}对象
     */
    <R> ExpressionBean<R> to(Type type);

    /**
     * 是否存在Bean对象
     *
     * @return 是否存在Bean对象
     */
    default boolean hasBean() {
        return getBean() != null;
    }

    //----------------------------------------------------------
    //                      to
    //----------------------------------------------------------

    default <R> ExpressionBean<R> to(Class<R> type) {
        return to((Type) type);
    }

    default <R> ExpressionBean<R> to(SerializationTypeToken<R> typeToken) {
        return to(typeToken.getType());
    }

    default <R> ExpressionBean<R> to(ResolvableType type) {
        return to(type.getType());
    }

    //----------------------------------------------------------
    //                      beanConvert
    //----------------------------------------------------------

    default <R> R beanConvert(Type typ) {
        return ConversionUtils.conversion(getBean(), typ);
    }

    default <R> R beanConvert(Class<R> type) {
        return beanConvert((Type) type);
    }

    default <R> R beanConvert(SerializationTypeToken<R> typeToken) {
        return beanConvert(typeToken.getType());
    }

    default <R> R beanConvert(ResolvableType type) {
        return beanConvert(type.getType());
    }

    //----------------------------------------------------------
    //                          get
    //----------------------------------------------------------

    default Object get(String expression) {
        return get(expression, Object.class);
    }

    default <V> V get(String expression, ResolvableType type) {
        return get(expression, type.getType());
    }

    default <V> V get(String expression, SerializationTypeToken<V> typeToken) {
        return get(expression, typeToken.getType());
    }

    default <V> V get(String expression, Class<V> clazz) {
        return get(expression, (Type) clazz);
    }

    default <E> List<E> getList(String expression, Class<E> elementClass) {
        return get(expression, ResolvableType.forClassWithGenerics(List.class, elementClass));
    }

    default List<?> getList(String expression) {
        return getList(expression, Object.class);
    }

    //----------------------------------------------------------
    //                    Basic Types
    //----------------------------------------------------------


    default String getString(String expression) {
        return get(expression, String.class);
    }

    default Integer getInt(String expression) {
        return get(expression, Integer.class);
    }

    default Long getLong(String expression) {
        return get(expression, Long.class);
    }

    default Double getDouble(String expression) {
        return get(expression, Double.class);
    }

    default Boolean getBoolean(String expression) {
        return get(expression, Boolean.class);
    }

    default Float getFloat(String expression) {
        return get(expression, Float.class);
    }

    default Short getShort(String expression) {
        return get(expression, Short.class);
    }

    default Byte getByte(String expression) {
        return get(expression, Byte.class);
    }

    default Character getChar(String expression) {
        return get(expression, Character.class);
    }

    //----------------------------------------------------------
    //                    Basic Types Array
    //----------------------------------------------------------

    default String[] getStringArray(String expression) {
        return get(expression, String[].class);
    }

    default int[] getIntArray(String expression) {
        return get(expression, int[].class);
    }

    default long[] getLongArray(String expression) {
        return get(expression, long[].class);
    }

    default double[] getDoubleArray(String expression) {
        return get(expression, double[].class);
    }

    default boolean[] getBooleanArray(String expression) {
        return get(expression, boolean[].class);
    }

    default float[] getFloatArray(String expression) {
        return get(expression, float[].class);
    }

    default short[] getShortArray(String expression) {
        return get(expression, short[].class);
    }

    default byte[] getByteArray(String expression) {
        return get(expression, byte[].class);
    }

    default char[] getCharArray(String expression) {
        return get(expression, char[].class);
    }


    //----------------------------------------------------------
    //                    Basic Types Array
    //----------------------------------------------------------

    default List<String> getStringList(String expression) {
        return getList(expression, String.class);
    }

    default List<Integer> getIntList(String expression) {
        return getList(expression, Integer.class);
    }

    default List<Long> getLongList(String expression) {
        return getList(expression, Long.class);
    }

    default List<Double> getDoubleList(String expression) {
        return getList(expression, Double.class);
    }

    default List<Boolean> getBooleanList(String expression) {
        return getList(expression, Boolean.class);
    }

    default List<Float> getFloatList(String expression) {
        return getList(expression, Float.class);
    }

    default List<Short> getShortList(String expression) {
        return getList(expression, Short.class);
    }

    default List<Byte> getByteList(String expression) {
        return getList(expression, Byte.class);
    }

    default List<Character> getCharList(String expression) {
        return getList(expression, Character.class);
    }


}
