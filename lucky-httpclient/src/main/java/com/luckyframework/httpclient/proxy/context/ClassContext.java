package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.StaticClassEntry;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.spel.LazyValue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.*;

/**
 * 类级别的上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/21 19:41
 */
public class ClassContext extends Context {
    public ClassContext(Class<?> currentClass) {
        super(currentClass);
    }

    @Override
    public Class<?> getCurrentAnnotatedElement() {
        return (Class<?>) super.getCurrentAnnotatedElement();
    }

    public List<FieldContext> getFieldContexts(Object classObject) {
        Field[] fields = ClassUtils.getAllFields(getCurrentAnnotatedElement());
        List<FieldContext> fieldContexts = new ArrayList<>(fields.length);
        for (Field field : fields) {
            fieldContexts.add(new FieldContext(this, field, FieldUtils.getValue(classObject, field)));
        }
        return fieldContexts;
    }

    @Override
    public void setContextVar() {
        getContextVar().addRootVariable(CLASS_CONTEXT, LazyValue.of(this));
        getContextVar().addRootVariable(CLASS, LazyValue.of(this::getCurrentAnnotatedElement));
        getContextVar().importPackage(getCurrentAnnotatedElement().getPackage().getName());
        getContextVar().addVariables(StaticClassEntry.create(getCurrentAnnotatedElement()).getAllStaticMethods());
        super.setContextVar();
    }

    @Override
    protected void importSpELVar() {
        importSpELVarByClass(getCurrentAnnotatedElement());
        super.importSpELVar();
    }

    private void importSpELVarByClass(Class<?> aClass) {
        if (aClass == null || aClass == Object.class) {
            return;
        }
        Class<?> superclass = aClass.getSuperclass();
        importSpELVarByClass(superclass);
        for (Class<?> anInterface : aClass.getInterfaces()) {
            importSpELVarByClass(anInterface);
        }
        importSpELVarByAnnotatedElement(aClass);
    }

}
