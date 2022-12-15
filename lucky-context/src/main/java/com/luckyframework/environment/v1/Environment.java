package com.luckyframework.environment.v1;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 环境变量
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/18 下午5:45
 */
@SuppressWarnings("all")
public interface Environment {

    String COMMON = "DEF";

    /**
     * 根据Key拿到对应的环境变量
     * @param key key值
     * @return key对应的环境变量
     */
    Object getProperty(String key);

    /**
     * 解析单个${}表达式
     * @param single$Expression 单个${}表达式
     * @return 表达式对应的值
     */
    Object parsSingleExpression(String single$Expression);

    /**
     * 解析${}表达式
     * @param $Expression 可能含有表达式的对象
     * @return 返回解析后的值
     */
    Object parsExpression(Object $Expression);

    /**
     * 根据Key拿到对应的环境变量,并将其转化为对应类型
     * @param key key值
     * @param aClass 类型
     * @return key对应的环境变量
     */
    default <T> T getProperty(String key,Class<T> aClass){
        return (T) getProperty(key,ResolvableType.forClass(aClass));
    }

    /**
     * 根据Key拿到对应的环境变量,并将其转化为对应类型
     * @param key key值
     * @param type 类型
     * @return key对应的环境变量
     */
    default <T> T getProperty(String key, Type type){
        return (T) getProperty(key,ResolvableType.forType(type));
    }

    /**
     * 解析${}表达式,并将其转化为对应类型
     * @param $Expression 单个${Expression}表达式
     * @param resolvableType 类型
     * @return key对应的环境变量
     */
    Object parsExpression(Object $Expression, ResolvableType resolvableType);

    /**
     * 解析单个${}表达式,并将其转化为对应类型
     * @param $Expression 单个${Expression}表达式
     * @param aClass 类型
     * @return key对应的环境变量
     */
    default <T> T parsExpression(Object $Expression,Class<T> aClass){
        return (T) parsExpression($Expression,ResolvableType.forClass(aClass));
    }

    /**
     * 解析单个${}表达式,并将其转化为对应类型
     * @param $Expression key值
     * @param type 类型
     * @return key对应的环境变量
     */
    default <T> T parsExpression(Object $Expression, Type type){
        return (T) parsExpression($Expression,ResolvableType.forType(type));
    }

    /**
     * 解析单个${}表达式,并将其转化为对应类型
     * @param single$Expression 单个${Expression}表达式
     * @param resolvableType 类型
     * @return key对应的环境变量
     */
    Object parsSingleExpression(String single$Expression, ResolvableType resolvableType);

    /**
     * 解析单个${}表达式,并将其转化为对应类型
     * @param single$Expression 单个${Expression}表达式
     * @param aClass 类型
     * @return key对应的环境变量
     */
    default <T> T parsSingleExpression(String single$Expression,Class<T> aClass){
        return (T) parsSingleExpression(single$Expression,ResolvableType.forClass(aClass));
    }

    /**
     * 解析单个${}表达式,并将其转化为对应类型
     * @param single$Expression key值
     * @param type 类型
     * @return key对应的环境变量
     */
    default <T> T parsSingleExpression(String single$Expression, Type type){
        return (T) parsSingleExpression(single$Expression,ResolvableType.forType(type));
    }

    /**
     * 根据Key拿到对应的环境变量,并将其转化为对应类型
     * @param key key值
     * @param resolvableType 类型
     * @return key对应的环境变量
     */
    Object getProperty(String key, ResolvableType resolvableType);

    /**
     * 设置环境变量
     * @param key Key值
     * @param value Value值
     */
    void setProperty(String key,Object value);

    /**
     * 获取所有环境变量
     * @return 所有环境变量
     */
    Map<String,Object> getProperties();

    /**
     * 获取原始值组成的Map
     * @return 原始值Map
     */
    Map<String,Object> getOriginalMap();

    /**
     * 返回环境名称
     * @return 环境名称
     */
    default String getProfiles(){
        return COMMON;
    }

    /**
     * 判断key是否在环境变量中有对应
     * @param key
     * @return
     */
    boolean containsKey(String key);

    default void setJvmEnv(String key, Object value){

    }

    default <T> T getProperty(String key,Class<T> type,T defaultValue){
        return containsKey(key) ? getProperty(key,type) : defaultValue;
    }
}
