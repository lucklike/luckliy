package com.luckyframework.common;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.conversion.TypeConversionException;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java容器工具
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/7 下午10:49
 */
@SuppressWarnings("unchecked")
public class ContainerUtils {


    /**
     * 将数组转化为Set
     * @param array 数组
     * @param <T>   数组元素的类型
     * @return      转化后的Set
     */
    public static <T> Set<T> arrayToSet(T[] array){
        return Stream.of(array).collect(Collectors.toSet());
    }

    /**
     * 将数组中的元素拷贝到Set中
     * @param array 数组
     * @param set 集合
     * @param <T> 泛型
     */
    public static <T> void copyToSet(T[] array, Set<T> set){
        set.addAll(Arrays.asList(array));
    }

    /**
     * 将数组转化为List
     * @param array 数组
     * @param <T>   数组元素的类型
     * @return      转化后的List
     */
    public static <T> List<T> arrayToList(T[] array){
        return Stream.of(array).collect(Collectors.toList());
    }

    /**
     * 将数组中的元素拷贝到List中
     * @param array 数组
     * @param list 集合
     * @param <T> 泛型
     */
    public static <T> void copyToList(T[] array, List<T> list){
        list.addAll(Arrays.asList(array));
    }

    /***
     * 将List转化为数组
     * @param list List集合
     * @param <T>  集合元素类型
     * @return     转化后的数组
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] listToArray(List<T> list, Class<T> entryClass){
        Assert.notNull(list,"list is null!");
        int size = list.size();
        Object array = Array.newInstance(entryClass,size);
        for (int i = 0; i < size; i++) {
            Array.set(array, i, list.get(i));
        }
        return (T[]) array;
    }

    /***
     * 将Set转化为数组
     * @param set Set
     * @param <T>  集合元素类型
     * @return     转化后的数组
     */
    public static <T> T[] setToArray(Set<T> set, Class<T> entryClass){
        List<T> list = new ArrayList<>(set);
        return listToArray(list, entryClass);
    }

    /**
     * 将byte数组转化为输入流
     * @param byteData byte数组
     * @return 输入流
     */
    public static InputStream byteArrayToInputStream(byte[] byteData){
        return new ByteArrayInputStream(byteData);
    }

    /**
     * 融合多个数组，将新数组返回
     * @param arrays 数组列表
     * @return 融合后的数组
     */
    public static Object[] merge(Object[]... arrays){
        List<Object> list = new ArrayList<>();
        for (Object[] array : arrays) {
            list.addAll(Arrays.asList(array));
        }
        Object[] result=new Class[list.size()];
        list.toArray(result);
        return result;
    }

    /**
     * 判断元素是否在数组中
     * @param array 数组
     * @param source 目标对象
     */
    public static boolean inArrays(Object[] array, Object source){
        if(isEmptyArray(array)){
            return false;
        }
        for (Object entry : array) {
            if((entry == null && source == null) || source.equals(entry)){
                return true;
            }
        }
        return false;
    }

    /**
     * 判断数组是否为空
     * @param array 数组
     */
    public static boolean isEmptyArray(Object[] array){
        return array == null || array.length == 0;
    }


    /**
     * 判断Map是否为空
     * @param map 待判断的Map
     */
    public static boolean isEmptyMap(Map<?,?> map){
        return map==null||map.isEmpty();
    }

    /**
     * 判断集合是否为空集合
     * @param collection 待判断的集合
     */
    public static boolean isEmptyCollection(Collection<?> collection){
        return collection==null||collection.isEmpty();
    }

    /**
     * 数组向下转型,将Object类型的数组转化为确定类型的数组
     * @param array 原数组
     * @param aClass 转化的目标数组元素类型
     * @return 向下转型后的数组
     * @param <T> 泛型
     */
    public static <T> T[] arrayDowncasting(@NonNull Object[] array, Class<T> aClass){
        if(array.getClass().getComponentType() == aClass){
            return (T[]) array;
        }
        Object arrayDown = Array.newInstance(aClass, array.length);
        for (int i = 0; i < array.length; i++) {
            try {
                Array.set(arrayDown, i, array[i]);
            }catch (Exception e){
                throw new TypeConversionException("Array downcast failed, element at element {["+array[i].getClass()+"] '"+array[i]+"'} cannot be cast to '"+aClass+"'");
            }
        }
        return (T[]) arrayDown;
    }

    /**
     * 将Map中的元素按照指定的规则进行排序，返回排序之后的Map
     * @param sourceMap         原始Map
     * @param entryComparator   元素排序比较器
     * @return                  排序之后的Map
     * @param <K>               Key泛型
     * @param <V>               Value泛型
     */
    public static <K, V> Map<K, V> mapSorted(Map<K, V> sourceMap, Comparator<Map.Entry<K, V>> entryComparator){
        Map<K, V> resultMap = new LinkedHashMap<>(sourceMap.size());
        List<Map.Entry<K, V>> entryList = new ArrayList<>(sourceMap.entrySet());
        entryList.sort(entryComparator);
        entryList.forEach(en -> resultMap.put(en.getKey(), en.getValue()));
        return resultMap;
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        String[] array = ConversionUtils.conversion(list, String[].class);
        Console.printCyan(Arrays.toString(array));
    }
}
