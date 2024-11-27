package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.var.VarScope;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.spel.LazyValue;

import java.lang.reflect.Field;

import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CLASS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CLASS_CONTEXT_$;


/**
 * 类级别的上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/21 19:41
 */
public final class ClassContext extends Context {

    /**
     * 类上下文构造器
     *
     * @param currentClass 当前类
     */
    public ClassContext(Class<?> currentClass) {
        super(currentClass);
    }

    @Override
    public Class<?> getCurrentAnnotatedElement() {
        return (Class<?>) super.getCurrentAnnotatedElement();
    }

    /**
     * 获取类上的所有属性上下文
     *
     * @param classObject 类对象
     * @return 属性上下文集合
     */
    public FieldContext[] getFieldContexts(Object classObject) {
        Field[] fields = ClassUtils.getAllFields(getCurrentAnnotatedElement());
        FieldContext[] fieldContexts = new FieldContext[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldContexts[i] = new FieldContext(this, fields[i], FieldUtils.getValue(classObject, fields[i]));
        }
        return fieldContexts;
    }

    @Override
    public void setContextVar() {
        SpELVariate contextVar = getContextVar();
        contextVar.addRootVariable($_CLASS_CONTEXT_$, LazyValue.of(this));
        contextVar.addRootVariable($_CLASS_$, LazyValue.of(this::getCurrentAnnotatedElement));
        contextVar.addHook(getCurrentAnnotatedElement());

        Class<?> currentClass = getCurrentAnnotatedElement();

        // 加载由@SpELImpoet注解导入的SpEL变量、包 -> root()、var()、rootLit()、varLit()、pack()
        loadSpELImportAnnVarFunFindParent(currentClass);
        // 加载由@SpELImpoet注解导入的类 -> value()
        loadSpELImportAnnImportClassesVarFindParent(this, this, currentClass, VarScope.DEFAULT, VarScope.CLASS);

        // 加载当前类中的SpEL变量、函数、包
        importClassPackage(currentClass);
        loadClassSpELFun(currentClass);
        loadClassSpELVar(this, currentClass, VarScope.CLASS, VarScope.DEFAULT);
        useHook(Lifecycle.CLASS);
    }


    private void loadSpELImportAnnVarFunFindParent(Class<?> clazz) {
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

    void loadSpELImportAnnImportClassesVarFindParent(Context storeContext, Context execContext, Class<?> clazz, VarScope... scopes) {
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
