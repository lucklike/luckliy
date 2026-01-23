package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.AbstractCtrlMap;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.ValueSpaceConstant;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_PARAM_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName._$VALUE_CONTEXT_SOURCE_VALUE$_;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName._PARAM_CONTEXT_INDEX_;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName._VALUE_CONTEXT_VALUE_;


/**
 * 参数上下文
 *
 * @author 、 @version 1.0.0
 * @date 2023/9/21 13:04
 */
public final class ParameterContext extends ValueContext {

    private final MethodContext methodContext;

    private String name;
    private final Object value;
    private final int index;
    private final ResolvableType type;

    public ParameterContext(MethodContext methodContext, String paramName, Object value, int index) {
        super(methodContext.getCurrentAnnotatedElement().getParameters()[index]);
        setParentContext(methodContext);
        this.methodContext = methodContext;
        this.name = paramName;
        this.value = value;
        this.index = index;
        this.type = ResolvableType.forMethodParameter(methodContext.getCurrentAnnotatedElement(), index);
        this.initContext();
    }

    public MethodContext getMethodContext() {
        return methodContext;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Object doGetValue() {
        return value;
    }

    public ResolvableType getType() {
        return this.type;
    }

    @Override
    public Parameter getCurrentAnnotatedElement() {
        return (Parameter) super.getCurrentAnnotatedElement();
    }

    @Override
    @SuppressWarnings("all")
    public void initContext() {
        super.initContext();

        /*
            name        -> 最终参数值
            $name       -> 原始参数值
            $n          -> 第n个参数的最终值
            $$n         -> 第n个参数的原始值

            name::type  -> 名为name的参数的类型
            $n::type    -> 第n个参数的类型
         */

        // 拆包后的值
        LazyValue<Object> realLazyValue = LazyValue.of(this::getValue);
        // 原始参数值
        LazyValue<Object> sourceValue = LazyValue.of(this::doGetValue);
        // 参数类型
        LazyValue<ResolvableType> lazyType = LazyValue.of(this::getType);

        Map<String, Object> paramImmutableMap = new HashMap<>(4);
        paramImmutableMap.put(_PARAM_CONTEXT_INDEX_, index);
        paramImmutableMap.put(_VALUE_CONTEXT_VALUE_, realLazyValue);
        paramImmutableMap.put(_$VALUE_CONTEXT_SOURCE_VALUE$_, sourceValue);
        getContextVar().addRootVariable(ValueSpaceConstant.PARAM_CONTEXT_SPACE, Collections.unmodifiableMap(paramImmutableMap));


        // 设置参数信息到父上下文中
        SpELVariate mrpw = getParentContext().getContextVar();
        Map<String, Object> parentImmutableMap = new HashMap<>(8);
        parentImmutableMap.put($_PARAM_CONTEXT_$ + index, this);
        parentImmutableMap.put($_PARAM_CONTEXT_$ + getName(), this);
        parentImmutableMap.put(getName(), realLazyValue);
        parentImmutableMap.put("$" + index, realLazyValue);
        parentImmutableMap.put("$" + getName(), sourceValue);
        parentImmutableMap.put("$$" + index, sourceValue);
        parentImmutableMap.put(getName() + "::type", lazyType);
        parentImmutableMap.put("$" + index + "::type", lazyType);

        if (index == 0) {
            mrpw.addRootVariable(ValueSpaceConstant.METHOD_CONTEXT_ARGS_SPACE, parentImmutableMap);
        } else {
            ((Map) mrpw.getRoot().get(ValueSpaceConstant.METHOD_CONTEXT_ARGS_SPACE)).putAll(parentImmutableMap);
            if (index == methodContext.getArguments().length - 1) {
                ((AbstractCtrlMap) mrpw.getRoot()).toUnmodifiable(ValueSpaceConstant.METHOD_CONTEXT_ARGS_SPACE);
            }
        }
    }

}
