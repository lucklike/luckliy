package com.luckyframework.httpclient.proxy.spel.hook;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorFactory;
import com.luckyframework.httpclient.proxy.async.Model;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.httpclient.proxy.spel.hook.callback.HookExecutorException;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Hook组，用于管理某个Class中的所有不同生命周期的Hook
 */
public class HookGroup {

    private static final Logger log = LoggerFactory.getLogger(HookGroup.class);

    /**
     * 生命周期-Hook参数Map
     */
    private final Map<Lifecycle, List<Param>> hookParamMap;

    /**
     * 私有构造函数
     *
     * @param namespace 命名空间
     * @param clazz     Class
     */
    private HookGroup(String namespace, Class<?> clazz) {
        this.hookParamMap = new LinkedHashMap<>();
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
     * 是否存在Hook
     *
     * @return 是否存在Hook
     */
    public boolean hasHook() {
        return !hookParamMap.isEmpty();
    }

    /**
     * 运行指定生命周期下的所有Hook函数
     *
     * @param context        上下文对象
     * @param lifecycle      生命周期
     * @param errorInterrupt 发生异常时是否中断后续流程
     */
    public void useHook(Context context, Lifecycle lifecycle, boolean errorInterrupt) {
        List<Param> paramList = hookParamMap.get(lifecycle);
        if (ContainerUtils.isNotEmptyCollection(paramList)) {
            for (Param param : paramList) {
                selectionModeUseOneHook(context, param, errorInterrupt);
            }
        }
    }

    /**
     * 使用指定的模式来执行一个Hook函数
     *
     * @param context 上下文对象
     * @param param   执行参数
     */
    private void selectionModeUseOneHook(Context context, Param param, boolean errorInterrupt) {
        try {
            // 校验enable属性，结果为false时不将执行该回调
            String enable = context.toAnnotation(param.getAnnotation(), Hook.class).enable();
            if (StringUtils.hasText(enable) && !context.parseExpression(enable, boolean.class)) {
                return;
            }
            if (param.isAsync()) {
                getHookExecutor(context, param).execute(() -> useOneHook(context, param));
            } else {
                useOneHook(context, param);
            }
        } catch (Throwable e) {
            if (errorInterrupt && param.isErrorInterrupt()) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new HookExecutorException(e);
            } else {
                String hookInfo;
                AnnotatedElement source = param.getNamespaceWrap().getSource();
                if (source instanceof Method) {
                    Method m = (Method) source;
                    hookInfo = StringUtils.format("Method {}.{}()", m.getDeclaringClass().getSimpleName(), m.getName());
                } else if (source instanceof Field) {
                    Field f = (Field) source;
                    hookInfo = StringUtils.format("Field {}.{}", f.getDeclaringClass().getSimpleName(), f.getName());
                } else {
                    hookInfo = source.toString();
                }
                log.error("Hook 【'{}'】 executor failure.", hookInfo, e);
            }
        }
    }

    /**
     * 获取异步执行Hook的线程池
     *
     * @param context 上下文对象
     * @param param   执行参数
     * @return 异步执行Hook的线程池
     */
    private AsyncTaskExecutor getHookExecutor(Context context, Param param) {
        return param.getOrCreateExecutor(() -> {
            HttpClientProxyObjectFactory proxyFactory = context.getHttpProxyFactory();

            // 确认异步模型
            Model model = param.getModel() == Model.USE_COMMON ? proxyFactory.getAsyncModel() : param.getModel();

            String poolInfo = param.getPoolName();
            if (StringUtils.hasText(poolInfo)) {
                return AsyncTaskExecutorFactory.create(context.createExecutor(poolInfo), model);
            }

            return AsyncTaskExecutorFactory.createDefault(proxyFactory, model);
        });
    }

    /**
     * 执行某一个Hook函数
     *
     * @param context 上下文对象
     * @param param   执行参数
     */
    private void useOneHook(Context context, Param param) {
        Function<Context, HookHandler> hookHandlerFunction = param.getHookHandlerFunction();
        HookContext hookContext = new HookContext(context, param.getAnnotation());
        hookHandlerFunction.apply(context).handle(hookContext, param.getNamespaceWrap());
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

        // 静态属性钩子
        for (Field staticField : ASMUtil.getAllStaticFieldOrder(clazz)) {
            Hook hookAnn = AnnotationUtils.findMergedAnnotation(staticField, Hook.class);
            if (hookAnn != null) {
                List<Param> paramList = hookParamMap.computeIfAbsent(hookAnn.lifecycle(), _k -> new ArrayList<>());
                paramList.add(createHookParam(staticField, hookAnn, namespace));
            }
        }

        // 静态方法钩子
        for (Method staticMethod : ASMUtil.getAllStaticMethodOrder(clazz)) {
            Hook hookAnn = AnnotationUtils.findMergedAnnotation(staticMethod, Hook.class);
            if (hookAnn != null) {
                List<Param> paramList = hookParamMap.computeIfAbsent(hookAnn.lifecycle(), _k -> new ArrayList<>());
                paramList.add(createHookParam(staticMethod, hookAnn, namespace));
            }
        }

    }

    private Param createHookParam(AnnotatedElement annotatedElement, Hook hookAnn, String namespace) {
        AsyncHook asyncHook = AnnotationUtils.sameAnnotationCombined(annotatedElement, AsyncHook.class);
        boolean async = false;
        String poolName = null;
        Model model = Model.JAVA_THREAD;
        if (asyncHook != null) {
            async = asyncHook.async();
            poolName = asyncHook.value();
            model = asyncHook.model();
        }
        return new Param(
                async,
                poolName,
                model,
                namespace,
                hookAnn.errorInterrupt(),
                hookAnn,
                annotatedElement,
                context -> context.generateObject(hookAnn.hookHandle(), hookAnn.hookHandleClass(), HookHandler.class)
        );
    }


    /**
     * 内部类，用于存储Hook运行时所需要的参数
     */
    static class Param {

        /**
         * 是否异步执行
         */
        private final boolean async;

        /**
         * 用于异步执行的线程池名称
         */
        private final String poolName;

        /**
         * 异步模型
         */
        private final Model model;

        /**
         * 发生异常时是否中断后续流程
         */
        private final boolean errorInterrupt;

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
         * 用于执行异步任务的执行器
         */
        private AsyncTaskExecutor executor;

        /**
         * Hook参数构造器
         *
         * @param async               是否为异步钩子
         * @param poolName            用于异步执行的线程池名称
         * @param model               异步模型
         * @param namespace           命名空间
         * @param errorInterrupt      是否忽略执行过程中的错误
         * @param annotation          Hook系列注解实例
         * @param source              源对象
         * @param hookHandlerFunction 用于获取{@link HookHandler}实例的Function函数
         */
        public Param(boolean async,
                     String poolName,
                     Model model,
                     String namespace,
                     boolean errorInterrupt,
                     Annotation annotation,
                     AnnotatedElement source,
                     Function<Context, HookHandler> hookHandlerFunction
        ) {
            this.async = async;
            this.poolName = poolName;
            this.model = model;
            this.errorInterrupt = errorInterrupt;
            this.annotation = annotation;
            this.hookHandlerFunction = hookHandlerFunction;
            this.namespaceWrap = NamespaceWrap.wrap(namespace, source);
        }

        /**
         * 钩子函数是否设置为异步执行
         *
         * @return 钩子函数是否设置为异步执行
         */
        public boolean isAsync() {
            return async;
        }

        /**
         * 获取用于异步执行的线程池名称
         *
         * @return 用于异步执行的线程池名称
         */
        public String getPoolName() {
            return poolName;
        }

        /**
         * 获取异步模型
         *
         * @return 异步模型
         */
        public Model getModel() {
            return model;
        }

        /**
         * 发生异常时是否中断后续流程
         *
         * @return 发生异常时是否中断后续流程
         */
        public boolean isErrorInterrupt() {
            return errorInterrupt;
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

        /**
         * 获取异步执行器
         *
         * @return 异步执行器
         */
        public synchronized AsyncTaskExecutor getOrCreateExecutor(Supplier<AsyncTaskExecutor> executorSupplier) {
            if (executor == null) {
                executor = executorSupplier.get();
            }
            return executor;
        }
    }
}
