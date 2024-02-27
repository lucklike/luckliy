package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.RequestMethod;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.impl.SaveResultResponseProcessor;
import com.luckyframework.httpclient.proxy.annotations.DomainNameMeta;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.RetryMeta;
import com.luckyframework.httpclient.proxy.annotations.RetryProhibition;
import com.luckyframework.httpclient.proxy.annotations.VoidResultConvert;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.ResponseConvert;
import com.luckyframework.httpclient.proxy.convert.VoidResponseConvert;
import com.luckyframework.httpclient.proxy.creator.AbstractObjectCreator;
import com.luckyframework.httpclient.proxy.creator.Generate;
import com.luckyframework.httpclient.proxy.creator.ObjectCreator;
import com.luckyframework.httpclient.proxy.creator.ReflectObjectCreator;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.dynamic.DynamicParamLoader;
import com.luckyframework.httpclient.proxy.handle.DefaultHttpExceptionHandle;
import com.luckyframework.httpclient.proxy.handle.HttpExceptionHandle;
import com.luckyframework.httpclient.proxy.interceptor.Interceptor;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformer;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformerChain;
import com.luckyframework.httpclient.proxy.retry.RetryActuator;
import com.luckyframework.httpclient.proxy.retry.RetryDeciderContent;
import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;
import com.luckyframework.httpclient.proxy.spel.SpELConvert;
import com.luckyframework.httpclient.proxy.statics.StaticParamLoader;
import com.luckyframework.httpclient.proxy.url.DomainNameContext;
import com.luckyframework.httpclient.proxy.url.DomainNameGetter;
import com.luckyframework.httpclient.proxy.url.HttpRequestContext;
import com.luckyframework.httpclient.proxy.url.URLGetter;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.core.ResponseProcessor.DO_NOTHING_PROCESSOR;

/**
 * Http客户端代理对象生成工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/28 15:57
 */
public class HttpClientProxyObjectFactory {

    /**
     * 对象创建器
     */
    private static AbstractObjectCreator objectCreator = new ReflectObjectCreator();

    /**
     * SpEL转换器
     */
    private static SpELConvert spELConverter = new SpELConvert();

    /**
     * SpEL表达式参数配置
     */
    private static final Map<String, Object> expressionParams = new HashMap<>();

    /**
     * 重试执行器【缓存】
     */
    private final Map<Method, RetryActuator> retryActuatorCacheMap = new ConcurrentHashMap<>(16);

    /**
     * 连接超时时间
     */
    private Integer connectionTimeout;

    /**
     * 读超时时间
     */
    private Integer readTimeout;

    /**
     * 写超时时间
     */
    private Integer writeTimeout;

    /**
     * 域名认证器
     */
    private HostnameVerifier hostnameVerifier;

    /**
     * SSLSocketFactory
     */
    private SSLSocketFactory sslSocketFactory;

    /**
     * 公共请求头参数
     */
    private ConfigurationMap headers = new ConfigurationMap();

    /**
     * 公共路径请求参数
     */
    private ConfigurationMap pathParams = new ConfigurationMap();

    /**
     * 公共URL请求参数
     */
    private ConfigurationMap queryParams = new ConfigurationMap();

    /**
     * 公共请求参数
     */
    private ConfigurationMap requestParams = new ConfigurationMap();

    /**
     * 拦截器执行器集合
     */
    private final List<InterceptorPerformer> interceptorPerformerList = new ArrayList<>();

    /**
     * 拦截器执行器工厂集合
     */
    private final List<Generate<InterceptorPerformer>> performerGenerateList = new ArrayList<>();

    /**
     * 用于异步执行的Http任务的线程池
     */
    private Executor asyncExecutor;

    /**
     * 用于异步执行的Http任务的线程池{@link Supplier}
     */
    private Supplier<Executor> asyncExecutorSupplier = () -> new SimpleAsyncTaskExecutor("http-task-");

    /**
     * Http请求执行器
     */
    private HttpExecutor httpExecutor = new JdkHttpExecutor();

    /**
     * 异常处理器
     */
    private HttpExceptionHandle exceptionHandle = new DefaultHttpExceptionHandle();

    /**
     * 异常处理器生成器
     */
    private Generate<HttpExceptionHandle> exceptionHandleGenerate;

    /**
     * 响应转换器
     */
    private ResponseConvert responseConvert;

    /**
     * 响应转换器生成器
     */
    private Generate<ResponseConvert> responseConvertGenerate;

    /**
     * void方法结果转换器
     */
    private VoidResponseConvert voidResponseConvert;

    /**
     * void方法结果转换器生成器
     */
    private Generate<VoidResponseConvert> voidResponseConvertGenerate;

    public HttpClientProxyObjectFactory(HttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    public HttpClientProxyObjectFactory() {

    }

    //------------------------------------------------------------------------------------------------
    //                                getter and setter methods
    //------------------------------------------------------------------------------------------------

    public static SpELConvert getSpELConverter() {
        return spELConverter;
    }

    public static void setSpELConverter(SpELConvert spELConverter) {
        HttpClientProxyObjectFactory.spELConverter = spELConverter;
    }

    public static void addExpressionParam(String name, Object value) {
        expressionParams.put(name, value);
    }

    public static void removeExpressionParam(String... names) {
        for (String name : names) {
            expressionParams.remove(name);
        }
    }

    public static void addExpressionParams(Map<String, Object> confMap) {
        expressionParams.putAll(confMap);
    }

    public static void setExpressionParams(Map<String, Object> confMap) {
        confMap.clear();
        expressionParams.putAll(confMap);
    }

    public static Map<String, Object> getExpressionParams() {
        return expressionParams;
    }

    public Executor getAsyncExecutor() {
        if (asyncExecutor == null) {
            asyncExecutor = asyncExecutorSupplier.get();
        }
        return asyncExecutor;
    }

    public void setAsyncExecutor(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    public void setAsyncExecutorSupplier(Supplier<Executor> asyncExecutorSupplier) {
        this.asyncExecutorSupplier = asyncExecutorSupplier;
    }

    public static ObjectCreator getObjectCreator() {
        return objectCreator;
    }

    public static void setObjectCreator(AbstractObjectCreator objectCreator) {
        HttpClientProxyObjectFactory.objectCreator = objectCreator;
    }

    public Integer getConnectionTimeout() {
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getReadTimeout() {
        return this.readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Integer getWriteTimeout() {
        return this.writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public HttpExecutor getHttpExecutor() {
        return httpExecutor;
    }

    public void setHttpExecutor(HttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    public HttpExceptionHandle getExceptionHandle() {
        return exceptionHandle;
    }

    public HttpExceptionHandle getExceptionHandle(Context context) {
        if (this.exceptionHandleGenerate != null) {
            return this.exceptionHandleGenerate.create(context);
        }
        return getExceptionHandle();
    }


    public void setExceptionHandle(HttpExceptionHandle exceptionHandle) {
        this.exceptionHandle = exceptionHandle;
    }

    public void setExceptionHandle(Generate<HttpExceptionHandle> exceptionHandleGenerate) {
        this.exceptionHandleGenerate = exceptionHandleGenerate;
    }

    public <T extends HttpExceptionHandle> void setExceptionHandle(Class<T> exceptionHandleClass, String exceptionHandleMsg, Scope scope, Consumer<T> handleConsumer) {
        setExceptionHandle(context -> objectCreator.newObject(exceptionHandleClass, exceptionHandleMsg, context, scope, handleConsumer));
    }

    public <T extends HttpExceptionHandle> void setExceptionHandle(Class<? extends HttpExceptionHandle> exceptionHandleClass, Scope scope) {
        setExceptionHandle(exceptionHandleClass, "", scope, h -> {
        });
    }

    public void addInterceptorPerformers(InterceptorPerformer... interceptorPerformers) {
        this.interceptorPerformerList.addAll(Arrays.asList(interceptorPerformers));
    }

    public void addInterceptorPerformers(Collection<InterceptorPerformer> interceptorPerformers) {
        this.interceptorPerformerList.addAll(interceptorPerformers);
    }

    public List<InterceptorPerformer> getInterceptorPerformerList(MethodContext methodContext) {
        List<InterceptorPerformer> interceptorPerformers = new ArrayList<>(this.interceptorPerformerList.size() + this.performerGenerateList.size());
        interceptorPerformers.addAll(this.interceptorPerformerList);
        this.performerGenerateList.forEach(factory -> interceptorPerformers.add(factory.create(methodContext)));
        return interceptorPerformers;
    }

    public void addInterceptors(Interceptor... interceptors) {
        Stream.of(interceptors).forEach(inter -> this.interceptorPerformerList.add(new InterceptorPerformer(inter)));
    }

    public void addInterceptor(Interceptor interceptor, Integer priority) {
        this.interceptorPerformerList.add(new InterceptorPerformer(interceptor, priority));
    }

    public void addInterceptor(Generate<InterceptorPerformer> performerGenerate) {
        this.performerGenerateList.add(performerGenerate);
    }

    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, String interceptorMsg, Scope scope, Consumer<T> interceptorConsumer, Integer priority) {
        addInterceptor(
                context -> new InterceptorPerformer(() -> objectCreator.newObject(interceptorClass, interceptorMsg, context, scope, interceptorConsumer), priority)
        );
    }

    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, String interceptorMsg, Scope scope, Integer priority) {
        addInterceptor(
                context -> new InterceptorPerformer(() -> objectCreator.newObject(interceptorClass, interceptorMsg, context, scope, i -> {
                }), priority)
        );
    }

    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, Scope scope, Consumer<T> interceptorConsumer, Integer priority) {
        addInterceptor(interceptorClass, "", scope, interceptorConsumer, priority);
    }

    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, Scope scope, Integer priority) {
        addInterceptor(interceptorClass, "", scope, i -> {
        }, priority);
    }

    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, Scope scope, Consumer<T> interceptorConsumer) {
        addInterceptor(interceptorClass, scope, interceptorConsumer, null);
    }

    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, Scope scope) {
        addInterceptor(interceptorClass, scope, i -> {
        }, null);
    }

    public ResponseConvert getResponseConvert(Context context) {
        if (this.responseConvertGenerate != null) {
            return responseConvertGenerate.create(context);
        }
        return getResponseConvert();
    }

    public ResponseConvert getResponseConvert() {
        return responseConvert;
    }

    public void setResponseConvert(ResponseConvert responseConvert) {
        this.responseConvert = responseConvert;
    }

    public void setResponseConvert(Generate<ResponseConvert> responseConvertGenerate) {
        this.responseConvertGenerate = responseConvertGenerate;
    }

    public <T extends ResponseConvert> void setResponseConvert(Class<T> responseConvertClass, String responseConvertMsg, Scope scope, Consumer<T> convertConsumer) {
        setResponseConvert(context -> objectCreator.newObject(responseConvertClass, responseConvertMsg, context, scope, convertConsumer));
    }

    public <T extends ResponseConvert> void setResponseConvert(Class<T> responseConvertClass, String responseConvertMsg, Scope scope) {
        setResponseConvert(context -> objectCreator.newObject(responseConvertClass, responseConvertMsg, context, scope, c -> {
        }));
    }

    public <T extends ResponseConvert> void setResponseConvert(Class<T> responseConvertClass, Scope scope, Consumer<T> convertConsumer) {
        setResponseConvert(responseConvertClass, "", scope, convertConsumer);
    }

    public <T extends ResponseConvert> void setResponseConvert(Class<T> responseConvertClass, Scope scope) {
        setResponseConvert(responseConvertClass, "", scope, c -> {
        });
    }

    public VoidResponseConvert getVoidResponseConvert(MethodContext context) {
        if (voidResponseConvertGenerate != null) {
            return voidResponseConvertGenerate.create(context);
        }
        return voidResponseConvert;
    }

    public VoidResponseConvert getVoidResponseConvert() {
        return voidResponseConvert;
    }

    public void setVoidResponseConvert(VoidResponseConvert voidResponseConvert) {
        this.voidResponseConvert = voidResponseConvert;
    }

    public void setVoidResponseConvert(Generate<VoidResponseConvert> voidResponseConvertGenerate) {
        this.voidResponseConvertGenerate = voidResponseConvertGenerate;
    }

    public <T extends VoidResponseConvert> void setVoidResponseConvert(Class<T> vrcgClass, String vrcgMsg, Scope scope, Consumer<T> convertConsumer) {
        setVoidResponseConvert(context -> objectCreator.newObject(vrcgClass, vrcgMsg, context, scope, convertConsumer));
    }

    public <T extends VoidResponseConvert> void setVoidResponseConvert(Class<T> vrcgClass, String vrcgMsg, Scope scope) {
        setVoidResponseConvert(context -> objectCreator.newObject(vrcgClass, vrcgMsg, context, scope, c -> {
        }));
    }

    public <T extends VoidResponseConvert> void setVoidResponseConvert(Class<T> vrcgClass, Scope scope, Consumer<T> convertConsumer) {
        setVoidResponseConvert(vrcgClass, "", scope, convertConsumer);
    }

    public <T extends VoidResponseConvert> void setVoidResponseConvert(Class<T> vrcgClass, Scope scope) {
        setVoidResponseConvert(vrcgClass, "", scope, c -> {
        });
    }

    public void setHeaders(ConfigurationMap headerMap) {
        this.headers = headerMap;
    }

    public void setProxyClassHeaders(Class<?> proxyClass, Map<String, Object> proxyClassHeaders) {
        this.headers.put(proxyClass.getName(), proxyClassHeaders);
    }

    public void setPathParameters(ConfigurationMap pathMap) {
        this.pathParams = pathMap;
    }

    public void setProxyClassPathParameters(Class<?> proxyClass, Map<String, Object> proxyClassPathParameters) {
        this.pathParams.put(proxyClass.getName(), proxyClassPathParameters);
    }

    public void setQueryParameters(ConfigurationMap queryMap) {
        this.queryParams = queryMap;
    }

    public void setProxyClassQueryParameter(Class<?> proxyClass, Map<String, Object> proxyClassQueryParameters) {
        this.queryParams.put(proxyClass.getName(), proxyClassQueryParameters);
    }

    public void setFormParameters(ConfigurationMap formMap) {
        this.requestParams = formMap;
    }

    public void setProxyClassFormParameter(Class<?> proxyClass, Map<String, Object> proxyClassFormParameters) {
        this.requestParams.put(proxyClass.getName(), proxyClassFormParameters);
    }

    public void addInputStream(String name, String fileName, InputStream inputStream) {
        MultipartFile mf = new MultipartFile(inputStream, fileName);
        this.requestParams.put(name, mf);
    }

    public void addFiles(String name, File... files) {
        this.requestParams.put(name, files);
    }

    public void addFiles(String name, String... filePaths) {
        addFiles(name, ConversionUtils.conversion(filePaths, File[].class));
    }

    public void addResources(String name, Resource... resources) {
        this.requestParams.put(name, resources);
    }

    public void addResources(String name, String... resourcePaths) {
        addResources(name, ConversionUtils.conversion(resourcePaths, Resource[].class));
    }

    public void addMultipartFiles(String name, MultipartFile... multipartFiles) {
        this.requestParams.put(name, multipartFiles);
    }

    private HttpClientProxyObjectFactory getHttpProxyFactory() {
        return this;
    }

    //------------------------------------------------------------------------------------------------
    //                                generate proxy object
    //------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public <T> T getCglibProxyObject(Class<T> interfaceClass) {
        return (T) ProxyFactory.getCglibProxyObject(interfaceClass, Enhancer::create, new CglibHttpRequestMethodInterceptor(interfaceClass));
    }

    @SuppressWarnings("unchecked")
    public <T> T getJdkProxyObject(Class<T> interfaceClass) {
        return (T) ProxyFactory.getJdkProxyObject(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new JdkHttpRequestInvocationHandler(interfaceClass));
    }

    public void shutdown() {
        if (this.asyncExecutor instanceof ExecutorService) {
            ((ExecutorService) this.asyncExecutor).shutdown();
        }
    }

    public void shutdownNow() {
        if (this.asyncExecutor instanceof ExecutorService) {
            ((ExecutorService) this.asyncExecutor).shutdownNow();
        }
    }

    //------------------------------------------------------------------------------------------------
    //                               cglib/Jdk method interceptor
    //------------------------------------------------------------------------------------------------

    class CglibHttpRequestMethodInterceptor extends HttpRequestProxy implements MethodInterceptor {

        CglibHttpRequestMethodInterceptor(Class<?> interfaceClass) {
            super(interfaceClass);
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            return methodProxy(proxy, method, args);
        }
    }

    class JdkHttpRequestInvocationHandler extends HttpRequestProxy implements InvocationHandler {

        JdkHttpRequestInvocationHandler(Class<?> interfaceClass) {
            super(interfaceClass);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return methodProxy(proxy, method, args);
        }
    }


    //------------------------------------------------------------------------------------------------
    //                           request encapsulation and execution
    //------------------------------------------------------------------------------------------------

    /**
     * HTTP请求代理类
     */
    class HttpRequestProxy {

        /**
         * 被代理的接口Class
         */
        private final ClassContext interfaceContext;

        /**
         * 代理类的继承结构
         */
        private final Set<String> proxyClassInheritanceStructure;

        /**
         * 静态参数加载器【缓存】
         */
        private final Map<Method, StaticParamLoaderPair> staticParamLoaderMap = new ConcurrentHashMap<>(8);

        /**
         * 动态参数加载器【缓存】
         */
        private final Map<Method, DynamicParamLoader> dynamicParamLoaderMap = new ConcurrentHashMap<>(8);

        /**
         * 拦截器信息【缓存】
         */
        private final Map<Method, InterceptorPerformerChain> interceptorCacheMap = new ConcurrentHashMap<>(8);

        /**
         * 公共请求头参数【缓存】
         */
        private Map<String, Object> commonHeaderParams;

        /**
         * 公共URL参数【缓存】
         */
        private Map<String, Object> commonQueryParams;

        /**
         * 公共路径参数【缓存】
         */
        private Map<String, Object> commonPathParams;

        /**
         * 公共请求参数【缓存】
         */
        private Map<String, Object> commonRequestParams;

        /**
         * 构造方法，使用一个接口Class来初始化请求代理器
         *
         * @param interfaceClass 被代理的接口Class
         */
        HttpRequestProxy(Class<?> interfaceClass) {
            this.interfaceContext = new ClassContext(interfaceClass);
            this.interfaceContext.setHttpProxyFactory(getHttpProxyFactory());
            this.proxyClassInheritanceStructure = getProxyClassInheritanceStructure();
        }


        //----------------------------------------------------------------
        //                     Proxy Method
        //----------------------------------------------------------------


        /**
         * 方法代理，当接口方被调用时执行的就是这部分的代码
         *
         * @param proxy  代理对象
         * @param method 接口方法
         * @param args   执行方法时的参数列表
         * @return 方法执行结果，即Http请求的结果
         * @throws IOException 执行时可能会发生IO异常
         */
        public Object methodProxy(Object proxy, Method method, Object[] args) throws IOException {
            if (method.isDefault()) {
                return MethodUtils.invokeDefault(proxy, method, args);
            }
            if (ReflectionUtils.isEqualsMethod(method)) {
                return Objects.equals(proxy, args[0]);
            }
            if (ReflectionUtils.isHashCodeMethod(method)) {
                return proxy.getClass().hashCode();
            }
            if (ReflectionUtils.isToStringMethod(method)) {
                return interfaceContext.getCurrentAnnotatedElement().getName() + proxy.getClass().getSimpleName();
            }
            if (MethodUtils.isObjectMethod(method)) {
                return MethodUtils.invoke(proxy, method, args);
            }
            MethodContext methodContext = createMethodContext(proxy, method, args);
            try {
                return invokeHttpProxyMethod(methodContext);
            } finally {
                objectCreator.removeMethodContextElement(methodContext);
            }
        }

        /**
         * 创建方法上下文
         *
         * @param method 方法实例
         * @param args   参数列表
         * @return 方法上下文
         * @throws IOException IO异常
         */
        private MethodContext createMethodContext(Object proxyObject, Method method, Object[] args) throws IOException {
            interfaceContext.setProxyObject(proxyObject);
            return new MethodContext(interfaceContext, method, args);
        }

        /**
         * 执行Http代码方法
         *
         * @param methodContext 方法上下文
         * @return 方法执行结果，即Http请求的结果
         */
        private Object invokeHttpProxyMethod(MethodContext methodContext) {
            // 获取基本请求体
            Request request = createBaseRequest(methodContext);
            // 公共参数设置
            commonParamSetting(request);
            // 静态参数设置
            staticParamSetting(request, methodContext);
            // 动态参数设置
            dynamicParamSetting(request, methodContext);
            // 获取异常处理器
            HttpExceptionHandle finalExceptionHandle = getFinallyHttpExceptionHandle(methodContext);
            // 获取拦截器链
            InterceptorPerformerChain interceptorChain = createInterceptorPerformerChain(methodContext);
            // 执行拦截器逻辑
            interceptorChain.beforeExecute(request, methodContext);

            // 执行不需要解析请求体的方法
            if (methodContext.isNotAnalyzeBodyMethod()) {
                ResponseProcessor finalRespProcessor = getFinalVoidResponseProcessor(methodContext.getArguments());

                // void 方法
                if (methodContext.isVoidMethod()) {
                    if (methodContext.isAsyncMethod()) {
                        getAsyncExecutor().execute(() -> executeVoidRequest(request, methodContext, finalRespProcessor, interceptorChain, finalExceptionHandle));
                    } else {
                        executeVoidRequest(request, methodContext, finalRespProcessor, interceptorChain, finalExceptionHandle);
                    }
                    return null;
                }

                // 非void方法
                if (methodContext.isFutureMethod()) {
                    CompletableFuture<?> completableFuture = CompletableFuture.supplyAsync(() -> executeVoidRequest(request, methodContext, finalRespProcessor, interceptorChain, finalExceptionHandle), getAsyncExecutor());
                    return ListenableFuture.class.isAssignableFrom(methodContext.getReturnType())
                            ? new CompletableToListenableFutureAdapter<>(completableFuture)
                            : completableFuture;
                }
                return executeVoidRequest(request, methodContext, finalRespProcessor, interceptorChain, finalExceptionHandle);
            }

            // 执行需要解析请求体的方法
            // 执行返回值类型为Future的方法
            if (methodContext.isFutureMethod()) {
                CompletableFuture<?> completableFuture = CompletableFuture.supplyAsync(() -> executeNonVoidRequest(request, methodContext, interceptorChain, finalExceptionHandle), getAsyncExecutor());
                return ListenableFuture.class.isAssignableFrom(methodContext.getReturnType())
                        ? new CompletableToListenableFutureAdapter<>(completableFuture)
                        : completableFuture;
            }
            // 执行具有返回值的普通方法
            return executeNonVoidRequest(request, methodContext, interceptorChain, finalExceptionHandle);
        }

        //----------------------------------------------------------------
        //         request instance creation and parameter setting
        //----------------------------------------------------------------

        /**
         * 创建一个基本的请求实例
         *
         * @param methodContext 方法上下文
         * @return 基本的请求实例
         */
        private Request createBaseRequest(MethodContext methodContext) {

            // 获取接口Class中配置的域名
            String domainName = getDomainName(methodContext);
            // 获取方法中配置的Url信息
            TempPair<String, RequestMethod> httpRequestInfo = getHttpRequestInfo(methodContext);
            // 构建Request对象
            return Request.builder(StringUtils.joinUrlPath(domainName, httpRequestInfo.getOne()), httpRequestInfo.getTwo());
        }

        private String getDomainName(MethodContext context) {
            // 构建域名注解上下文
            DomainNameMeta domainMetaAnn = context.getMergedAnnotationCheckParent(DomainNameMeta.class);
            if (domainMetaAnn == null) {
                return DomainNameMeta.EMPTY;
            }
            DomainNameContext domainNameContext = new DomainNameContext(context, domainMetaAnn);

            // 获取域名获取器的创建信息并创建实例
            DomainNameGetter domainNameGetter = context.generateObject(domainMetaAnn.getter());

            // 通过域名获取器获取域名信息
            return domainNameGetter.getDomainName(domainNameContext);
        }

        private TempPair<String, RequestMethod> getHttpRequestInfo(MethodContext context) {
            HttpRequest httpReqAnn = context.getMergedAnnotationCheckParent(HttpRequest.class);
            if (httpReqAnn == null) {
                throw new HttpExecutorException("The interface method is not an HTTP method: " + context.getSimpleSignature());
            }
            HttpRequestContext httpRequestContext = new HttpRequestContext(context, httpReqAnn);
            URLGetter urlGetter = context.generateObject(httpReqAnn.urlGetter());
            String resourceURI = urlGetter.getUrl(httpRequestContext);

            return TempPair.of(resourceURI, httpReqAnn.method());
        }

        /**
         * 公共请求参数设置
         *
         * @param request 请求实例
         */
        private void commonParamSetting(Request request) {
            commonSSLSetting(request);
            commonTimeoutSetting(request);
            commonHeadersSetting(request);
            commonQueryParamsSetting(request);
            commonPathParamsSetting(request);
            commonRequestParamsSetting(request);
        }

        private void commonSSLSetting(Request request) {
            HostnameVerifier verifier = getHostnameVerifier();
            SSLSocketFactory socketFactory = getSslSocketFactory();
            if (verifier != null) {
                request.setHostnameVerifier(verifier);
            }
            if (socketFactory != null) {
                request.setSSLSocketFactory(socketFactory);
            }
        }

        private void commonTimeoutSetting(Request request) {
            Integer connectionTimeout = getConnectionTimeout();
            Integer readTimeout = getReadTimeout();
            Integer writeTimeout = getWriteTimeout();

            if (connectionTimeout != null && connectionTimeout > 0) {
                request.setConnectTimeout(connectionTimeout);
            }

            if (readTimeout != null && readTimeout > 0) {
                request.setReadTimeout(readTimeout);
            }

            if (writeTimeout != null && writeTimeout > 0) {
                request.setWriterTimeout(writeTimeout);
            }
        }

        private void commonHeadersSetting(Request request) {
            Map<String, Object> headerParams = getCommonHeaderParams();
            headerParams.forEach((n, v) -> {
                if (ContainerUtils.isIterable(v)) {
                    ContainerUtils.getIterable(v).forEach(ve -> request.addHeader(n, ve));
                } else {
                    request.addHeader(n, v);
                }
            });
        }

        private void commonQueryParamsSetting(Request request) {
            Map<String, Object> queryParams = getCommonQueryParams();
            queryParams.forEach((n, v) -> {
                if (ContainerUtils.isIterable(v)) {
                    ContainerUtils.getIterable(v).forEach(ve -> request.addQueryParameter(n, ve));
                } else {
                    request.addQueryParameter(n, v);
                }
            });
        }

        private void commonPathParamsSetting(Request request) {
            request.setPathParameter(getCommonPathParams());
        }

        private void commonRequestParamsSetting(Request request) {
            request.setRequestParameter(getCommonRequestParams());
        }

        private Map<String, Object> getCommonPathParams() {
            if (commonPathParams == null) {
                commonPathParams = getCommonMapParam(pathParams);
            }
            return commonPathParams;
        }

        private Map<String, Object> getCommonRequestParams() {
            if (commonRequestParams == null) {
                commonRequestParams = getCommonMapParam(requestParams);
            }
            return commonRequestParams;
        }

        private Map<String, Object> getCommonQueryParams() {
            if (commonQueryParams == null) {
                commonQueryParams = getCommonMapParam(queryParams);
            }
            return commonQueryParams;
        }

        private Map<String, Object> getCommonHeaderParams() {
            if (commonHeaderParams == null) {
                commonHeaderParams = getCommonMapParam(headers);
            }
            return commonHeaderParams;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> getCommonMapParam(Map<String, Object> mapParam) {
            Map<String, Object> realMapParam = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : mapParam.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (proxyClassInheritanceStructure.contains(key) && (value instanceof Map)) {
                    realMapParam.putAll((Map<? extends String, Object>) value);
                } else {
                    try {
                        Class.forName(key);
                    } catch (ClassNotFoundException e) {
                        if ((value instanceof Map)) {
                            realMapParam.put(key, new ArrayList<>(((Map<String, Object>) value).values()));
                        } else {
                            realMapParam.put(key, value);
                        }
                    }

                }
            }
            return realMapParam;
        }

        private Set<String> getProxyClassInheritanceStructure() {
            Set<String> proxyClassNameSet = new HashSet<>();
            Class<?> proxyClass = interfaceContext.getCurrentAnnotatedElement();
            while (proxyClass != null) {
                proxyClassNameSet.add(proxyClass.getName());
                proxyClass = proxyClass.getSuperclass();
            }
            return proxyClassNameSet;
        }

        private void staticParamSetting(Request request, MethodContext methodContext) {
            Method method = methodContext.getCurrentAnnotatedElement();
            this.staticParamLoaderMap.computeIfAbsent(method, key -> new StaticParamLoaderPair(methodContext))
                    .resolverAndSetter(request, methodContext);
        }


        /**
         * 解析方法运行时参数列表，并将其设置到请求实例中
         *
         * @param request       请求实例
         * @param methodContext 当前方法执行环境上下文
         */
        private void dynamicParamSetting(Request request, MethodContext methodContext) {
            Method method = methodContext.getCurrentAnnotatedElement();
            this.dynamicParamLoaderMap.computeIfAbsent(method, key -> new DynamicParamLoader(methodContext))
                    .resolverAndSetter(request, methodContext);

        }


        //----------------------------------------------------------------
        //               Extension component acquisition
        //----------------------------------------------------------------

        /**
         * 从方法参数列表中查找响应处理器{@link ResponseProcessor}，不管参数列表中有多少响应处理器实例都只会返回
         * 找到的第一个，如果参数列表中不存在任何响应处理器实例则会返回{@link ResponseProcessor#DO_NOTHING_PROCESSOR}
         *
         * @param args 运行时参数列表
         * @return 响应处理器ResponseProcessor
         */
        private ResponseProcessor getFinalVoidResponseProcessor(Object[] args) {
            if (ContainerUtils.isEmptyArray(args)) {
                return DO_NOTHING_PROCESSOR;
            }
            for (Object arg : args) {
                if (arg instanceof ResponseProcessor) {
                    return (ResponseProcessor) arg;
                }
            }
            return DO_NOTHING_PROCESSOR;
        }

        /**
         * 获取最终异常处理器{@link HttpExceptionHandle}，检测方法或接口上是否存被{@link ExceptionHandleMeta @ExceptionHandle}
         * 注解标注，如果被标注则会使用{@link ObjectCreator#newObject(ObjectGenerate, Context)}方法创建出{@link HttpExceptionHandle}实例后进行返回
         *
         * @param methodContext 当前方法上下文
         * @return 异常处理器HttpExceptionHandle
         */
        private HttpExceptionHandle getFinallyHttpExceptionHandle(MethodContext methodContext) {
            ExceptionHandleMeta handleMetaAnn = methodContext.getSameAnnotationCombined(ExceptionHandleMeta.class);
            if (handleMetaAnn != null) {
                return methodContext.generateObject(handleMetaAnn.handle());
            }
            return getExceptionHandle(methodContext);
        }

        private InterceptorPerformerChain createInterceptorPerformerChain(MethodContext methodContext) {
            Method method = methodContext.getCurrentAnnotatedElement();
            return interceptorCacheMap.computeIfAbsent(method, _m -> {
                // 构建拦截器执行链
                InterceptorPerformerChain chain = new InterceptorPerformerChain();

                // 注册通过HttpClientProxyObjectFactory添加进来的拦截器
                chain.addInterceptorPerformers(getInterceptorPerformerList(methodContext));
                // 注册类上的由@InterceptorRegister注解注册的拦截器
                interfaceContext.getContainCombinationAnnotations(InterceptorRegister.class).forEach(ann -> chain.addInterceptor(interfaceContext.toAnnotation(ann, InterceptorRegister.class), methodContext));
                // 注册方法上的由@InterceptorRegister注解注册的拦截器
                methodContext.getContainCombinationAnnotations(InterceptorRegister.class).forEach(ann -> chain.addInterceptor(methodContext.toAnnotation(ann, InterceptorRegister.class), methodContext));

                // 按优先级进行排序
                chain.sort();

                return chain;
            });
        }

        /**
         * 执行void方法，出现异常时使用异常处理器处理异常
         *
         * @param request           请求实例
         * @param methodContext     当前方法上下文
         * @param responseProcessor 响应处理器
         * @param interceptorChain  拦截器链
         * @param handle            异常处理器
         */
        private Object executeVoidRequest(Request request,
                                          MethodContext methodContext,
                                          ResponseProcessor responseProcessor,
                                          InterceptorPerformerChain interceptorChain,
                                          HttpExceptionHandle handle) {
            try {
                ResponseMetaData respMetaData;
                if (responseProcessor instanceof SaveResultResponseProcessor) {
                    respMetaData = (ResponseMetaData) retryExecute(methodContext,
                            () -> getHttpExecutor().execute(request, (SaveResultResponseProcessor) responseProcessor).getResponseMetaData());
                } else {
                    respMetaData = (ResponseMetaData) retryExecute(methodContext, () -> {
                        final AtomicReference<ResponseMetaData> meta = new AtomicReference<>();
                        getHttpExecutor().execute(request, md -> {
                            meta.set(md);
                            responseProcessor.process(md);
                        });
                        return meta.get();
                    });
                }
                VoidResponse voidResponse = new VoidResponse(respMetaData);
                // 执行相应拦截器逻辑
                interceptorChain.afterExecute(voidResponse, responseProcessor, methodContext);

                if (methodContext.isVoidMethod()) {
                    return null;
                }
                if (methodContext.isVoidResponseMethod()) {
                    return voidResponse;
                }

                VoidResultConvert voidResultConvertAnn = methodContext.getSameAnnotationCombined(VoidResultConvert.class);
                VoidResponseConvert convert = voidResultConvertAnn == null
                        ? getVoidResponseConvert(methodContext)
                        : methodContext.generateObject(voidResultConvertAnn.convert());

                return convert.convert(voidResponse, new ConvertContext(methodContext, voidResultConvertAnn));
            } catch (Throwable throwable) {
                return handle.exceptionHandler(methodContext, request, throwable);
            }
        }

        /**
         * 执行非void有返回值的方法，出现异常时使用异常处理器处理异常
         *
         * @param request       请求实例
         * @param methodContext 方法上下文
         * @param handle        异常处理器
         * @return 请求转换结果
         */
        private Object executeNonVoidRequest(Request request, MethodContext methodContext, InterceptorPerformerChain interceptorChain, HttpExceptionHandle handle) {
            try {
                Response response = (Response) retryExecute(methodContext, () -> getHttpExecutor().execute(request));

                // 执行相应拦截器逻辑
                response = interceptorChain.afterExecute(response, methodContext);

                // 是否配置了禁用转换器
                if (methodContext.isConvertProhibition()) {
                    // 默认结果处理方法
                    return response.getEntity(methodContext.getRealMethodReturnType());
                }

                // 如果存在ResponseConvert优先使用该转换器转换结果
                ResultConvert resultConvertAnn = methodContext.getSameAnnotationCombined(ResultConvert.class);
                ResponseConvert convert = resultConvertAnn == null
                        ? getResponseConvert(methodContext)
                        : (ResponseConvert) objectCreator.newObject(resultConvertAnn.convert(), methodContext);
                if (convert != null) {
                    return convert.convert(response, new ConvertContext(methodContext, resultConvertAnn));
                }
                return response.getEntity(methodContext.getRealMethodReturnType());
            } catch (Throwable throwable) {
                return handle.exceptionHandler(methodContext, request, throwable);
            }
        }
    }

    //------------------------------------------------------------------------------------------------
    //                                   retry mechanism
    //------------------------------------------------------------------------------------------------


    @SuppressWarnings("all")
    private Object retryExecute(MethodContext context, Callable<Object> task) throws Exception {
        Method method = context.getCurrentAnnotatedElement();
        RetryActuator retryActuator = retryActuatorCacheMap.computeIfAbsent(method, _m -> {
            RetryMeta retryAnn = context.getMergedAnnotationCheckParent(RetryMeta.class);
            if (retryAnn == null || context.isAnnotatedCheckParent(RetryProhibition.class)) {
                return RetryActuator.DONT_RETRY;
            } else {
                // 获取任务名和重试次数
                String taskName = retryAnn.name();
                int retryCount = retryAnn.retryCount();

                // 构建重试前运行函数对象和重试决策者对象Supplier
                Supplier<RunBeforeRetryContext> beforeRetrySupplier = () -> context.generateObject(retryAnn.beforeRetry());
                Supplier<RetryDeciderContent> deciderSupplier = () -> context.generateObject(retryAnn.decider());

                // 构建重试执行器
                return new RetryActuator(taskName, retryCount, beforeRetrySupplier, deciderSupplier, retryAnn);
            }
        });
        return retryActuator.retryExecute(task, context);
    }


    //------------------------------------------------------------------------------------------------
    //                                 static parameter cache
    //------------------------------------------------------------------------------------------------

    static class StaticParamLoaderPair {
        private final StaticParamLoader interfaceStaticParamLoader;
        private final StaticParamLoader methodStaticParamLoader;

        public StaticParamLoaderPair(MethodContext methodContext) {
            this.interfaceStaticParamLoader = new StaticParamLoader(methodContext.getClassContext());
            this.methodStaticParamLoader = new StaticParamLoader(methodContext);
        }

        public void resolverAndSetter(Request request, MethodContext methodContext) {
            interfaceStaticParamLoader.resolverAndSetter(request, methodContext);
            methodStaticParamLoader.resolverAndSetter(request, methodContext);
        }
    }
}
