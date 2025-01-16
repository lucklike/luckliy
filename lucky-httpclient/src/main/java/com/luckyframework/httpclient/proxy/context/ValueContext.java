package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.NotHttpParam;
import com.luckyframework.httpclient.proxy.annotations.ValueUnpack;
import com.luckyframework.httpclient.proxy.unpack.ContextValueUnpack;
import com.luckyframework.httpclient.proxy.unpack.ValueUnpackContext;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.ResolvableType;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public Object getValue() {
        if (isAnalyze.compareAndSet(false, true)) {
            realValue = doGetValue();

            // 查找到所有的ValueUnpack注解进行参数处理
            List<ValueUnpack> valueUnpackList = findNestCombinationAnnotationsCheckParent(ValueUnpack.class);
            for (ValueUnpack vupAnn : valueUnpackList) {
                ContextValueUnpack contextValueUnpack = generateObject(vupAnn.valueUnpack(), vupAnn.unpackClass(), ContextValueUnpack.class);
                realValue = contextValueUnpack.getRealValue(new ValueUnpackContext(this, vupAnn), realValue);
            }
        }
        return this.realValue;
    }

    public abstract ResolvableType getType();

    public abstract Object doGetValue();

    @Override
    public void setContextVar() {
        getContextVar().addRootVariable($_VALUE_CONTEXT_$, LazyValue.of(this));
        getContextVar().addRootVariable(_VALUE_CONTEXT_NAME_, LazyValue.of(this::getName));
        getContextVar().addRootVariable(_VALUE_CONTEXT_TYPE_, LazyValue.of(this::getType));
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
