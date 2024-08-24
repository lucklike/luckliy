package com.luckyframework.common;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 支持纯字符串操作的Map接口
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/17 5:52 下午
 */
public interface SupportsStringManipulationMap {


    Object getProperty(String configKey);

    default Object getPropertyOrDefault(String configKey, Object defaultValue) {
        return containsConfigKey(configKey) ? getProperty(configKey) : defaultValue;
    }

    default <T> T getEntry(String configKey, Class<T> type) {
        return ConversionUtils.conversion(getProperty(configKey), type);
    }

    default <T> T getEntryOrDefault(String configKey, Class<T> type, T defaultValue) {
        return containsConfigKey(configKey) ? getEntry(configKey, type) : defaultValue;
    }

    default <T> T getEntry(String configKey, Type type) {
        return ConversionUtils.conversion(getProperty(configKey), type);
    }

    default <T> T getEntryOrDefault(String configKey, Type type, T defaultValue) {
        return containsConfigKey(configKey) ? getEntry(configKey, type) : defaultValue;
    }


    default <T> T getEntry(String configKey, SerializationTypeToken<T> typeToken) {
        return ConversionUtils.conversion(getProperty(configKey), typeToken);
    }

    default <T> T getEntryOrDefault(String configKey, SerializationTypeToken<T> typeToken, T defaultValue) {
        return containsConfigKey(configKey) ? getEntry(configKey, typeToken) : defaultValue;
    }

    @SuppressWarnings("unchecked")
    default <T> T getEntry(String configKey, ResolvableType resolvableType) {
        return (T) ConversionUtils.conversion(getProperty(configKey), resolvableType);
    }

    default <T> T getEntryOrDefault(String configKey, ResolvableType resolvableType, T defaultValue) {
        return containsConfigKey(configKey) ? getEntry(configKey, resolvableType) : defaultValue;
    }

    default byte getByte(String configKey) {
        return getEntry(configKey, byte.class);
    }

    default byte getByteOrDefault(String configKey, byte defaultValue) {
        return containsConfigKey(configKey) ? getByte(configKey) : defaultValue;
    }

    default byte[] getByteArray(String configKey) {
        return getEntry(configKey, byte[].class);
    }

    default byte[] getByteArrayOrDefault(String configKey, byte[] defaultValue) {
        return containsConfigKey(configKey) ? getByteArray(configKey) : defaultValue;
    }

    default boolean getBoolean(String configKey) {
        return getEntry(configKey, boolean.class);
    }

    default boolean getBooleanOrDefault(String configKey, boolean defaultValue) {
        return containsConfigKey(configKey) ? getBoolean(configKey) : defaultValue;
    }

    default boolean[] getBooleanArray(String configKey) {
        return getEntry(configKey, boolean[].class);
    }

    default boolean[] getBooleanArrayOrDefault(String configKey, boolean[] defaultValue) {
        return containsConfigKey(configKey) ? getBooleanArray(configKey) : defaultValue;
    }

    default char getChar(String configKey) {
        return getEntry(configKey, char.class);
    }

    default char getCharOrDefault(String configKey, char defaultValue) {
        return containsConfigKey(configKey) ? getChar(configKey) : defaultValue;
    }

    default char[] getCharArray(String configKey) {
        return getEntry(configKey, char[].class);
    }

    default char[] getCharArrayOrDefault(String configKey, char[] defaultValue) {
        return containsConfigKey(configKey) ? getCharArray(configKey) : defaultValue;
    }

    default short getShort(String configKey) {
        return getEntry(configKey, short.class);
    }

    default short getShortOrDefault(String configKey, short defaultValue) {
        return containsConfigKey(configKey) ? getShort(configKey) : defaultValue;
    }

    default short[] getShortArray(String configKey) {
        return getEntry(configKey, short[].class);
    }

    default short[] getShortArrayOrDefault(String configKey, short[] defaultValue) {
        return containsConfigKey(configKey) ? getShortArray(configKey) : defaultValue;
    }

    default int getInt(String configKey) {
        return getEntry(configKey, int.class);
    }

    default int getIntOrDefault(String configKey, int defaultValue) {
        return containsConfigKey(configKey) ? getInt(configKey) : defaultValue;
    }

    default int[] getIntArray(String configKey) {
        return getEntry(configKey, int[].class);
    }

    default int[] getIntArrayOrDefault(String configKey, int[] defaultValue) {
        return containsConfigKey(configKey) ? getIntArray(configKey) : defaultValue;
    }

    default List<Integer> getIntList(String configKey) {
        return getEntry(configKey, new SerializationTypeToken<List<Integer>>() {
        });
    }

    default List<Integer> getIntListOrDefault(String configKey, List<Integer> defaultValue) {
        return containsConfigKey(configKey) ? getIntList(configKey) : defaultValue;
    }

    default long getLong(String configKey) {
        return getEntry(configKey, long.class);
    }

    default long getLongOrDefault(String configKey, long defaultValue) {
        return containsConfigKey(configKey) ? getLong(configKey) : defaultValue;
    }

    default long[] getLongArray(String configKey) {
        return getEntry(configKey, long[].class);
    }

    default long[] getLongArrayOrDefault(String configKey, long[] defaultValue) {
        return containsConfigKey(configKey) ? getLongArray(configKey) : defaultValue;
    }

    default List<Long> getLongList(String configKey) {
        return getEntry(configKey, new SerializationTypeToken<List<Long>>() {
        });
    }

    default List<Long> getLongListOrDefault(String configKey, List<Long> defaultValue) {
        return containsConfigKey(configKey) ? getLongList(configKey) : defaultValue;
    }

    default double getDouble(String configKey) {
        return getEntry(configKey, double.class);
    }

    default double getDoubleOrDefault(String configKey, double defaultValue) {
        return containsConfigKey(configKey) ? getDouble(configKey) : defaultValue;
    }

    default double[] getDoubleArray(String configKey) {
        return getEntry(configKey, double[].class);
    }

    default double[] getDoubleArrayOrDefault(String configKey, double[] defaultValue) {
        return containsConfigKey(configKey) ? getDoubleArray(configKey) : defaultValue;
    }

    default List<Double> getDoubleList(String configKey) {
        return getEntry(configKey, new SerializationTypeToken<List<Double>>() {
        });
    }

    default List<Double> getDoubleListOrDefault(String configKey, List<Double> defaultValue) {
        return containsConfigKey(configKey) ? getDoubleList(configKey) : defaultValue;
    }

    default float getFloat(String configKey) {
        return getEntry(configKey, float.class);
    }

    default float getFloatOrDefault(String configKey, float defaultValue) {
        return containsConfigKey(configKey) ? getFloat(configKey) : defaultValue;
    }

    default float[] getFloatArray(String configKey) {
        return getEntry(configKey, float[].class);
    }

    default float[] getFloatArrayOrDefault(String configKey, float[] defaultValue) {
        return containsConfigKey(configKey) ? getFloatArray(configKey) : defaultValue;
    }

    default List<Float> getFloatList(String configKey) {
        return getEntry(configKey, new SerializationTypeToken<List<Float>>() {
        });
    }

    default List<Float> getFloatListOrDefault(String configKey, List<Float> defaultValue) {
        return containsConfigKey(configKey) ? getFloatList(configKey) : defaultValue;
    }

    default String getString(String configKey) {
        return getEntry(configKey, String.class);
    }

    default String getStringOrDefault(String configKey, String defaultValue) {
        return containsConfigKey(configKey) ? getString(configKey) : defaultValue;
    }

    default String[] getStringArray(String configKey) {
        return getEntry(configKey, String[].class);
    }

    default String[] getStringArrayOrDefault(String configKey, String[] defaultValue) {
        return containsConfigKey(configKey) ? getStringArray(configKey) : defaultValue;
    }


    default List<String> getStringList(String configKey) {
        return getEntry(configKey, new SerializationTypeToken<List<String>>() {
        });
    }

    default List<String> getStringListOrDefault(String configKey, List<String> defaultValue) {
        return containsConfigKey(configKey) ? getStringList(configKey) : defaultValue;
    }

    default Class<?> getClass(String configKey) {
        return getEntry(configKey, Class.class);
    }

    default Class<?> getClassOrDefault(String configKey, Class<?> defaultValue) {
        return containsConfigKey(configKey) ? getClass(configKey) : defaultValue;
    }

    default Class<?>[] getClassArray(String configKey) {
        return getEntry(configKey, Class[].class);
    }

    default Class<?>[] getClassArrayOrDefault(String configKey, Class<?>[] defaultValue) {
        return containsConfigKey(configKey) ? getClassArray(configKey) : defaultValue;
    }

    default <E extends Enum<E>> E getEnum(String configKey, Class<E> type) {
        return getEntry(configKey, type);
    }

    default <E extends Enum<E>> E getEnumOrDefault(String configKey, Class<E> type, E defaultValue) {
        return containsConfigKey(configKey) ? getEnum(configKey, type) : defaultValue;
    }

    default <E extends Enum<E>> E[] getEnumArray(String configKey, Class<E> type) {
        return getEntry(configKey, ResolvableType.forArrayComponent(ResolvableType.forClass(type)));
    }

    default <E extends Enum<E>> E[] getEnumArrayOrDefault(String configKey, Class<E> type, E[] defaultValue) {
        return containsConfigKey(configKey) ? getEnumArray(configKey, type) : defaultValue;
    }

    default <E extends Enum<E>> List<E> getEnumList(String configKey, Class<E> type) {
        ResolvableType resolvableType = ResolvableType.forClassWithGenerics(List.class, type);
        return getEntry(configKey, resolvableType);
    }

    default <E extends Enum<E>> List<E> getEnumListOrDefault(String configKey, Class<E> type, List<E> defaultValue) {
        return containsConfigKey(configKey) ? getEnumList(configKey, type) : defaultValue;
    }

    default boolean isAssignableFrom(String configKey, Class<?> type) {
        if (!containsConfigKey(configKey)) {
            return false;
        }
        Object object = getProperty(configKey);
        if (object == null) {
            return false;
        }
        return type.isAssignableFrom(object.getClass());
    }

    default boolean isAssignableFrom(String configKey, ResolvableType resolvableType) {
        if (!containsConfigKey(configKey)) {
            return false;
        }
        Object object = getProperty(configKey);
        if (object == null) {
            return false;
        }
        return ClassUtils.compatibleOrNot(resolvableType, ResolvableType.forInstance(object));
    }

    default boolean isAssignableFrom(String configKey, SerializationTypeToken<?> typeToken) {
        return isAssignableFrom(configKey, ResolvableType.forType(typeToken.getType()));
    }

    @SuppressWarnings("unchecked")
    default <T> void isAssignableFromRunning(String configKey, Class<T> type, Consumer<T> consumer) {
        if (!containsConfigKey(configKey)) {
            return;
        }
        Object object = getProperty(configKey);
        if (object == null) {
            return;
        }
        if (!type.isAssignableFrom(object.getClass())) {
            return;
        }
        consumer.accept((T) object);
    }

    default <T> void isAssignableFromRunning(String configKey, SerializationTypeToken<T> typeToken, Consumer<T> consumer) {
        if (!containsConfigKey(configKey)) {
            return;
        }
        Object object = getProperty(configKey);
        if (object == null) {
            return;
        }
        if (!ClassUtils.compatibleOrNot(ResolvableType.forType(typeToken.getType()), ResolvableType.forInstance(object))) {
            return;
        }
        consumer.accept(ConversionUtils.conversion(object, typeToken));
    }

    @SuppressWarnings("unchecked")
    default <T> T isAssignableFromReturn(String configKey, @NonNull T defaultValue) {
        T fromReturn = (T) isAssignableFromReturn(configKey, defaultValue.getClass());
        return fromReturn == null ? defaultValue : fromReturn;
    }

    @SuppressWarnings("unchecked")
    default <T> T isAssignableFromReturn(String configKey, @NonNull Class<T> type) {
        if (!containsConfigKey(configKey)) {
            return null;
        }
        Object object = getProperty(configKey);
        if (object == null) {
            return null;
        }
        if (!type.isAssignableFrom(object.getClass())) {
            return null;
        }
        return (T) object;
    }

    default <T> T isAssignableFromReturn(String configKey, SerializationTypeToken<T> typeToken) {
        if (!containsConfigKey(configKey)) {
            return null;
        }
        Object object = getProperty(configKey);
        if (object == null) {
            return null;
        }
        if (!ClassUtils.compatibleOrNot(ResolvableType.forType(typeToken.getType()), ResolvableType.forInstance(object))) {
            return null;
        }
        return ConversionUtils.conversion(object, typeToken);
    }

    default boolean isIterable(String configKey) {
        if (!containsConfigKey(configKey)) {
            return false;
        }
        Object object = getProperty(configKey);
        if (object == null) {
            return false;
        }
        return ContainerUtils.isIterable(object);
    }

    default Iterator<Object> getIterator(String configKey) {
        return ContainerUtils.getIterator(getProperty(configKey));
    }

    default <T> Iterator<T> getIterator(String configKey, Type type) {
        return ContainerUtils.getIterator(getProperty(configKey), type);
    }

    default <T> Iterator<T> getIterator(String configKey, Class<T> type) {
        return ContainerUtils.getIterator(getProperty(configKey), type);
    }

    default <T> Iterator<T> getIterator(String configKey, SerializationTypeToken<T> typeToken) {
        return ContainerUtils.getIterator(getProperty(configKey), typeToken);
    }

    default <T> Iterator<T> getIterator(String configKey, ResolvableType resolvableType) {
        return ContainerUtils.getIterator(getProperty(configKey), resolvableType);
    }

    default Iterable<Object> getIterable(String configKey) {
        return ContainerUtils.getIterable(getProperty(configKey));
    }

    default <T> Iterable<T> getIterable(String configKey, Type type) {
        return ContainerUtils.getIterable(getProperty(configKey), type);
    }

    default <T> Iterable<T> getIterable(String configKey, Class<T> type) {
        return ContainerUtils.getIterable(getProperty(configKey), type);
    }

    default <T> Iterable<T> getIterable(String configKey, SerializationTypeToken<T> typeToken) {
        return ContainerUtils.getIterable(getProperty(configKey), typeToken);
    }

    default <T> Iterable<T> getIterable(String configKey, ResolvableType resolvableType) {
        return ContainerUtils.getIterable(getProperty(configKey), resolvableType);
    }

    default <T> T looseBind(String configKey, ResolvableType resolvableType) {
        return ConversionUtils.looseBind(resolvableType, getProperty(configKey));
    }

    default <T> T looseBind(String configKey, SerializationTypeToken<T> resolvableType) {
        return ConversionUtils.looseBind(resolvableType, getProperty(configKey));
    }

    default <T> T looseBind(String configKey, Class<T> resolvableType) {
        return ConversionUtils.looseBind(resolvableType, getProperty(configKey));
    }

    default <T> T looseBind(String configKey, Type resolvableType) {
        return ConversionUtils.looseBind(resolvableType, getProperty(configKey));
    }

    SupportsStringManipulationMap getMap(String configKey);

    List<? extends SupportsStringManipulationMap> getMapList(String configKey);

    void addProperty(String configKey, Object confValue);

    void addProperties(Map<?, ?> properties);

    void addAsItExists(String configKey, Object confValue);

    void addAsItExists(Map<String, Object> properties);

    boolean containsConfigKey(String configKey);

    Object removeConfigProperty(String configKey);

    void mergeConfig(Map<String, Object> newConfigMap);

    Object putIn(String sourceKey, String newKey, Object newValue);

    void putEntity(Object entity);
}
