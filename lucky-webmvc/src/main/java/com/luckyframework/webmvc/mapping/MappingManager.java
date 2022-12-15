package com.luckyframework.webmvc.mapping;

import com.luckyframework.webmvc.mapping.exceptions.MappingHandleException;
import com.luckyframework.webmvc.mapping.exceptions.MappingModelRegisterException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * 映射管理器
 */
public interface MappingManager {

    /** 注册一个请求处理器*/
    void registerMappingHandle(MappingHandle mappingHandle);

    /** 注册一个映射模型*/
    void registerMappingModel(MappingModel mappingModel) throws MappingModelRegisterException;

    /** 判断一个映射是否存在相对应的映射模型*/
    int match(Mapping mapping);

    /** 根据映射返回匹配的映射模型*/
    @NonNull
    MappingModel[] getMathMappingModel(Mapping mapping);

    /** 获取所有的请求模型*/
    @NonNull
    MappingModel[] getAllMappingModel();

    /** 获取所有的映射执行器*/
    @NonNull
    MappingHandle[] getAllMappingHandle();

    /** 处理一个映射并得到处理后的结果*/
    @Nullable
    Object handler(Mapping mapping) throws MappingHandleException;

}
