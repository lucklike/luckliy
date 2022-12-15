package com.luckyframework.conversion;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 相互转换接口，可以将T类型转化为S类型，同时也支持将S类型转化为T类型
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/29 08:41
 */
public interface Interconversion<T, S> {

    String TO_TARGET        = "toTarget";
    String TO_SOURCE        = "toSource";
    String TO_TARGET_LIST   = "toTargetList";
    String TO_SOURCE_LIST   = "toSourceList";
    String TO_TARGET_SET   = "toTargetSet";
    String TO_SOURCE_SET   = "toSourceSet";
    String TO_TARGET_ARRAY  = "toTargetArray";
    String TO_SOURCE_ARRAY  = "toSourceArray";


    String GET_TARGET_CONVERT   = "getTargetConversion";
    String GET_SOURCE_CONVERT   = "getSourceConversion";

    /**
     * source转target
     * @param source source对象
     * @return target对象
     */
    T toTarget(S source);

    /**
     * target转source
     * @param target target对象
     * @return source对象
     */
    S toSource(T target);

    /**
     * sources集合转targetList
     * @param sources  sources集合
     * @return targetList
     */
    default List<T> toTargetList(Collection<S> sources){
        return null;
    }

    /**
     * source数组转targetList
     * @param sources  source数组
     * @return targetList
     */
    default List<T> toTargetList(S[] sources){
        return null;
    }

    /**
     * targets集合转sourceList
     * @param targets targets集合
     * @return sourceList
     */
    default List<S> toSourceList(Collection<T> targets){
        return null;
    }

    /**
     * target数组转sourceList
     * @param targets targets数组
     * @return sourceList
     */
    default List<S> toSourceList(T[] targets){
        return null;
    }

    /**
     * sources集合转targetSet
     * @param sources  sources集合
     * @return targetSet
     */
    default Set<T> toTargetSet(Collection<S> sources){
        return null;
    }

    /**
     * sources数组转targetSet
     * @param sources  sources数组
     * @return targetSet
     */
    default Set<T> toTargetSet(S[] sources){
        return null;
    }

    /**
     * targets集合转sourceSet
     * @param targets targets集合
     * @return sourceSet
     */
    default Set<S> toSourceSet(Collection<T> targets){
        return null;
    }

    /**
     * targets数组转sourceSet
     * @param targets targets数组
     * @return sourceSet
     */
    default Set<S> toSourceSet(T[] targets){
        return null;
    }

    /**
     * sources集合转targets数组
     * @param sources targets集合
     * @return targets数组
     */
    default T[] toTargetArray(Collection<S> sources){
        return null;
    }

    /**
     * sources数组转targets数组
     * @param sources targets数组
     * @return targets数组
     */
    default T[] toTargetArray(S[] sources){
        return null;
    }

    /**
     * targets集合转source数组
     * @param targets targets集合
     * @return source数组
     */
    default S[] toSourceArray(Collection<T> targets){
        return null;
    }

    /**
     * targets数组转source数组
     * @param targets targets数组
     * @return source数组
     */
    default S[] toSourceArray(T[] targets){
        return null;
    }


    /**
     * 获取可以将source对象转化为target的转换器
     * @return 可以将source对象转化为target的转换器
     */
    default ConversionService<T, S> getTargetConversion(){
        return null;
    }

    /**
     * 获取可以将target对象转化为source的转换器
     * @return 可以将target对象转化为source的转换器
     */
    default ConversionService<S, T> getSourceConversion(){
        return null;
    }
}
