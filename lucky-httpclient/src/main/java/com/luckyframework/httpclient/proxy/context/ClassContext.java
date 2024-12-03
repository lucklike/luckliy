package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.spel.LazyValue;

import java.lang.reflect.Field;

import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CLASS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CLASS_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_HTTP_PROXY_FACTORY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_THIS_$;


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
        contextVar.addRootVariable($_HTTP_PROXY_FACTORY_$, LazyValue.of(this::getHttpProxyFactory));
        contextVar.addRootVariable($_THIS_$, LazyValue.of(this::getProxyObject));
        contextVar.addRootVariable($_CLASS_$, LazyValue.of(this::getCurrentAnnotatedElement));

        // 加载由@SpELImport导入的包、函数、变量和Hook
        Class<?> currentClass = getCurrentAnnotatedElement();
        loadSpELImportElementFindParent(currentClass);

        // 加载当前类中的包、函数、变量和Hook
        importClassPackage(currentClass);
        loadClassSpELFun(currentClass);
        loadHook(currentClass);

        useHook(Lifecycle.CLASS);
    }


    private void loadSpELImportElementFindParent(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        Class<?> superclass = clazz.getSuperclass();
        loadSpELImportElementFindParent(superclass);
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            loadSpELImportElementFindParent(interfaceClass);
        }
        loadSpELImportElement(clazz);
    }
}
