package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.BodyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.NotHttpParam;
import com.luckyframework.httpclient.proxy.annotations.ValueUnpack;
import com.luckyframework.httpclient.proxy.spel.ValueSpaceConstant;
import com.luckyframework.httpclient.proxy.unpack.ContextValueUnpack;
import com.luckyframework.httpclient.proxy.unpack.ValueUnpackContext;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.ResolvableType;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CURRENT_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_VALUE_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName._VALUE_CONTEXT_NAME_;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName._VALUE_CONTEXT_TYPE_;

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

    public boolean isResourceType() {
        return HttpExecutor.isResourceParam(getType());
    }

    public boolean isResourceValue() {
        return HttpExecutor.isResourceParam(getValue());
    }

    public boolean isBinaryType() {
        return HttpExecutor.isBinaryParam(getType());
    }

    public boolean isBodyObjectInstance() {
        Object value = getValue();
        return (value instanceof BodyObject) || (value instanceof BodyObjectFactory);
    }

    public boolean isSimpleBaseType() {
        Object value = getValue();
        Class<?> checkType = value == null ? getType().toClass() : value.getClass();
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

    public Object getValue() {
        if (isAnalyze.compareAndSet(false, true)) {
            realValue = doGetValue();

            // 使用ParameterConvert进行转换
            realValue = ContextValueUnpack.parameterConvert(this, realValue);

            // 查找注解转换器进行转换
            List<ValueUnpack> valueUnpackList = findNestCombinationAnnotationsCheckParent(ValueUnpack.class);
            Set<Class<?>> contextValueUnpackClassSet = new HashSet<>();
            for (ValueUnpack vupAnn : valueUnpackList) {
                ContextValueUnpack contextValueUnpack = generateObject(vupAnn.valueUnpack(), vupAnn.unpackClass(), ContextValueUnpack.class);
                Class<? extends ContextValueUnpack> valueUnpackClass = contextValueUnpack.getClass();
                if (!contextValueUnpackClassSet.contains(valueUnpackClass)) {
                    realValue = contextValueUnpack.getRealValue(new ValueUnpackContext(this, vupAnn), realValue);
                    contextValueUnpackClassSet.add(valueUnpackClass);
                }
            }
        }
        return this.realValue;
    }

    public abstract ResolvableType getType();

    public abstract Object doGetValue();

    @Override
    public void initContext() {
        Map<String, Object> immutableMap = new HashMap<>(4);
        immutableMap.put($_VALUE_CONTEXT_$, this);
        immutableMap.put($_CURRENT_CONTEXT_$, this);
        immutableMap.put(_VALUE_CONTEXT_NAME_, LazyValue.of(this::getName));
        immutableMap.put(_VALUE_CONTEXT_TYPE_, LazyValue.of(this::getType));
        getContextVar().addRootVariable(ValueSpaceConstant.VALUE_CONTENT_SPACE, Collections.unmodifiableMap(immutableMap));
    }

    /**
     * 判断某个方法参数是否为显式的HTTP参数
     * <pre>
     *     同时满足以下条件的即为显式HTTP参数
     *     1.被{@link DynamicParam @DynamicParam}注解标注
     *     2.没有被{@link NotHttpParam @NotHttpParam}注解标注
     * </pre>
     *
     * @return 是HTTP参数返回<b>true</b>, 不是HTTP参数返回<b>false</b>
     */
    public boolean isExplicitHttpParam() {
        if (isAnnotated(NotHttpParam.class)) {
            return false;
        }
        if (isAnnotated(DynamicParam.class)) {
            return true;
        }
        boolean hasNotHttpParamAnn = isAnnotatedCheckParent(NotHttpParam.class);
        boolean hasDynamicParam = isAnnotatedCheckParent(DynamicParam.class);
        return hasDynamicParam && !hasNotHttpParamAnn;
    }

    /**
     * 鉴定某个HTTP参数是否一定不是一个HTTP参数
     * <pre>
     *      被{@link NotHttpParam @NotHttpParam}注解标注的参数一定不是HTTP参数
     * </pre>
     */
    public boolean notHttpParam() {
        return isAnnotatedCheckParent(NotHttpParam.class);
    }

}
