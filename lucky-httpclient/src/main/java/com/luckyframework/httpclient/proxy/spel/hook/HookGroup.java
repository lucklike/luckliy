package com.luckyframework.httpclient.proxy.spel.hook;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.spel.Namespace;
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

/**
 * Hook组，用于管理某个Class中的所有不同生命周期的Hook
 */
public class HookGroup {

    /**
     * 生命周期-Hook参数Map
     */
    private final Map<Lifecycle, List<Param>> hookParamMap = new LinkedHashMap<>();

    /**
     * 私有构造函数
     *
     * @param namespace 命名空间
     * @param clazz     Class
     */
    private HookGroup(String namespace, Class<?> clazz) {
        initialize(namespace, clazz);
    }

    /**
     * 使用命名空间和Class来创建一个Hook组
     *
     * @param namespace 命名空间
     * @param clazz     Class
     * @return Hook组
     */
    public static HookGroup create(String namespace, Class<?> clazz) {
        return new HookGroup(namespace, clazz);
    }

    /**
     * 使用命名空间和Class来创建一个Hook组，使用默认的命名空间
     *
     * @param clazz Class
     * @return Hook组
     */
    public static HookGroup create(Class<?> clazz) {
        return create(null, clazz);
    }

    /**
     * 运行指定生命周期下的所有Hook函数
     *
     * @param lifecycle 生命周期
     * @param context   上下文对象
     */
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

    /**
     * 初始化方法
     * <pre>
     *  1.namespace: 如果传入了则使用传入的，没有则检测有没有{@link Namespace},有则使用注解上的，没有则使用默认的
     *  2.获取所有被{@link Hook}注解标注的静态方法，注册为静态方法Hook
     *  3.获取所有被{@link Hook}注解标注的静态属性，注册为静态属性Hook
     * </pre>
     *
     * @param namespace 命名空间
     * @param clazz     CLass
     */
    public void initialize(String namespace, Class<?> clazz) {

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

    /**
     * 内部类，用于存储Hook运行时所需要的参数
     */
    static class Param {


        /**
         * {@link Hook}系列注解
         */
        private final Annotation annotation;

        /**
         * 包含命名空间和源对象的包装类
         */
        private final NamespaceWrap namespaceWrap;

        /**
         * 用于获取{@link HookHandler}实例的Function函数
         */
        private final Function<Context, HookHandler> hookHandlerFunction;

        /**
         * Hook参数构造器
         *
         * @param namespace           命名空间
         * @param annotation          Hook系列注解实例
         * @param source              源对象
         * @param hookHandlerFunction 用于获取{@link HookHandler}实例的Function函数
         */
        public Param(String namespace, Annotation annotation, Object source, Function<Context, HookHandler> hookHandlerFunction) {
            this.annotation = annotation;
            this.hookHandlerFunction = hookHandlerFunction;
            this.namespaceWrap = NamespaceWrap.wrap(namespace, source);
        }

        /**
         * 获取包含命名空间和源对象的包装类
         *
         * @return 包含命名空间和源对象的包装类
         */
        public NamespaceWrap getNamespaceWrap() {
            return namespaceWrap;
        }

        /**
         * 获取{@link Hook}系列注解实例
         *
         * @return {@link Hook}系列注解
         */
        public Annotation getAnnotation() {
            return annotation;
        }

        /**
         * 用于获取{@link HookHandler}实例的Function函数
         *
         * @return 获取 {@link HookHandler}实例的Function函数
         */
        public Function<Context, HookHandler> getHookHandlerFunction() {
            return hookHandlerFunction;
        }
    }
}
