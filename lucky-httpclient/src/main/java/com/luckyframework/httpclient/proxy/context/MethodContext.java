package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.AsyncExecutor;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;
import com.luckyframework.httpclient.proxy.annotations.RetryMeta;
import com.luckyframework.httpclient.proxy.annotations.RetryProhibition;
import com.luckyframework.httpclient.proxy.annotations.Wrapper;
import com.luckyframework.httpclient.proxy.creator.AbstractObjectCreator;
import com.luckyframework.httpclient.proxy.creator.Generate;
import com.luckyframework.httpclient.proxy.destroy.DestroyContext;
import com.luckyframework.httpclient.proxy.destroy.DestroyHandle;
import com.luckyframework.httpclient.proxy.destroy.DestroyMeta;
import com.luckyframework.httpclient.proxy.dynamic.DynamicParamLoader;
import com.luckyframework.httpclient.proxy.exeception.WrapperMethodInvokeException;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformer;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformerChain;
import com.luckyframework.httpclient.proxy.retry.RetryActuator;
import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;
import com.luckyframework.httpclient.proxy.spel.InternalVarName;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.statics.StaticParamLoader;
import com.luckyframework.spel.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_ARGS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_THROWABLE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$ASYNC_EXECUTOR$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_COUNT$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_DECIDER_FUNCTION$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_RUN_BEFORE_RETRY_FUNCTION$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_SWITCH$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_TASK_NAME$__;


/**
 * 方法上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/21 13:01
 */
public final class MethodContext extends Context implements MethodMetaAcquireAbility {

    private static final Logger log = LoggerFactory.getLogger(MethodContext.class);

    /**
     * 方法元信息上下文
     */
    private final MethodMetaContext metaContext;

    /**
     * 参数值数值
     */
    private final Object[] arguments;

    /**
     * 参数上下文数组
     */
    private final ParameterContext[] parameterContexts;

    /**
     * 方法上下文构造方法
     *
     * @param methodMetaContext 方法元信息上下文对象
     * @param arguments         参数列表
     * @throws IOException 构造过程中可能会出现IO异常
     */
    public MethodContext(MethodMetaContext methodMetaContext, Object[] arguments) throws IOException {
        super(methodMetaContext.getCurrentAnnotatedElement());
        this.metaContext = methodMetaContext;
        this.arguments = arguments == null ? new Object[0] : arguments;
        setParentContext(methodMetaContext.getParentContext());
        this.parameterContexts = createParameterContexts();
        setContextVar();
    }

    public MethodMetaContext getMetaContext() {
        return metaContext;
    }

    @Override
    public Method getCurrentAnnotatedElement() {
        return metaContext.getCurrentAnnotatedElement();
    }

    /**
     * 获取类上下文
     *
     * @return 类上下文
     */
    public ClassContext getClassContext() {
        return (ClassContext) getParentContext();
    }

    /**
     * 获取所有原始的参数值
     *
     * @return 方法参数列表
     */
    public Object[] getArguments() {
        return arguments;
    }

    /**
     * 获取所有经过处理之后的参数值
     *
     * @return 经过处理之后的参数列表
     */
    public Object[] getAfterProcessArguments() {
        return Stream.of(getParameterContexts()).map(ParameterContext::getValue).toArray(Object[]::new);
    }

    /**
     * 获取所有的方法参数上下文对象
     *
     * @return 所有的方法参数上下文对象
     */
    public ParameterContext[] getParameterContexts() {
        return this.parameterContexts;
    }


    /**
     * 获取当前方法的唯一签名信息<br/>
     * <pre>
     *  {@code
     *   类名#方法名(参数值1, 参数值1, ...)
     *
     *   例如：
     *   StringUtils#hasText('abcd')
     *  }
     * </pre>
     *
     * @return 当前方法的简单签名信息
     */
    public String getSignature() {
        return getClassContext().getCurrentAnnotatedElement().getSimpleName() + "#" + getCurrentAnnotatedElement().getName() + paramterToString();
    }

    /**
     * 将参数列表转化为字符串显示
     *
     * @return 参数列表转化为字符串显示
     */
    public String paramterToString() {
        return StringUtils.join(getArguments(), "(", ", ", ")");
    }

    /**
     * 使用当前方法上下文中的信息创建出对应的参数列表上下文对象
     *
     * @return 参数上下文对象数组
     */
    private ParameterContext[] createParameterContexts() {
        int parameterCount = getCurrentAnnotatedElement().getParameterCount();
        ParameterContext[] parameterContexts = new ParameterContext[parameterCount];
        String[] parameterNames = metaContext.getParameterNames();
        for (int i = 0; i < parameterCount; i++) {
            parameterContexts[i] = new ParameterContext(this, parameterNames[i], this.arguments[i], i);
        }
        return parameterContexts;
    }

    @Override
    public Parameter[] getParameters() {
        return metaContext.getParameters();
    }

    @Override
    public ResolvableType[] getParameterResolvableTypes() {
        return metaContext.getParameterResolvableTypes();
    }

    @Override
    public String[] getParameterNames() {
        return metaContext.getParameterNames();
    }

    @Override
    public ResolvableType getReturnResolvableType() {
        return metaContext.getReturnResolvableType();
    }

    @Override
    public Class<?> getReturnType() {
        return metaContext.getReturnType();
    }

    @Override
    public boolean isVoidMethod() {
        return metaContext.isVoidMethod();
    }

    @Override
    public boolean needAutoCloseResource() {
        return metaContext.needAutoCloseResource();
    }

    @Override
    public boolean isConvertProhibition() {
        return metaContext.isConvertProhibition();
    }

    @Override
    public boolean isAsyncMethod() {
        return metaContext.isAsyncMethod();
    }

    @Override
    public boolean isReqCreatCompleteExecutionWrapperMethod() {
        return metaContext.isReqCreatCompleteExecutionWrapperMethod();
    }

    @Override
    public boolean isImmediateExecutionWrapperMethod() {
        return metaContext.isImmediateExecutionWrapperMethod();
    }

    @Override
    public Object invokeWrapperMethod() {
        try {
            return parseExpression(getMergedAnnotation(Wrapper.class).value(), getRealMethodReturnType());
        } catch (Exception e) {
            throw new WrapperMethodInvokeException(e, "Wrapper method invocation failed: '{}'", getCurrentAnnotatedElement()).printException(log);
        }
    }

    @Override
    public boolean isFutureMethod() {
        return metaContext.isFutureMethod();
    }

    @Override
    public boolean isOptionalMethod() {
        return metaContext.isOptionalMethod();
    }

    @Override
    public Type getRealMethodReturnType() {
        return metaContext.getRealMethodReturnType();
    }

    @Override
    public ResolvableType getRealMethodReturnResolvableType() {
        return metaContext.getRealMethodReturnResolvableType();
    }

    @Override
    public String getSimpleSignature() {
        return metaContext.getSimpleSignature();
    }

    @Override
    public void setContextVar() {
        SpELVariate contextVar = getContextVar();
        contextVar.addRootVariable($_METHOD_CONTEXT_$, this);
        contextVar.addRootVariable($_METHOD_ARGS_$, LazyValue.of(this::getArguments));
        Method currentMethod = getCurrentAnnotatedElement();

        // 加载由@SpELImport导入的函数、变量和Hook
        handleSpELImport(currentMethod, importVarHandler());

        useHook(Lifecycle.METHOD);
    }

    public void setThrowableVar(Throwable throwable) {
        getContextVar().addRootVariable($_THROWABLE_$, throwable);
        useHook(Lifecycle.THROWABLE);
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        try {
            // 处理由DestroyMeta注解注册的销毁逻辑
            List<DestroyMeta> destroyMetaAnnList = findNestCombinationAnnotationsCheckParent(DestroyMeta.class);
            for (DestroyMeta destroyMetaAnn : destroyMetaAnnList) {
                try {
                    String enable = destroyMetaAnn.enable();
                    if (StringUtils.hasText(enable) && !parseExpression(enable, boolean.class)) {
                        continue;
                    }

                    DestroyHandle destroyHandle = generateObject(destroyMetaAnn.destroyHandle(), destroyMetaAnn.destroyClass(), DestroyHandle.class);
                    destroyHandle.destroy(new DestroyContext(this, destroyMetaAnn));
                } catch (Exception e) {
                    log.error("Destruction processor execution failed", e);
                }
            }

            // 执行销毁回调器
            useHook(Lifecycle.DESTROY, false);
        } finally {
            // 移除当前METHOD_CONTEXT作用域对象
            ((AbstractObjectCreator) getHttpProxyFactory().getObjectCreator()).removeMethodContextElement(this);
        }
    }

    /**
     * 获取当前方法的重试执行器
     *
     * @return 重试执行器
     */
    @SuppressWarnings("all")
    public RetryActuator getRetryActuator() {
        return this.metaContext.getOrCreateRetryActuator(() -> {
            Boolean retryEnable = getVar(__$RETRY_SWITCH$__, Boolean.class);
            if (Objects.equals(Boolean.TRUE, retryEnable)) {
                // Task Name
                String taskName = getVar(__$RETRY_TASK_NAME$__, String.class);
                taskName = StringUtils.hasText(taskName) ? taskName : getSimpleSignature();

                // count
                Integer retryCount = getVar(__$RETRY_COUNT$__, Integer.class);
                retryCount = retryCount != null ? retryCount : 3;

                // Function
                Function<MethodContext, RunBeforeRetryContext> beforeRetryFunction = getVar(__$RETRY_RUN_BEFORE_RETRY_FUNCTION$__, Function.class);
                Function<MethodContext, RetryDeciderContext> deciderFunction = getVar(__$RETRY_DECIDER_FUNCTION$__, Function.class);

                return new RetryActuator(taskName, retryCount, beforeRetryFunction, deciderFunction, null);
            } else if (Objects.equals(Boolean.FALSE, retryEnable)) {
                return RetryActuator.DONT_RETRY;
            } else {
                RetryMeta retryAnn = getMergedAnnotationCheckParent(RetryMeta.class);
                if (retryAnn == null || isAnnotatedCheckParent(RetryProhibition.class)) {
                    return RetryActuator.DONT_RETRY;
                } else {
                    // 获取任务名和重试次数
                    String taskName = retryAnn.name();
                    int retryCount = retryAnn.retryCount();

                    // 构建重试前运行函数对象和重试决策者对象Function
                    Function<MethodContext, RunBeforeRetryContext> beforeRetryFunction = c -> c.generateObject(retryAnn.beforeRetry());
                    Function<MethodContext, RetryDeciderContext> deciderFunction = c -> c.generateObject(retryAnn.decider());

                    // 构建重试执行器
                    return new RetryActuator(taskName, retryCount, beforeRetryFunction, deciderFunction, retryAnn);
                }
            }
        });
    }

    /**
     * 获取拦截器执行链{@link InterceptorPerformerChain}实例
     * <pre>
     *     1.注册全局生效的拦截器
     *     2.注册类上通过{@link InterceptorRegister @InterceptorRegister}系列注解注入的拦截器
     *     3.注册方法上通过{@link InterceptorRegister @InterceptorRegister}系列注解注入的拦截器
     *     4.将所有拦截器按照优先级进行排序
     * </pre>
     *
     * @return 拦截器执行链InterceptorPerformerChain实例
     */
    public InterceptorPerformerChain getInterceptorChain() {
        return this.metaContext.getOrCreateInterceptorChain(() -> {
            // 构建拦截器执行链
            InterceptorPerformerChain chain = new InterceptorPerformerChain();

            // 注册通过HttpClientProxyObjectFactory添加进来的拦截器
            chain.addInterceptorPerformers(getInterceptorPerformerList());

            // 添加类上以及方法上的拦截器
            findNestCombinationAnnotationsCheckParent(InterceptorRegister.class)
                    .forEach(chain::addInterceptor);

            // 按优先级进行排序
            chain.sort(this);

            return chain;
        });
    }

    /**
     * 加载解析静态参数，并将参数添加到请求实例中
     *
     * @param request 请求实例
     */
    public void loadStaticParams(Request request) {
        this.metaContext.getOrCreateStaticParamLoader(() -> new StaticParamLoader(this))
                .resolverAndSetter(request, this);
    }

    /**
     * 加载解析动态参数，并将参数添加到请求实例中
     *
     * @param request 请求实例
     */
    public void loadDynamicParams(Request request) {
        this.metaContext.getOrCreateDynamicParamLoader(() -> new DynamicParamLoader(this))
                .resolverAndSetter(request, this);
    }

    /**
     * 获取所有的通用拦截器
     *
     * @return 所有的通用拦截器集合
     */
    public List<InterceptorPerformer> getInterceptorPerformerList() {
        HttpClientProxyObjectFactory httpProxyFactory = getHttpProxyFactory();
        List<InterceptorPerformer> interceptorPerformerList = httpProxyFactory.getInterceptorPerformerList();
        List<Generate<InterceptorPerformer>> performerGenerateList = httpProxyFactory.getPerformerGenerateList();

        List<InterceptorPerformer> interceptorPerformers = new ArrayList<>(interceptorPerformerList.size() + performerGenerateList.size());
        interceptorPerformers.addAll(interceptorPerformerList);
        performerGenerateList.forEach(factory -> interceptorPerformers.add(factory.create(this)));
        return interceptorPerformers;
    }

    /**
     * 获取用于执行当前HTTP任务的线程池
     * <pre>
     *     1.如果检测到SpEL环境中存在{@value InternalVarName#__$ASYNC_EXECUTOR$__},则使用变量值所对应的线程池
     *     2.如果当前方法上标注了{@link AsyncExecutor @AsyncExecutor}注解，则返回该注解所指定的线程池
     *     3.否则返回默认的线程池
     * </pre>
     *
     * @return 执行当前HTTP任务的线程池
     */
    public Executor getExecutor() {
        return this.metaContext.getOrCreateExecutor(() -> {

            HttpClientProxyObjectFactory proxyFactory = getHttpProxyFactory();

            // 首先尝试从环境变量中获取线程池配置
            String asyncExecName = getVar(__$ASYNC_EXECUTOR$__, String.class);

            // 再尝试从注解中获取
            if (!StringUtils.hasText(asyncExecName)) {
                AsyncExecutor asyncExecAnn = getSameAnnotationCombined(AsyncExecutor.class);
                if (asyncExecAnn != null && StringUtils.hasText(asyncExecAnn.poolName())) {
                    asyncExecName = asyncExecAnn.poolName();
                }
            }

            if (StringUtils.hasText(asyncExecName)) {
                return proxyFactory.getAlternativeAsyncExecutor(asyncExecName).getValue();
            }

            // 最后取默认线程池
            return proxyFactory.getAsyncExecutor();
        });
    }
}
