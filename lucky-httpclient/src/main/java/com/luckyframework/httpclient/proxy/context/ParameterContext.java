package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Parameter;

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
        setContextVar();
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
    public void setContextVar() {
        super.setContextVar();

        // 设置参数索引到SpEL运行时环境中
        getContextVar().addRootVariable(_PARAM_CONTEXT_INDEX_, index);

        // 拆包后的值
        LazyValue<Object> realLazyValue = LazyValue.of(this::getValue);
        // 原始参数值
        LazyValue<Object> sourceValue = LazyValue.of(this::doGetValue);
        getContextVar().addRootVariable(_VALUE_CONTEXT_VALUE_, realLazyValue);
        getContextVar().addRootVariable(_$VALUE_CONTEXT_SOURCE_VALUE$_, sourceValue);

        // 设置参数信息到父上下文中
        SpELVariate mrpw = getParentContext().getContextVar();
        mrpw.addRootVariable(getName(), realLazyValue);
        mrpw.addRootVariable("p" + index, realLazyValue);
        mrpw.addRootVariable("$" + getName(), sourceValue);
        mrpw.addRootVariable("$p" + index, sourceValue);


        // 设置参数类型信息到父上下文中
        LazyValue<ResolvableType> lazyType = LazyValue.of(this::getType);
        mrpw.addRootVariable("$" + getName() + "_type", lazyType);
        mrpw.addRootVariable("$p" + index + "_type", lazyType);
    }

}
