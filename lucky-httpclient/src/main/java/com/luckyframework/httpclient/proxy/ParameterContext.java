package com.luckyframework.httpclient.proxy;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Parameter;

/**
 * 参数上下文
 *
 * @author 、 @version 1.0.0
 * @date 2023/9/21 13:04
 */
public class ParameterContext extends ValueContext {

    private final MethodContext methodContext;

    private String name;
    private final Object value;
    private final int index;
    private final ResolvableType type;

    public ParameterContext(MethodContext methodContext, String paramName, Object value, int index) {
        super(methodContext.getCurrentAnnotatedElement().getParameters()[index]);
        this.methodContext = methodContext;
        this.name = paramName;
        this.value = value;
        this.index = index;
        this.type = ResolvableType.forMethodParameter(methodContext.getCurrentAnnotatedElement(), index);
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

    public Object getValue() {
        return value;
    }

    public ResolvableType getType(){
        return this.type;
    }

    @Override
    public Parameter getCurrentAnnotatedElement() {
        return (Parameter) super.getCurrentAnnotatedElement();
    }

}
