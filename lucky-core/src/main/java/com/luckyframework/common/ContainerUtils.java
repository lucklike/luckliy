package com.luckyframework.common;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.conversion.TypeConversionException;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java容器工具
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/7 下午10:49
 */
@SuppressWarnings("unchecked")
public class ContainerUtils {


    /**
     * 将数组转化为Set
     *
     * @param array 数组
     * @param <T>   数组元素的类型
     * @return 转化后的Set
     */
    public static <T> Set<T> arrayToSet(T[] array) {
        return Stream.of(array).collect(Collectors.toSet());
    }

    /**
     * 将数组中的元素拷贝到Set中
     *
     * @param array 数组
     * @param set   集合
     * @param <T>   泛型
     */
    public static <T> void copyToSet(T[] array, Set<T> set) {
        set.addAll(Arrays.asList(array));
    }

    /**
     * 将数组转化为List
     *
     * @param array 数组
     * @param <T>   数组元素的类型
     * @return 转化后的List
     */
    public static <T> List<T> arrayToList(T[] array) {
        return Stream.of(array).collect(Collectors.toList());
    }

    /**
     * 将数组中的元素拷贝到List中
     *
     * @param array 数组
     * @param list  集合
     * @param <T>   泛型
     */
    public static <T> void copyToList(T[] array, List<T> list) {
        list.addAll(Arrays.asList(array));
    }

    /***
     * 将List转化为数组
     * @param list List集合
     * @param <T>  集合元素类型
     * @return 转化后的数组
     */
    public static <T> T[] listToArray(List<T> list, Class<T> entryClass) {
        return iterableToArray(list, entryClass);
    }

    /***
     * 将Set转化为数组
     * @param set Set
     * @param <T>  集合元素类型
     * @return 转化后的数组
     */
    public static <T> T[] setToArray(Set<T> set, Class<T> entryClass) {
        return iterableToArray(set, entryClass);
    }

    /**
     * 将byte数组转化为输入流
     *
     * @param byteData byte数组
     * @return 输入流
     */
    public static InputStream byteArrayToInputStream(byte[] byteData) {
        return new ByteArrayInputStream(byteData);
    }

    /**
     * 融合多个数组，将新数组返回
     *
     * @param arrays 数组列表
     * @return 融合后的数组
     */
    public static Object[] merge(Object[]... arrays) {
        List<Object> list = new ArrayList<>();
        for (Object[] array : arrays) {
            list.addAll(Arrays.asList(array));
        }
        Object[] result = new Object[list.size()];
        list.toArray(result);
        return result;
    }

    /**
     * 判断元素是否在数组中
     *
     * @param array  数组
     * @param source 目标对象
     */
    public static boolean inArrays(Object[] array, Object source) {
        if (isEmptyArray(array)) {
            return false;
        }
        for (Object entry : array) {
            if ((entry == null && source == null) || source.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    public static boolean notInArrays(Object[] array, Object source) {
        return !inArrays(array, source);
    }

    /**
     * 判断数组是否为空
     *
     * @param array 数组
     */
    public static boolean isEmptyArray(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNotEmptyArray(Object[] array) {
        return !isEmptyArray(array);
    }

    /**
     * 判断Map是否为空
     *
     * @param map 待判断的Map
     */
    public static boolean isEmptyMap(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmptyMap(Map<?, ?> map) {
        return !isEmptyMap(map);
    }

    /**
     * 判断集合是否为空集合
     *
     * @param collection 待判断的集合
     */
    public static boolean isEmptyCollection(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmptyCollection(Collection<?> collection) {
        return !isEmptyCollection(collection);
    }

    /**
     * 数组向下转型,将Object类型的数组转化为确定类型的数组
     *
     * @param array  原数组
     * @param aClass 转化的目标数组元素类型
     * @param <T>    泛型
     * @return 向下转型后的数组
     */
    public static <T> T[] arrayDowncast(@NonNull Object[] array, Class<T> aClass) {
        if (array.getClass().getComponentType() == aClass) {
            return (T[]) array;
        }
        Object arrayDown = Array.newInstance(aClass, array.length);
        for (int i = 0; i < array.length; i++) {
            try {
                Array.set(arrayDown, i, array[i]);
            } catch (Exception e) {
                throw new TypeConversionException("Array downcast failed, element at element {[" + array[i].getClass() + "] '" + array[i] + "'} cannot be cast to '" + aClass + "'");
            }
        }
        return (T[]) arrayDown;
    }

    /**
     * 将Map中的元素按照指定的规则进行排序，返回排序之后的Map
     *
     * @param sourceMap       原始Map
     * @param entryComparator 元素排序比较器
     * @param <K>             Key泛型
     * @param <V>             Value泛型
     * @return 排序之后的Map
     */
    public static <K, V> Map<K, V> mapSorted(Map<K, V> sourceMap, Comparator<Map.Entry<K, V>> entryComparator) {
        Map<K, V> resultMap = new LinkedHashMap<>(sourceMap.size());
        List<Map.Entry<K, V>> entryList = new ArrayList<>(sourceMap.entrySet());
        entryList.sort(entryComparator);
        entryList.forEach(en -> resultMap.put(en.getKey(), en.getValue()));
        return resultMap;
    }

    public static boolean isIterable(Object object) {
        return isArray(object) || object instanceof Iterable || object instanceof Iterator;
    }

    /**
     * 获取元素类型
     * 1.当object为数组或者{@link Iterable}实例时，返回其元素类型
     * 2.当object为其他类型时返回其本身的类型
     *
     * @param object 目标对象
     * @return 元素类型
     */
    public static Class<?> getElementType(@NonNull Object object) {
        if (isArray(object)) {
            return object.getClass().getComponentType();
        }
        if (object instanceof Iterator) {
            Iterator<?> iterator = (Iterator<?>) object;
            while (iterator.hasNext()) {
                Object next = iterator.next();
                if (next != null) {
                    return next.getClass();
                }
            }
            return ResolvableType.forClass(Iterator.class, object.getClass()).getRawClass();
        }
        if (object instanceof Iterable) {
            for (Object next : (Iterable<?>) object) {
                if (next != null) {
                    return next.getClass();
                }
            }
            return ResolvableType.forClass(Iterable.class, object.getClass()).getRawClass();
        }
        return object.getClass();
    }

    public static Class<?> getElementType(@NonNull ResolvableType objectType) {
        if (objectType.isArray()) {
            return objectType.getComponentType().getRawClass();
        }
        Class<?> iteratorClass = Objects.requireNonNull(objectType.getRawClass());
        if (Iterable.class.isAssignableFrom(iteratorClass) || Iterator.class.isAssignableFrom(iteratorClass)) {
            return objectType.getGeneric(0).getRawClass();
        }
        return objectType.getRawClass();
    }

    public static boolean isCollectionOrArray(Object object) {
        return isArray(object) || isCollection(object);
    }

    public static boolean isArray(Object object) {
        return object != null && object.getClass().isArray();
    }

    public static boolean isCollection(Object object) {
        return object instanceof Collection;
    }

    public static Iterator<Object> getIterator(Object object) {
        if (object instanceof Iterator) {
            return (Iterator<Object>) object;
        }
        if (object instanceof Iterable) {
            return ((Iterable<Object>) object).iterator();
        }
        if (isArray(object)) {
            int length = Array.getLength(object);
            return new Iterator<Object>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < length;
                }

                @Override
                public Object next() {
                    return Array.get(object, index++);
                }
            };
        }
        throw new LuckyRuntimeException("The object '" + object + "' is not an iterable object.");
    }

    public static <T> Iterator<T> getIterator(Object object, Type type) {
        Iterator<Object> iterator = getIterator(object);
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return ConversionUtils.conversion(iterator.next(), type);
            }
        };
    }

    public static <T> Iterator<T> getIterator(Object object, Class<T> type) {
        return getIterator(object, (Type) type);
    }

    public static <T> Iterator<T> getIterator(Object object, ResolvableType type) {
        return getIterator(object, type.getType());
    }

    public static <T> Iterator<T> getIterator(Object object, SerializationTypeToken<T> typeToken) {
        return getIterator(object, typeToken.getType());
    }

    public static Iterable<Object> getIterable(Object object) {
        return new Iterable<Object>() {
            @Override
            @NonNull
            public Iterator<Object> iterator() {
                return getIterator(object);
            }
        };
    }

    public static <T> Iterable<T> getIterable(Object object, Type type) {
        return new Iterable<T>() {
            @Override
            @NonNull
            public Iterator<T> iterator() {
                return getIterator(object, type);
            }
        };
    }

    public static <T> Iterable<T> getIterable(Object object, Class<T> type) {
        return getIterable(object, (Type) type);
    }

    public static <T> Iterable<T> getIterable(Object object, ResolvableType type) {
        return getIterable(object, type.getType());
    }

    public static <T> Iterable<T> getIterable(Object object, SerializationTypeToken<T> typeToken) {
        return getIterable(object, typeToken.getType());
    }

    public static int getIteratorLength(Object object) {
        if (isArray(object)) {
            return Array.getLength(object);
        }
        if (isCollection(object)) {
            return ((Collection<?>) object).size();
        }
        if (object instanceof Iterable) {
            int length = 0;
            for (Object ignored : ((Iterable<?>) object)) {
                length++;
            }
            return length;
        }
        throw new LuckyRuntimeException("The object '" + object + "' is not an iterable object.");
    }

    public static <T> T getIteratorFirst(Iterator<T> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }


    /**
     * 判断给定的对象是否为特定元素类型的迭代器
     *
     * @param object      带判断的对象
     * @param elementType 元素类型
     * @return 给定的对象是否为特定元素类型的迭代器
     */
    public static boolean isSpecificElementIterable(Object object, @NonNull Class<?> elementType) {
        return isIterable(object) && getElementType(object) == elementType;
    }

    /**
     * 判断给定的类型是否为特定元素类型的迭代器
     *
     * @param objectType  带判断的对象
     * @param elementType 元素类型
     * @return 给定的类型是否为特定元素类型的迭代器
     */
    public static boolean isSpecificElementIterable(ResolvableType objectType, @NonNull Class<?> elementType) {
        return isIterable(objectType) && getElementType(objectType) == elementType;
    }

    public static <T> T[] iterableToArray(Iterable<T> iterator, Class<T> type) {
        int length = getIteratorLength(iterator);
        Object array = Array.newInstance(type, length);
        int i = 0;
        for (T t : iterator) {
            Array.set(array, i++, t);
        }
        return (T[]) array;
    }


    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        String[] array = ConversionUtils.conversion(list, String[].class);
        Console.printCyan(Arrays.toString(array));
    }
}
