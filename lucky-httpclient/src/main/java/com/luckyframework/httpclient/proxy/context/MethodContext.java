package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.generalapi.describe.ApiDescribe;
import com.luckyframework.httpclient.generalapi.describe.DescribeFunction;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.AsyncExecutor;
import com.luckyframework.httpclient.proxy.annotations.InterceptorMeta;
import com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta;
import com.luckyframework.httpclient.proxy.annotations.RetryMeta;
import com.luckyframework.httpclient.proxy.annotations.RetryProhibition;
import com.luckyframework.httpclient.proxy.annotations.Wrapper;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorFactory;
import com.luckyframework.httpclient.proxy.async.Model;
import com.luckyframework.httpclient.proxy.creator.AbstractObjectCreator;
import com.luckyframework.httpclient.proxy.creator.Generate;
import com.luckyframework.httpclient.proxy.destroy.DestroyContext;
import com.luckyframework.httpclient.proxy.destroy.DestroyHandle;
import com.luckyframework.httpclient.proxy.destroy.DestroyMeta;
import com.luckyframework.httpclient.proxy.dynamic.DynamicParamLoader;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionExecuteException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionMismatchException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionNotFoundException;
import com.luckyframework.httpclient.proxy.exeception.WrapperMethodInvokeException;
import com.luckyframework.httpclient.proxy.handle.ResultContext;
import com.luckyframework.httpclient.proxy.handle.ResultHandler;
import com.luckyframework.httpclient.proxy.handle.ResultHandlerHolder;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformer;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformerChain;
import com.luckyframework.httpclient.proxy.retry.RetryActuator;
import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;
import com.luckyframework.httpclient.proxy.slow.SlowResponseHandler;
import com.luckyframework.httpclient.proxy.slow.SlowResponseHandlerMeta;
import com.luckyframework.httpclient.proxy.spel.InternalVarName;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.ValueSpaceConstant;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.statics.StaticParamLoader;
import com.luckyframework.httpclient.proxy.typeparser.PackTypeParser;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.spel.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_API_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CURRENT_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_ARGS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_CONTENT_INIT_THREAD_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_THROWABLE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$ASYNC_CONCURRENCY$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$ASYNC_EXECUTOR$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$ASYNC_MODEL$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_COUNT$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_DECIDER_FUNCTION$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_RUN_BEFORE_RETRY_FUNCTION$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_SWITCH$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_TASK_NAME$__;
import static com.luckyframework.httpclient.proxy.spel.hook.Lifecycle.INVOKE_WRAPPER_METHOD;


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
     * 约定的Wrapper方法后缀
     */
    public final String WRAPPER_FUNCTION_SUFFIX = "$Wrapper";

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
     * 结果处理器持有者
     */
    private final ResultHandlerHolder resultHandlerHolder;

    /**
     * 方法上下文构造方法
     *
     * @param methodMetaContext 方法元信息上下文对象
     * @param arguments         参数列表
     */
    public MethodContext(MethodMetaContext methodMetaContext, Object[] arguments) {
        super(methodMetaContext.getCurrentAnnotatedElement());
        setParentContext(methodMetaContext.getParentContext());
        this.metaContext = methodMetaContext;
        this.arguments = arguments == null ? new Object[0] : arguments;
        this.resultHandlerHolder = getResultHandlerHolder();
        this.parameterContexts = createParameterContexts();
        initContext();
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

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getArgument(Class<T> type) {
        for (Object argument : getArguments()) {
            if (type.isInstance(argument)) {
                return (T) argument;
            }
        }
        return null;
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
            useHook(INVOKE_WRAPPER_METHOD);
            Wrapper wrapperAnn = getMergedAnnotation(Wrapper.class);
            if (StringUtils.hasText(wrapperAnn.value())) {
                return handleResultAndReturn(parseExpression(wrapperAnn.value(), getResultType()));
            }
            Method wrapperFuncMethod = getWrapperFuncMethod(wrapperAnn.fun());
            if (wrapperFuncMethod != null) {
                return handleResultAndReturn(autoInjectParamExecuteMethod(null, wrapperFuncMethod));
            }
            throw new SpELFunctionExecuteException("Wrapper config not found");
        } catch (Throwable e) {
            throw new WrapperMethodInvokeException(e, "Wrapper method invocation failed: '{}'", FontUtil.getRedUnderline(MethodUtils.getLocation(getCurrentAnnotatedElement()))).error(log);
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
        return getMethodConvertReturnResolvableType().getType();
    }

    @Override
    public ResolvableType getMethodConvertReturnResolvableType() {
        for (PackTypeParser packTypeParser : getHttpProxyFactory().getPackTypeParsers()) {
            if (packTypeParser.canHandle(this)) {
                return packTypeParser.getRealType(this, getReturnResolvableType());
            }
        }
        return getReturnResolvableType();
    }

    @Override
    public String getSimpleSignature() {
        return metaContext.getSimpleSignature();
    }

    @Override
    public void initContext() {
        SpELVariate contextVar = getContextVar();

        Map<String, Object> immutableMap = new HashMap<>(4);
        immutableMap.put($_METHOD_CONTEXT_$, this);
        immutableMap.put($_CURRENT_CONTEXT_$, this);
        immutableMap.put($_METHOD_ARGS_$, LazyValue.of(this::getArguments));
        immutableMap.put($_METHOD_CONTENT_INIT_THREAD_$, Thread.currentThread());
        contextVar.addRootVariable(ValueSpaceConstant.METHOD_CONTEXT_SPACE, Collections.unmodifiableMap(immutableMap));

        // 添加基于@Describe注解的接口信息
        contextVar.addRootVariable(ValueSpaceConstant.API_DESC_SPACE, Collections.singletonMap($_API_$, LazyValue.of(() -> DescribeFunction.describe(this))));

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
     * 运行当前方法
     *
     * @param args 参数列表
     * @return 运行当前方法的结果
     */
    public Object invokeCurrentMethod(Object... args) {
        return MethodUtils.invoke(getProxyObject(), getCurrentAnnotatedElement(), args);
    }

    /**
     * 运行当前方法
     *
     * @return 运行当前方法的结果
     */
    public Object invokeCurrentMethod() {
        return MethodUtils.invoke(getProxyObject(), getCurrentAnnotatedElement(), getArguments());
    }

    /**
     * 执行当前方法，传入一个实现类对象
     *
     * @param impl       实现类对象
     * @param exFunction 异常转换函数
     * @return 方法执行结果
     * @throws Throwable 执行过程中可能出现的异常
     */
    public Object invokeImplMethod(Object impl, Function<Throwable, Throwable> exFunction) throws Throwable {
        return MethodUtils.invokeThrow(impl, getCurrentAnnotatedElement(), exFunction, getArguments());
    }


    /**
     * 执行当前方法，传入一个实现类对象
     *
     * @param impl 实现类对象
     * @return 方法执行结果
     * @throws Throwable 执行过程中可能出现的异常
     */
    public Object invokeImplMethod(Object impl) throws Throwable {
        return invokeImplMethod(impl, Throwable::getCause);
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
                } catch (Throwable e) {
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

            //-----------------------------------------------------------
            //                      ConfigApi
            //-----------------------------------------------------------

            // ConfigApi中的开关
            Boolean retryEnable = getVar(__$RETRY_SWITCH$__, Boolean.class);

            // ConfigApi明确标注禁止重试
            if (Objects.equals(Boolean.FALSE, retryEnable)) {
                return RetryActuator.DONT_RETRY;
            }

            // ConfigApi
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

                return new RetryActuator(taskName, retryCount, beforeRetryFunction, deciderFunction, false, null);
            }

            //-----------------------------------------------------------
            //                      AnnotationApi
            //-----------------------------------------------------------

            // 使用注解明确标注禁止重试
            if (isAnnotatedCheckParent(RetryProhibition.class)) {
                return RetryActuator.DONT_RETRY;
            }

            // 获取重试元注解
            RetryMeta retryAnn = getMergedAnnotationCheckParent(RetryMeta.class);

            // 不存在重试注解时，使用全局的重试执行器
            if (retryAnn == null) {
                return getHttpProxyFactory().getRetryActuator();
            }

            //存在重试元注解

            // 校验开关
            boolean enable = parseExpression(retryAnn.enable(), boolean.class);
            if (!enable) {
                return RetryActuator.DONT_RETRY;
            }

            // 校验重试次数
            int retryCount = parseExpression(retryAnn.retryCount(), int.class);
            if (retryCount <= 0) {
                return RetryActuator.DONT_RETRY;
            }
            // 构建重试前运行函数对象和重试决策者对象Function
            Function<MethodContext, RunBeforeRetryContext> beforeRetryFunction = c -> c.generateObject(retryAnn.beforeRetry());
            Function<MethodContext, RetryDeciderContext> deciderFunction = c -> c.generateObject(retryAnn.decider());

            // 构建重试执行器
            return new RetryActuator(retryAnn.name(), retryCount, beforeRetryFunction, deciderFunction, retryAnn.strict(), retryAnn);
        });
    }

    /**
     * 获取拦截器执行链{@link InterceptorPerformerChain}实例
     * <pre>
     *     1.注册全局生效的拦截器
     *     2.注册类上通过{@link InterceptorMeta @InterceptorMeta}系列注解注入的拦截器
     *     3.注册方法上通过{@link InterceptorMeta @InterceptorMeta}系列注解注入的拦截器
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
            findNestCombinationAnnotationsCheckParent(InterceptorMeta.class).forEach(chain::addInterceptor);

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
        this.metaContext.getOrCreateStaticParamLoader(() -> new StaticParamLoader(this)).resolverAndSetter(request, this);
    }

    /**
     * 加载解析动态参数，并将参数添加到请求实例中
     *
     * @param request 请求实例
     */
    public void loadDynamicParams(Request request) {
        this.metaContext.getOrCreateDynamicParamLoader(() -> new DynamicParamLoader(this)).resolverAndSetter(request, this);
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
     *     2.如果检测到SpEL环境中存在{@value InternalVarName#__$ASYNC_CONCURRENCY$__},则使创建支持并发控制的线程池
     *     3.如果当前方法上标注了{@link AsyncExecutor @AsyncExecutor}注解，则返回该注解所指定的线程池
     *     4.否则返回默认的线程池
     * </pre>
     *
     * @return 执行当前HTTP任务的线程池
     */
    public AsyncTaskExecutor getAsyncTaskExecutor() {
        return this.metaContext.getOrCreateAsyncTaskExecutor(() -> {
            // 如果入参中存在线程池参数则使用入参中的线程池
            AsyncTaskExecutor taskExecutor = getArgument(AsyncTaskExecutor.class);
            if (taskExecutor != null) {
                return taskExecutor;
            }

            // 获取异步模型
            Model asyncModel = getAsyncModel();
            // 并发数
            Integer concurrency = null;
            // 执行器
            Executor executor = null;

            // 首先尝试从上下文变量中获取线程池配置
            String executorVar = getVar(__$ASYNC_EXECUTOR$__, String.class);
            if (StringUtils.hasText(executorVar)) {
                executor = createExecutor(executorVar);
            }

            // 尝试从上下文变量中获取异步并发配置
            String concurrencyVar = getVar(__$ASYNC_CONCURRENCY$__, String.class);
            if (StringUtils.hasText(concurrencyVar)) {
                concurrency = parseExpression(concurrencyVar, int.class);
            }

            // 尝试从注解中获取程池配置和并发配置
            AsyncExecutor asyncExecAnn = getMergedAnnotationCheckParent(AsyncExecutor.class);
            if (asyncExecAnn != null) {

                // 解析@AsyncExecutor注解的executor属性
                if (executor == null) {
                    String executorExp = asyncExecAnn.executor();
                    if (StringUtils.hasText(executorExp)) {
                        executor = createExecutor(executorExp);
                    }
                }

                // 解析@AsyncExecutor注解的concurrency属性
                if (concurrency == null) {
                    String concurrencyConfig = asyncExecAnn.concurrency();
                    if (StringUtils.hasText(concurrencyConfig)) {
                        concurrency = parseExpression(concurrencyConfig, int.class);
                    }
                }
            }

            HttpClientProxyObjectFactory proxyFactory = getHttpProxyFactory();
            concurrency = concurrency == null ? proxyFactory.getDefaultExecutorConcurrency() : concurrency;
            return executor == null ? AsyncTaskExecutorFactory.createDefault(proxyFactory, concurrency, asyncModel) : AsyncTaskExecutorFactory.create(executor, concurrency, asyncModel);
        });
    }

    /**
     * 获取当前代理方法的描述信息
     *
     * @return 当前代理方法的描述信息
     */
    public ApiDescribe getApiDescribe() {
        return getRootVar($_API_$, ApiDescribe.class);
    }

    /**
     * 获取方法字符串
     * <pre>
     *     ${ClassName}.${MethodName}
     * </pre>
     *
     * @return 方法字符串
     */
    public String getMethodString() {
        return this.metaContext.getMethodString();
    }

    /**
     * 获取异步模型并缓存
     *
     * @return 异步模型
     */
    public Model getAsyncModel() {
        return metaContext.getOrCreateAsyncModel(() -> {
            Model model = getVar(__$ASYNC_MODEL$__, Model.class);
            if (model == null) {
                AsyncExecutor asyncExecAnn = getMergedAnnotationCheckParent(AsyncExecutor.class);
                if (asyncExecAnn != null) {
                    model = asyncExecAnn.model();
                }
            }

            if (model == null || model == Model.USE_COMMON) {
                return getHttpProxyFactory().getAsyncModel();
            }

            return model;
        });
    }

    /**
     * 能否应用结果处理器
     *
     * @return 能否应用结果处理器
     */
    public boolean canApplyResultHandler() {
        return isVoidMethod() && resultHandlerHolder != null;
    }

    /**
     * 获取结果类型
     *
     * @return 结果类型
     */
    public ResolvableType getResultResolvableType() {
        if (canApplyResultHandler()) {
            ResolvableType resultType = resultHandlerHolder.getResultType();
            if (Optional.class.isAssignableFrom(resultType.toClass())) {
                return resultType.hasGenerics() ? resultType.getGeneric(0) : ResolvableType.forClass(Object.class);
            }
            return resultType;
        }
        return getMethodConvertReturnResolvableType();
    }

    /**
     * 获取结果类型
     *
     * @return 结果类型
     */
    public Type getResultType() {
        return getResultResolvableType().getType();
    }

    /**
     * 处理并返回结果
     *
     * @param result 结果对象
     * @return 接口最终返回结果
     * @throws Throwable 处理过程中可能出现异常
     */
    public Object handleResultAndReturn(Object result) throws Throwable {
        if (canApplyResultHandler()) {
            handleResult(result);
            return null;
        }
        return result;
    }

    /**
     * 处理结果
     *
     * @param result 结果
     * @param <T>    结果泛型
     * @throws Throwable 可能出现的异常
     */
    @SuppressWarnings("all")
    public <T> void handleResult(T result) throws Throwable {
        ResultHandler resultHandler = resultHandlerHolder.getResultHandler();
        ResolvableType resultType = resultHandlerHolder.getResultType();
        if (Optional.class.isAssignableFrom(resultType.toClass())) {
            resultHandler.handleResult(new ResultContext<>(this, Optional.ofNullable(result)));
        } else {
            resultHandler.handleResult(new ResultContext<>(this, result));
        }
    }

    public synchronized HttpExecutor getHttpExecutor() {
        HttpExecutor executor = getArgument(HttpExecutor.class);
        return executor != null ? executor : super.getHttpExecutor();
    }


    /**
     * 获取指定的用于处理Wrapper逻辑的函数，如果不存在则会尝试查找约定的Wrapper函数
     *
     * @param wrapperFuncName 指定的Wrapper函数名
     * @return Wrapper方法
     */
    @Nullable
    private Method getWrapperFuncMethod(String wrapperFuncName) {

        // 是否指定了处理函数
        boolean isAppoint = StringUtils.hasText(wrapperFuncName);

        // 获取指定的wrapper函数名，如果不存在则使用约定的wrapper函数名
        MethodWrap wrapperFuncMethodWrap = getSpELFuncOrDefault(wrapperFuncName, WRAPPER_FUNCTION_SUFFIX);

        // 找不到函数时的处理
        if (wrapperFuncMethodWrap.isNotFound()) {
            if (isAppoint) {
                throw new SpELFunctionNotFoundException("Wrapper SpEL function named '{}' is not found in context.", wrapperFuncName);
            }
            return null;
        }

        // 函数返回值类型不匹配时的处理
        Method wrapperFuncMethod = wrapperFuncMethodWrap.getMethod();
        if (!ClassUtils.compatibleOrNot(ResolvableType.forMethodReturnType(wrapperFuncMethod), getMethodConvertReturnResolvableType())) {
            if (isAppoint) {
                throw new SpELFunctionMismatchException("Wrapper SpEL function '{}' returns a type value that is incompatible with the target type of the conversion. \n\t--- func-return-type: {} \n\t--- target-type: {}", wrapperFuncName, ResolvableType.forMethodReturnType(wrapperFuncMethod), getMethodConvertReturnResolvableType());
            }
            return null;
        }
        // 校验条件满足
        return wrapperFuncMethod;
    }


    /**
     * 从参数列表获取结果处理器持有者
     *
     * @return 结果处理器持有者
     */
    @Nullable
    private ResultHandlerHolder getResultHandlerHolder() {

        // 优先尝试从参数列表中获取
        Object[] arguments = getArguments();
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            if (argument instanceof ResultHandler && ResultHandler.class.isAssignableFrom(getParameters()[i].getType())) {
                ResolvableType resultType = ResolvableType.forMethodParameter(getCurrentAnnotatedElement(), i);
                return new ResultHandlerHolder((ResultHandler<?>) argument, resultType.hasGenerics() ? resultType.getGeneric(0) : ResolvableType.forClass(Object.class));
            }
        }

        // 尝试从注解中获取
        ResultHandlerMeta resultHandlerMeta = getMergedAnnotationCheckParent(ResultHandlerMeta.class);
        if (resultHandlerMeta != null) {
            ResultHandler<?> resultHandler = generateObject(resultHandlerMeta.handler(), resultHandlerMeta.handlerClass(), ResultHandler.class);
            ResolvableType resultType = ResolvableType.forClass(ResultHandler.class, resultHandler.getClass());
            return new ResultHandlerHolder(resultHandler, resultType.hasGenerics() ? resultType.getGeneric(0) : ResolvableType.forClass(Object.class));
        }
        return null;
    }


    /**
     * 获取当前上下文中生效的慢响应处理器
     *
     * @return 慢响应处理器
     */
    @Nullable
    public SlowResponseHandler getSlowResponseHandler() {
        SlowResponseHandlerMeta slowResponseHandlerMetaAnn = getMergedAnnotationCheckParent(SlowResponseHandlerMeta.class);
        if (slowResponseHandlerMetaAnn != null) {
            return generateObject(slowResponseHandlerMetaAnn.slowHandler(), slowResponseHandlerMetaAnn.slowHandlerClass(), SlowResponseHandler.class);
        }
        return getHttpProxyFactory().getSlowResponseHandler();
    }

}
