package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;

import java.util.Map;

/**
 * 当前正在操作的参数
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 06:25
 */
public class CurrentlyOperationParam {

    private final String name;
    private final Object value;
    private final ResolvableType type;

    public CurrentlyOperationParam(String name, Object value, ResolvableType type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public ResolvableType getType() {
        return type;
    }

    public boolean isNull() {
        return value == null;
    }

    public boolean isResponseProcessorInstance(){
        return value instanceof ResponseProcessor;
    }

    public boolean isResourceType() {
        return HttpExecutor.isResourceParam(type);
    }

    public boolean isBodyObjectInstance() {
        return value instanceof BodyObject;
    }

    public boolean isSimpleBaseType() {
        return ClassUtils.isSimpleBaseType(type.getRawClass());
    }

    public boolean isMapInstance() {
        return value instanceof Map;
    }

    public boolean isIterableInstance() {
        return ContainerUtils.isIterable(value);
    }
}
