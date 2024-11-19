package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.spel.var.VarScope;
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

        Class<?> currentClass = getCurrentAnnotatedElement();

        // 加载由@SpELImpoet注解导入的SpEL变量、包 -> root()、var()、rootLit()、varLit()、pack()
        loadSpELImportAnnVarFunFindParent(currentClass);
        // 加载由@SpELImpoet注解导入的类 -> value()
        loadSpELImportAnnImportClassesVarFindParent(this, this, currentClass, VarScope.DEFAULT, VarScope.CLASS);

        // 加载当前类中的SpEL变量、函数、包
        importClassPackage(currentClass);
        loadClassSpELFun(currentClass);
        loadClassSpELVar(this, currentClass, VarScope.CLASS, VarScope.DEFAULT);
        super.setContextVar();
    }


    protected void loadSpELImportAnnVarFunFindParent(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        Class<?> superclass = clazz.getSuperclass();
        loadSpELImportAnnVarFunFindParent(superclass);
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            loadSpELImportAnnVarFunFindParent(interfaceClass);
        }
        loadSpELImportAnnVarFun(clazz);
    }

    protected void loadSpELImportAnnImportClassesVarFindParent(Context storeContext, Context execContext, Class<?> clazz, VarScope... scopes) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        Class<?> superclass = clazz.getSuperclass();
        loadSpELImportAnnImportClassesVarFindParent(storeContext, execContext, superclass, scopes);
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            loadSpELImportAnnImportClassesVarFindParent(storeContext, execContext, interfaceClass, scopes);
        }
        loadSpELImportAnnImportClassesVar(storeContext, execContext, clazz, scopes);
    }

}
