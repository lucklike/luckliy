package com.luckyframework.httpclient.proxy.unpack;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文参数值拆包器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/11/24 13:35
 */
@FunctionalInterface
public interface ContextValueUnpack {

    /**
     * 参数转换器集合
     */
    List<ParameterConvert> parameterConverts = new ArrayList<>();

    /**
     * 添加一个参数转换器
     *
     * @param parameterConvert 参数转换器
     */
    static void addParameterConvert(ParameterConvert parameterConvert) {
        parameterConverts.add(parameterConvert);
    }

    /**
     * 添加一个参数转换器
     *
     * @param index       索引位置
     * @param parameterConvert 参数转换器
     */
    static void addParameterConvert(int index, ParameterConvert parameterConvert) {
        parameterConverts.add(index, parameterConvert);
    }

    /**
     * 设置一个参数转换器
     *
     * @param index       索引位置
     * @param parameterConvert 参数转换器
     */
    static void setParameterConvert(int index, ParameterConvert parameterConvert) {
        parameterConverts.set(index, parameterConvert);
    }

    /**
     * 传入一个参数转换器，覆盖某个指定的参数转换器（不存在时不覆盖）
     *
     * @param clazz       要覆盖的参数转换器
     * @param parameterConvert 传入的参数转换器
     */
    static void coverParameterConvert(Class<? extends ParameterConvert> clazz, ParameterConvert parameterConvert) {
        int index = getParameterConvertIndex(clazz);
        if (index != -1) {
            parameterConverts.set(index, parameterConvert);
        }
    }

    /**
     * 获取某个参数转换器的索引信息
     *
     * @param clazz 参数转换器类型
     * @return 对应的索引信息
     */
    static int getParameterConvertIndex(Class<? extends ParameterConvert> clazz) {
        for (int i = 0; i < parameterConverts.size(); i++) {
            if (parameterConverts.get(i).getClass() == clazz) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 用于将包装值转化为真实值的方法
     *
     * @param unpackContext 上下文
     * @param wrapperValue  包装值
     * @return 真实值
     * @throws ContextValueUnpackException 拆包失败时会抛出该异常
     */
    Object getRealValue(ValueUnpackContext unpackContext, Object wrapperValue) throws ContextValueUnpackException;
    
    
    default Object parameterConvert(ValueUnpackContext unpackContext, Object wrapperValue) {
        for (ParameterConvert parameterConvert : parameterConverts) {
            if (parameterConvert.canConvert(wrapperValue)) {
                return parameterConvert.convert(wrapperValue);
            }
        }
        return getRealValue(unpackContext, wrapperValue);
    }
}
