package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.ValueUnpack;
import com.luckyframework.httpclient.proxy.unpack.ContextValueUnpack;
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

    private Object realValue;
    private boolean isAnalyze = false;

    public ValueContext(AnnotatedElement currentAnnotatedElement) {
        super(currentAnnotatedElement);
    }

    public boolean isNullValue() {
        return getValue() == null;
    }

    public boolean isResponseProcessorInstance() {
        return getValue() instanceof ResponseProcessor;
    }

    public boolean isResourceType() {
        return HttpExecutor.isResourceParam(getType());
    }

    public boolean isBodyObjectInstance() {
        return getValue() instanceof BodyObject;
    }

    public boolean isSimpleBaseType() {
        Object value = getValue();
        Class<?> checkType = value == null ? getType().getRawClass() : value.getClass();
        return ClassUtils.isSimpleBaseType(checkType);
    }

    public boolean isMapInstance() {
        return getValue() instanceof Map;
    }

    public boolean isIterableInstance() {
        return ContainerUtils.isIterable(getValue());
    }

    public abstract void setName(String name);

    public abstract String getName();

    public synchronized Object getValue() {
        if (!isAnalyze) {
            realValue = doGetValue();
            if (isAnnotatedCheckParent(ValueUnpack.class)) {
                ValueUnpack vupAnn = toAnnotation(getMergedAnnotationCheckParent(ValueUnpack.class), ValueUnpack.class);
                ContextValueUnpack contextValueUnpack =
                        (ContextValueUnpack) HttpClientProxyObjectFactory.getObjectCreator().newObject(vupAnn.valueUnpack());
                realValue = contextValueUnpack.getRealValue(realValue, vupAnn);
            }
            isAnalyze = true;
        }
        return this.realValue;
    }

    public abstract ResolvableType getType();

    public abstract Object doGetValue();
}
