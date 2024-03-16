package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.annotations.ArgHandle;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.NotHttpParam;
import com.luckyframework.httpclient.proxy.annotations.ValueUnpack;
import com.luckyframework.httpclient.proxy.spel.SpELUtils;
import com.luckyframework.httpclient.proxy.unpack.ContextValueUnpack;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.VALUE_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.VALUE_CONTEXT_NAME;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.VALUE_CONTEXT_TYPE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.VALUE_CONTEXT_VALUE;

/**
 * 值上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/4 00:09
 */
public abstract class ValueContext extends Context {

    private Object realValue;
    private final AtomicBoolean isAnalyze = new AtomicBoolean(false);

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

    public boolean isBinaryType() {
        return HttpExecutor.isBinaryParam(getType());
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
        if (isAnalyze.compareAndSet(false, true)) {
            realValue = doGetValue();
            if (isAnnotatedCheckParent(ValueUnpack.class)) {
                ValueUnpack vupAnn = toAnnotation(getMergedAnnotationCheckParent(ValueUnpack.class), ValueUnpack.class);
                ContextValueUnpack contextValueUnpack = generateObject(vupAnn.valueUnpack());
                realValue = contextValueUnpack.getRealValue(realValue, vupAnn);
            }
            if (isAnnotatedCheckParent(ArgHandle.class)) {

                Annotation ann = getSameAnnotationCombined(ArgHandle.class);
                ArgHandle argHandleAnn = toAnnotation(ann, ArgHandle.class);

                realValue = parseExpression(argHandleAnn.value(), arg -> {
                    arg.extractKeyValue(VALUE_CONTEXT, this);
                    arg.extractKeyValue(VALUE_CONTEXT_NAME, getName());
                    arg.extractKeyValue(VALUE_CONTEXT_TYPE, getType());
                    arg.extractKeyValue(VALUE_CONTEXT_VALUE, realValue);
                });

            }
        }
        return this.realValue;
    }

    public abstract ResolvableType getType();

    public abstract Object doGetValue();

    @Override
    public SpELUtils.ExtraSpELArgs getSpELArgs() {
        return getParentContext().getSpELArgs();
    }

    @Override
    public <T> T parseExpression(String expression, ResolvableType returnType, Consumer<SpELUtils.ExtraSpELArgs> argSetter) {
        return getParentContext().parseExpression(expression, returnType, argSetter);
    }

    public boolean notHttpParam() {
        boolean hasNotHttpParamAnn = isAnnotatedCheckParent(NotHttpParam.class);
        boolean hasDynamicParam = isAnnotated(DynamicParam.class);
        return !hasDynamicParam && hasNotHttpParamAnn;
    }
}
