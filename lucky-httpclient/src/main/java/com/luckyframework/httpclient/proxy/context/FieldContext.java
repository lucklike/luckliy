package com.luckyframework.httpclient.proxy.context;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Field;

/**
 * 属性上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/30 01:39
 */
public class FieldContext extends ValueContext {

    private final ClassContext classContext;
    private String name;

    private final Object value;

    private final ResolvableType type;

    public FieldContext(ClassContext classContext, Field field, Object value) {
        super(field);
        setParentContext(classContext);
        this.classContext = classContext;
        this.name = field.getName();
        this.type = ResolvableType.forField(field);
        this.value = value;
    }

    public FieldContext(Field field, Object value) {
        this(new ClassContext(field.getDeclaringClass()), field, value);
    }

    public ClassContext getClassContext() {
        return classContext;
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
        return type;
    }
}
