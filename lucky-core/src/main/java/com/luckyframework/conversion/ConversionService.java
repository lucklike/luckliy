package com.luckyframework.conversion;

import org.springframework.core.ResolvableType;

import java.util.Collection;
import java.util.List;

/**
 * 转化器接口，用于实现将某个类型转化为另一个类型的功能
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/3 03:24
 */
public interface ConversionService<T, S> {

    /**
     * 将原对象转化为目标对象
     * @param sourceObject 原对象
     * @return  目标对象
     */
    T conversion(S sourceObject);

    /**
     * 对于给定的目标类型和原类型，本转换器是否可以转换
     * @param targetType    目标类型
     * @param sourceType    原类型
     * @return  是否可以转换
     */
    boolean canConvert(ResolvableType targetType, ResolvableType sourceType);

    /**
     * 转换器的签名信息，转换器的唯一标识
     * @return 转换器的签名信息
     */
    String getSignature();

    /**
     * 添加另一个转换器
     * @param conversionService 其他的转换器
     */
    void addUseConversion(ConversionService<?,?> conversionService);

    /**
     * 添加一组转换器
     * @param conversionServices 其他的一组转换器
     */
    default void addUseConversions(List<ConversionService<?,?>> conversionServices){
        conversionServices.forEach(this::addUseConversion);
    }

    /**
     * 获取转换类型的真实Class类型
     * @param type 转换类型
     * @return 真实的Class类型
     */
    static Class<?> getConversionClass(ResolvableType type){
        Class<?> rawClass = type.getRawClass();
        if(rawClass !=null && rawClass.isArray()){
            return getConversionClass(type.getComponentType());
        }
        if(rawClass !=null && Collection.class.isAssignableFrom(rawClass)){
            return getConversionClass(type.getGeneric(0));
        }
        return rawClass;
    }
}
