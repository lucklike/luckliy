package com.luckyframework.conversion;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 转换器管理器，用于注册、获取转换器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/4 21:02
 */
@SuppressWarnings("all")
public abstract class ConversionManager {

    /** 所有注册进来的转换器*/
    private static final Map<String, ConversionService> conversionServiceMap = new ConcurrentHashMap<>(64);

    /**
     * 注册一个转换器，如果该转换器已经存在会抛{@link IllegalArgumentException}异常
     * @param conversionName        转换器名称
     * @param conversionService     转换器
     */
    public static void registryConversionService(String conversionName, ConversionService conversionService){
        if(contains(conversionName)){
            throw new IllegalArgumentException("Converter named '"+conversionName+"' already exists!");
        }
        conversionServiceMap.put(conversionName, conversionService);
    }

    /**
     * 根据转换器名称获取一个转化器实例
     * @param conversionName 转化器名称
     * @return 转化器实例
     */
    public static ConversionService getConversionService(String conversionName){
        return conversionServiceMap.get(conversionName);
    }

    /**
     * 是否存在该名称的转换器
     * @param conversionName 转换器名称
     * @return 是否存在该名称的转换器
     */
    public static boolean contains(String conversionName){
        return conversionServiceMap.containsKey(conversionName);
    }

    /**
     * 获取所有转换器映射关系
     * @return 所有转换器映射关系
     */
    public static Map<String, ConversionService> getConversionServiceMap(){
        return conversionServiceMap;
    }

    /**
     * 获取所有转换器的名称
     * @return 所有转换器的名称
     */
    public static Set<String> getAllConversionNames(){
        return getConversionServiceMap().keySet();
    }

    /**
     * 获取所有转换器实例
     * @return 所有转换器实例
     */
    public Collection<ConversionService> getAllConversionServices(){
        return conversionServiceMap.values();
    }

}
