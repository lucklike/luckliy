package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class HookGroup {

    private final Map<Lifecycle, List<Param>> hookParamMap = new LinkedHashMap<>();

    private HookGroup(String namespace, Class<?> clazz) {
        init(namespace, clazz);
    }

    public static HookGroup create(String namespace, Class<?> clazz) {
        return new HookGroup(namespace, clazz);
    }

    public static HookGroup create(Class<?> clazz) {
        return create(null, clazz);
    }

    public void useHook(Lifecycle lifecycle, Context context) {
        List<Param> paramList = hookParamMap.get(lifecycle);
        if (ContainerUtils.isNotEmptyCollection(paramList)) {
            for (Param param : paramList) {
                Function<Context, HookHandler> hookHandlerFunction = param.getHookHandlerFunction();
                HookContext hookContext = new HookContext(context, param.getAnnotation());
                hookHandlerFunction.apply(context).handle(hookContext, param.getNamespaceWrap());
            }
        }
    }

    public void init(String namespace, Class<?> clazz) {

        Assert.notNull(clazz, "clazz must not be null");

        // 解析获取namespace
        if (!StringUtils.hasText(namespace)) {
            Namespace namespaceAnn = AnnotationUtils.findMergedAnnotation(clazz, Namespace.class);
            if (namespaceAnn != null && StringUtils.hasText(namespaceAnn.value())) {
                namespace = namespaceAnn.value();
            }
        }

        // 静态方法钩子
        for (Method staticMethod : ClassUtils.getAllStaticMethod(clazz)) {
            Hook hookAnn = AnnotationUtils.findMergedAnnotation(staticMethod, Hook.class);
            if (hookAnn != null) {
                List<Param> paramList = hookParamMap.computeIfAbsent(hookAnn.lifecycle(), _k -> new ArrayList<>());
                paramList.add(new Param(namespace, hookAnn, staticMethod, context -> context.generateObject(hookAnn.hookHandle(), hookAnn.hookHandleClass(), HookHandler.class)));
            }
        }

        // 静态属性钩子
        for (Field staticField : ClassUtils.getAllStaticField(clazz)) {
            Hook hookAnn = AnnotationUtils.findMergedAnnotation(staticField, Hook.class);
            if (hookAnn != null) {
                List<Param> paramList = hookParamMap.computeIfAbsent(hookAnn.lifecycle(), _k -> new ArrayList<>());
                paramList.add(new Param(namespace, hookAnn, staticField, context -> context.generateObject(hookAnn.hookHandle(), hookAnn.hookHandleClass(), HookHandler.class)));
            }
        }
    }

    public static class Param {

        private final Annotation annotation;
        private final NamespaceWrap namespaceWrap;
        private final Function<Context, HookHandler> hookHandlerFunction;

        public Param(String namespace, Annotation annotation, Object source, Function<Context, HookHandler> hookHandlerFunction) {
            this.annotation = annotation;
            this.hookHandlerFunction = hookHandlerFunction;
            this.namespaceWrap = NamespaceWrap.wrap(namespace, source);
        }

        public NamespaceWrap getNamespaceWrap() {
            return namespaceWrap;
        }

        public Annotation getAnnotation() {
            return annotation;
        }

        public Function<Context, HookHandler> getHookHandlerFunction() {
            return hookHandlerFunction;
        }
    }
}
