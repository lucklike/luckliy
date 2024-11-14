package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.spel.StaticClassEntry;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.spel.LazyValue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS_CONTEXT;

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
        MapRootParamWrapper contextVar = getContextVar();
        contextVar.addRootVariable(CLASS_CONTEXT, LazyValue.of(this));
        contextVar.addRootVariable(CLASS, LazyValue.of(this::getCurrentAnnotatedElement));
        contextVar.importPackage(getCurrentAnnotatedElement().getPackage().getName());

        StaticClassEntry classEntry = StaticClassEntry.create(getCurrentAnnotatedElement());
        contextVar.addVariables(classEntry.getAllStaticMethods());
        // 导入变量
        StaticClassEntry.Variable variables = classEntry.getAllVariables();
        // 导入字面量
        contextVar.addRootVariables(variables.getRootVarLitMap());
        contextVar.addVariables(variables.getVarLitMap());

        // 导入Root变量
        variables.getRootVarMap().forEach((k, v) -> {
            String key = parseExpression(k);
            Object value = getParsedValue(v);
            contextVar.addRootVariable(key, value);
        });

        // 导入普通变量
        variables.getVarMap().forEach((k, v) -> {
            String key = parseExpression(k);
            Object value = getParsedValue(v);
            contextVar.addVariable(key, value);
        });
        super.setContextVar();
    }

    @Override
    protected void importSpELVar() {
        importSpELVarByClass(getCurrentAnnotatedElement());
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
