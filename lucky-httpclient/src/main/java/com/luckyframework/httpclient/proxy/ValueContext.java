package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.AnnotatedElement;
import java.util.Map;

/**
 * 值上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/4 00:09
 */
public abstract class ValueContext extends Context {

    public ValueContext(AnnotatedElement currentAnnotatedElement) {
        super(currentAnnotatedElement);
    }

    public boolean isNullValue() {
        return getValue() == null;
    }

    public boolean isResponseProcessorInstance(){
        return getValue() instanceof ResponseProcessor;
    }

    public boolean isResourceType() {
        return HttpExecutor.isResourceParam(getType());
    }

    public boolean isBodyObjectInstance() {
        return getValue() instanceof BodyObject;
    }

    public boolean isSimpleBaseType() {
        return ClassUtils.isSimpleBaseType(getType().getRawClass());
    }

    public boolean isMapInstance() {
        return getValue() instanceof Map;
    }

    public boolean isIterableInstance() {
        return ContainerUtils.isIterable(getValue());
    }

    public abstract void setName(String name);

    public abstract String getName();

    public abstract Object getValue();

    public abstract ResolvableType getType();
}
