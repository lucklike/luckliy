package com.luckyframework.environment.v1;

import java.util.Map;

/**
 * 数据单元
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/18 下午6:08
 */
public interface StorageUnit {

    String NULL_EXPRESSION = "#{null}";


    /***
     * 获取真实值
     * @param key key值
     * @return key对应的真实值
     */
    Object getRealValue(String key);

    /**
     * 将输入值转化为真实值
     * @param value 输入值
     * @return 真实值
     */
    Object changeToReal(Object value);

    /**
     * 解析单个${}表达式
     * @param single$Expression 单个${}表达式
     * @return 表达式对应的值
     */
    Object parsSingleExpression(String single$Expression);

    /**
     * 解析包含${}的表达式
     * @param $Expression 可能含有表达式的对象
     * @return 返回解析后的值
     */
    Object parsExpression(Object $Expression);

    /**
     * 获取真实值组成的Map
     * @return 真实值Map
     */
    Map<String,Object> getRealMap();

    /**
     * 获取原始值组成的Map
     * @return 原始值Map
     */
    Map<String,Object> getOriginalMap();

    /**
     * 设置一个K-V数据
     * @param key   Key值
     * @param value Value值
     */
    void setProperties(String key , Object value);

    /**
     * 检验一个表达式是否是一个${}表达式
     * @param prefix 待检验的表达式
     */
    default boolean isExpression(String prefix){
        prefix=prefix.trim();
        return prefix.startsWith("${")&&prefix.endsWith("}");
    }
}
