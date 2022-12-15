package com.luckyframework.webmvc.mapping;

/**
 * 映射处理器，处理一个请求模型
 */
public interface MappingHandle {

    default Mapping handleBefore(Mapping mapping){
        return mapping;
    }

    Object handler(MappingModel mappingModel);

    default Object handleAfter(Mapping mapping,Object result){
        return result;
    }

}
