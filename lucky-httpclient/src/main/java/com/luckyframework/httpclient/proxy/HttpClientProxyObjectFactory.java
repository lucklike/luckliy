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
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.impl.SaveResultResponseProcessor;
import com.luckyframework.httpclient.proxy.annotations.DomainName;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.RequestInterceptorHandle;
import com.luckyframework.httpclient.proxy.annotations.ResponseInterceptorHandle;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.impl.creator.CachedReflectObjectCreator;
import com.luckyframework.httpclient.proxy.impl.DefaultHttpExceptionHandle;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

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
    private static ObjectCreator objectCreator = new CachedReflectObjectCreator();

    /**
     * SpEL转换器
     */
    private static SpELConvert spELConverter = new SpELConvert();

    /**
     * SpEL表达式参数配置
     */
    private static final Map<String, Object> expressionParams = new HashMap<>();

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
     * 公共请求头参数
     */
    private ConfigurationMap headers;

    /**
     * 公共路径请求参数
     */
    private ConfigurationMap pathParams;

    /**
     * 公共URL请求参数
     */
    private ConfigurationMap queryParams;

    /**
     * 公共请求参数
     */
    private ConfigurationMap requestParams = new ConfigurationMap();

    /**
     * 请求处理器集合
     */
    private final List<RequestInterceptor> requestInterceptorList = new ArrayList<>();

    /**
     * 请求处理器集合
     */
    private final List<ResponseInterceptor> responseInterceptorList = new ArrayList<>();

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
     * 响应转换器
     */
    private ResponseConvert responseConvert;

    public HttpClientProxyObjectFactory(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    public HttpClientProxyObjectFactory() {

    }

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

    public static void setObjectCreator(ObjectCreator objectCreator) {
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

    public HttpExecutor getHttpExecutor() {
        return httpExecutor;
    }

    public void setHttpExecutor(HttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    public HttpExceptionHandle getExceptionHandle() {
        return exceptionHandle;
    }

    public void setExceptionHandle(HttpExceptionHandle exceptionHandle) {
        this.exceptionHandle = exceptionHandle;
    }

    public List<RequestInterceptor> getRequestInterceptorList() {
        return requestInterceptorList;
    }

    public List<ResponseInterceptor> getResponseInterceptorList() {
        return responseInterceptorList;
    }

    public void addRequestInterceptors(RequestInterceptor... requestInterceptors) {
        this.requestInterceptorList.addAll(Arrays.asList(requestInterceptors));
    }

    public void addRequestInterceptors(Collection<RequestInterceptor> requestInterceptors) {
        this.requestInterceptorList.addAll(requestInterceptors);
    }

    public void addResponseInterceptors(ResponseInterceptor... responseInterceptors) {
        this.responseInterceptorList.addAll(Arrays.asList(responseInterceptors));
    }

    public void addResponseInterceptors(Collection<ResponseInterceptor> requestAfterProcessors) {
        this.responseInterceptorList.addAll(requestAfterProcessors);
    }

    public ResponseConvert getResponseConvert() {
        return responseConvert;
    }

    public void setResponseConvert(ResponseConvert responseConvert) {
        this.responseConvert = responseConvert;
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
    //                                 Cglib/Jdk方法拦截器
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
    //                                 Http请求逻辑封装
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
         * 请求拦截器信息【缓存】
         */
        private final Map<Method, List<RequestInterceptorActuator>> requestInterceptorCacheMap = new ConcurrentHashMap<>(16);

        /**
         * 响应拦截器信息【缓存】
         */
        private final Map<Method, List<ResponseInterceptorActuator>> responsetInterceptorCacheMap = new ConcurrentHashMap<>(16);

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
            this.interfaceContext.setHttpExecutor(getHttpExecutor());
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
            if (ReflectionUtils.isEqualsMethod(method)) {
                return proxy.getClass() == args[0].getClass();
            }
            if (ReflectionUtils.isHashCodeMethod(method)) {
                return proxy.getClass().hashCode();
            }
            if (ReflectionUtils.isToStringMethod(method)) {
                return interfaceContext.getCurrentAnnotatedElement().getName() + proxy.getClass().getSimpleName();
            }
            return invokeHttpProxyMethod(createMethodContext(method, args));
        }

        /**
         * 创建方法上下文
         *
         * @param method 方法实例
         * @param args   参数列表
         * @return 方法上下文
         * @throws IOException IO异常
         */
        private MethodContext createMethodContext(Method method, Object[] args) throws IOException {
            MethodContext methodContext = new MethodContext(interfaceContext, method, args);
            methodContext.setParentContext(interfaceContext);
            return methodContext;
        }

        /**
         * 执行Http代码方法
         *
         * @param methodContext 方法上下文
         * @return 方法执行结果，即Http请求的结果
         * @throws IOException 执行时可能会发生IO异常
         */
        private Object invokeHttpProxyMethod(MethodContext methodContext) throws IOException {

            // 获取基本请求体
            Request request = createBaseRequest(methodContext);
            // 公共参数设置
            commonParamSetting(request);
            // 静态参数设置
            staticParamSetting(request, methodContext);
            // 方法参数级别参数设置
            methodArgsParamSetting(request, methodContext);
            // 对最终的请求实例进行处理
            requestInterceptorProcessor(request, methodContext);

            // 获取异常处理器
            HttpExceptionHandle finalExceptionHandle = getFinallyHttpExceptionHandle(methodContext);

            // 执行void方法
            if (methodContext.isVoidMethod()) {
                ResponseProcessor finalRespProcessor = getFinalVoidResponseProcessor(methodContext.getArguments());
                if (methodContext.isAsyncMethod()) {
                    getAsyncExecutor().execute(() -> executeVoidRequest(request, methodContext, finalRespProcessor, finalExceptionHandle));
                } else {
                    executeVoidRequest(request, methodContext, finalRespProcessor, finalExceptionHandle);
                }
                return null;
            }

            // 执行返回值类型为Future的方法
            if (methodContext.isFutureMethod()) {
                CompletableFuture<?> completableFuture = CompletableFuture.supplyAsync(() -> executeNonVoidRequest(request, methodContext, finalExceptionHandle), getAsyncExecutor());
                return ListenableFuture.class.isAssignableFrom(methodContext.getReturnType()) ? new CompletableToListenableFutureAdapter<>(completableFuture) : completableFuture;
            }
            // 执行具有返回值的普通方法
            return executeNonVoidRequest(request, methodContext, finalExceptionHandle);
        }

        //----------------------------------------------------------------
        //         Request instance creation and parameter setting
        //----------------------------------------------------------------

        /**
         * 创建一个基本的请求实例
         *
         * @param methodContext 方法上下文
         * @return 基本的请求实例
         */
        private Request createBaseRequest(MethodContext methodContext) {
            DomainName domainNameAnn = methodContext.getClassContext().getMergedAnnotation(DomainName.class);
            // 获取接口Class中配置的域名
            String classUrl = "";
            if (domainNameAnn != null) {
                classUrl = createURL(domainNameAnn.getter(), domainNameAnn.getterMsg(), domainNameAnn.value(), methodContext);
            }

            // 获取方法中配置的Url信息
            HttpRequest httpReqAnn = methodContext.getMergedAnnotation(HttpRequest.class);
            if (httpReqAnn == null) {
                throw new HttpExecutorException("The interface method is not an HTTP method: " + methodContext.getClassContext());
            }

            //  组合URL信息
            String methodUrl = createURL(httpReqAnn.urlGetter(), httpReqAnn.urlGetterMsg(), httpReqAnn.url(), methodContext);
            return Request.builder(StringUtils.joinUrlPath(classUrl, methodUrl), httpReqAnn.method());
        }

        private String createURL(Class<? extends URLGetter> getterClass, String getterMsg, String configValue, MethodContext methodContext) {
            if (getterClass != URLGetter.class || StringUtils.hasText(getterMsg)) {
                return objectCreator.newObject(getterClass, getterMsg).getUrl(configValue, methodContext);
            }
            return configValue;
        }

        /**
         * 公共请求参数设置
         *
         * @param request 请求实例
         */
        private void commonParamSetting(Request request) {
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

            headersSetting(request);
            queryParamsSetting(request);
            pathParamsSetting(request);
            requestParamsSetting(request);
        }

        private void headersSetting(Request request) {
            Map<String, Object> headerParams = getCommonHeaderParams();
            headerParams.forEach((n, v) -> {
                if (ContainerUtils.isIterable(v)) {
                    ContainerUtils.getIterable(v).forEach(ve -> request.addHeader(n, ve));
                } else {
                    request.addHeader(n, v);
                }
            });
        }

        private void queryParamsSetting(Request request) {
            Map<String, Object> queryParams = getCommonQueryParams();
            queryParams.forEach((n, v) -> {
                if (ContainerUtils.isIterable(v)) {
                    ContainerUtils.getIterable(v).forEach(ve -> request.addQueryParameter(n, ve));
                } else {
                    request.addQueryParameter(n, v);
                }
            });
        }

        private void pathParamsSetting(Request request) {
            request.setPathParameter(getCommonPathParams());
        }

        private void requestParamsSetting(Request request) {
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
            this.staticParamLoaderMap
                    .computeIfAbsent(method, key -> new StaticParamLoaderPair(objectCreator, methodContext))
                    .resolverAndSetter(request, methodContext);
        }


        /**
         * 解析方法运行时参数列表，并将其设置到请求实例中
         *
         * @param request       请求实例
         * @param methodContext 当前方法执行环境上下文
         */
        private void methodArgsParamSetting(Request request, MethodContext methodContext) {
            Method method = methodContext.getCurrentAnnotatedElement();
            this.dynamicParamLoaderMap
                    .computeIfAbsent(method, key -> new DynamicParamLoader(objectCreator, methodContext))
                    .resolverAndSetter(request, methodContext);

        }


        //----------------------------------------------------------------
        //               Extension component acquisition
        //----------------------------------------------------------------

        /**
         * 尝试使用{@link AnnotationUtils#sameAnnotationCombined(AnnotatedElement, Class)}的方式获取方法上的{@link ResultConvert}
         * 组合注解，如果可以获取到则使用注解中的配置来构造，否则使用默认构造
         *
         * @param methodContext 当前方法上下文
         * @return 响应解析器ResponseConvert和由ResultConvert注解产生的组合注解组成的TempPair
         */
        private TempPair<ResponseConvert, Annotation> getFinalResponseConvertPair(MethodContext methodContext) {
            ResultConvert combinationAnnotation = methodContext.getSameAnnotationCombined(ResultConvert.class);
            if (combinationAnnotation != null) {
                ResponseConvert convert = objectCreator.newObject(combinationAnnotation.convert(), combinationAnnotation.convertMsg());
                return TempPair.of(convert, combinationAnnotation);
            }
            return TempPair.of(getResponseConvert(), () -> ResultConvert.class);
        }

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
         * 获取最终异常处理器{@link HttpExceptionHandle}，检测方法或接口上是否存被{@link ExceptionHandle @ExceptionHandle}
         * 注解标注，如果被标记则解析出{@link ExceptionHandle#value()}属性和{@link ExceptionHandle#handleMsg()}属性
         * 并使用{@link ObjectCreator#newObject(Class, String)}方法创建出{@link HttpExceptionHandle}实例后进行返回
         *
         * @param methodContext 当前方法上下文
         * @return 异常处理器HttpExceptionHandle
         */
        private HttpExceptionHandle getFinallyHttpExceptionHandle(MethodContext methodContext) {
            ExceptionHandle combinationAnnotation = methodContext.getSameAnnotationCombined(ExceptionHandle.class);
            if (combinationAnnotation != null) {
                return objectCreator.newObject(combinationAnnotation.value(), combinationAnnotation.handleMsg());
            }
            return getExceptionHandle();
        }


        /**
         * 处理最终的请求实例，收集类上以及方法上的{@link RequestInterceptorHandle}注解，使用{@link RequestInterceptorHandle#requestPriority()}进行优先级排序后依次
         * 实例化{@link RequestInterceptorHandle#requestProcessor()}指定的{@link RequestInterceptor}类的实例对请求进行处理
         *
         * @param request       最终的请求实例
         * @param methodContext 当前执行方法上下文
         */
        @SuppressWarnings("unchecked")
        private void requestInterceptorProcessor(Request request, MethodContext methodContext) {
            Method method = methodContext.getCurrentAnnotatedElement();
            if (!this.requestInterceptorCacheMap.containsKey(method)) {
                List<RequestInterceptorActuator> interceptorActuators = new ArrayList<>(8);

                // 收集HttpClientProxyObjectFactory中配置的请求拦截器
                getRequestInterceptorList().forEach(reqInter -> interceptorActuators.add(RequestInterceptorActuator.createNullType(reqInter)));

                // 收集并执行使用@RequestInterceptorHandle注解标注的请求拦截器
                // 类上的
                for (Annotation classAnn : interfaceContext.getContainCombinationAnnotations(RequestInterceptorHandle.class)) {
                    Class<? extends RequestInterceptor> reqProcessClass = (Class<? extends RequestInterceptor>) interfaceContext.getAnnotationAttribute(classAnn, "requestProcessor");
                    if (reqProcessClass != null && RequestInterceptor.class != reqProcessClass) {
                        String processorMsg = interfaceContext.getAnnotationAttribute(classAnn, "requestProcessorMsg", String.class);
                        int requestPriority = interfaceContext.getAnnotationAttribute(classAnn, "requestPriority", int.class);
                        RequestInterceptor requestInterceptor = objectCreator.newObject(reqProcessClass, processorMsg);
                        interceptorActuators.add(RequestInterceptorActuator.createClassType(requestInterceptor, classAnn, requestPriority));
                    }
                }
                // 方法上的
                for (Annotation methodAnn : methodContext.getContainCombinationAnnotations(RequestInterceptorHandle.class)) {
                    Class<? extends RequestInterceptor> reqProcessClass = (Class<? extends RequestInterceptor>) methodContext.getAnnotationAttribute(methodAnn, "requestProcessor");
                    if (reqProcessClass != null && RequestInterceptor.class != reqProcessClass) {
                        String processorMsg = methodContext.getAnnotationAttribute(methodAnn, "requestProcessorMsg", String.class);
                        int requestPriority = methodContext.getAnnotationAttribute(methodAnn, "requestPriority", int.class);
                        RequestInterceptor requestInterceptor = objectCreator.newObject(reqProcessClass, processorMsg);
                        interceptorActuators.add(RequestInterceptorActuator.createMethodType(requestInterceptor, methodAnn, requestPriority));
                    }
                }
                // 按照优先级进行排序，并加入缓存
                interceptorActuators.sort(Comparator.comparingInt(RequestInterceptorActuator::priority));
                this.requestInterceptorCacheMap.put(method, interceptorActuators);
            }
            this.requestInterceptorCacheMap.get(method).forEach(ia -> ia.activate(request, methodContext));
        }


        /**
         * 处理响应结果，收集类上以及方法上的{@link ResponseInterceptorHandle}注解，使用{@link ResponseInterceptorHandle#responsePriority()} ()}进行优先级排序后依次
         * 实例化{@link ResponseInterceptorHandle#responseProcessor()} 指定的{@link ResponseInterceptor}类的实例对请求进行处理
         *
         * @param response      响应市里
         * @param methodContext 当前执行方法上下文
         */
        @SuppressWarnings("unchecked")
        private void responseInterceptorProcessor(Response response, MethodContext methodContext) {
            Method method = methodContext.getCurrentAnnotatedElement();
            if (!this.responsetInterceptorCacheMap.containsKey(method)) {
                List<ResponseInterceptorActuator> interceptorActuators = new ArrayList<>(8);

                // 收集HttpClientProxyObjectFactory中配置的响应拦截器
                getResponseInterceptorList().forEach(respInter -> interceptorActuators.add(ResponseInterceptorActuator.createNullType(respInter)));

                // 收集并执行使用@ResponseInterceptorHandle注解标注的响应拦截器
                // 类上的
                for (Annotation classAnn : interfaceContext.getContainCombinationAnnotations(ResponseInterceptorHandle.class)) {
                    Class<? extends ResponseInterceptor> reqProcessClass = (Class<? extends ResponseInterceptor>) interfaceContext.getAnnotationAttribute(classAnn, "responseProcessor");
                    if (reqProcessClass != null && ResponseInterceptor.class != reqProcessClass) {
                        String processorMsg = interfaceContext.getAnnotationAttribute(classAnn, "responseProcessorMsg", String.class);
                        Integer responsePriority = interfaceContext.getAnnotationAttribute(classAnn, "responsePriority", int.class);
                        ResponseInterceptor responseInterceptor = objectCreator.newObject(reqProcessClass, processorMsg);
                        interceptorActuators.add(ResponseInterceptorActuator.createClassType(responseInterceptor, classAnn, responsePriority));
                    }
                }

                // 方法上的
                for (Annotation methodAnn : methodContext.getContainCombinationAnnotations(ResponseInterceptorHandle.class)) {
                    Class<? extends ResponseInterceptor> reqProcessClass = (Class<? extends ResponseInterceptor>) methodContext.getAnnotationAttribute(methodAnn, "responseProcessor");
                    if (reqProcessClass != null && ResponseInterceptor.class != reqProcessClass) {
                        String processorMsg = methodContext.getAnnotationAttribute(methodAnn, "responseProcessorMsg", String.class);
                        Integer responsePriority = methodContext.getAnnotationAttribute(methodAnn, "responsePriority", int.class);
                        ResponseInterceptor responseInterceptor = objectCreator.newObject(reqProcessClass, processorMsg);
                        interceptorActuators.add(ResponseInterceptorActuator.createMethodType(responseInterceptor, methodAnn, responsePriority));
                    }
                }
                // 按照优先级进行排序，并加入缓存
                interceptorActuators.sort(Comparator.comparingInt(ResponseInterceptorActuator::priority));
                this.responsetInterceptorCacheMap.put(method, interceptorActuators);
            }
            this.responsetInterceptorCacheMap.get(method).forEach(ia -> ia.activate(response, methodContext));

        }

        //----------------------------------------------------------------
        //             Execute the http proxy method logic
        //----------------------------------------------------------------


        /**
         * 执行void方法，出现异常时使用异常处理器处理异常
         *
         * @param request           请求实例
         * @param methodContext     当前方法上下文
         * @param responseProcessor 响应处理器
         * @param handle            异常处理器
         */
        private void executeVoidRequest(Request request, MethodContext methodContext, ResponseProcessor responseProcessor, HttpExceptionHandle handle) {
            try {
                if (responseProcessor instanceof SaveResultResponseProcessor) {
                    getHttpExecutor().execute(request, (SaveResultResponseProcessor) responseProcessor);
                } else {
                    getHttpExecutor().execute(request, responseProcessor);
                }
                // 处理原始响应结果
//                responseInterceptorProcessor(null, methodContext);
            } catch (Exception e) {
                handle.exceptionHandler(request, e);
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
        private Object executeNonVoidRequest(Request request, MethodContext methodContext, HttpExceptionHandle handle) {
            try {
                Response response = getHttpExecutor().execute(request);

                // 处理原始响应结果
                responseInterceptorProcessor(response, methodContext);

                // 是否配置了禁用转换器
                if (methodContext.isConvertProhibition()) {
                    // 默认结果处理方法
                    return response.getEntity(methodContext.getRealMethodReturnType());
                }

                // 如果存在ResponseConvert优先使用该转换器转换结果
                TempPair<ResponseConvert, Annotation> finalResponseConvertPair = getFinalResponseConvertPair(methodContext);
                ResponseConvert convert = finalResponseConvertPair.getOne();
                Annotation annotation = finalResponseConvertPair.getTwo();
                if (convert != null) {
                    return convert.convert(response, methodContext, annotation);
                }
                return response.getEntity(methodContext.getRealMethodReturnType());
            } catch (Exception e) {
                handle.exceptionHandler(request, e);
            }
            return null;
        }
    }


    //------------------------------------------------------------------------------------------------
    //                                 静态参数缓存元素
    //------------------------------------------------------------------------------------------------

    static class StaticParamLoaderPair {
        private final StaticParamLoader interfaceStaticParamLoader;
        private final StaticParamLoader methodStaticParamLoader;

        public StaticParamLoaderPair(ObjectCreator objectCreator, MethodContext methodContext) {
            this.interfaceStaticParamLoader = new StaticParamLoader(objectCreator, methodContext.getClassContext());
            this.methodStaticParamLoader = new StaticParamLoader(objectCreator, methodContext);
        }

        public void resolverAndSetter(Request request, MethodContext methodContext) {
            interfaceStaticParamLoader.resolverAndSetter(request, methodContext);
            methodStaticParamLoader.resolverAndSetter(request, methodContext);
        }
    }


    /**
     * 基本请求信息
     */
    static class RequestBaseInfo {

        /**
         * URL信息
         */
        private final String url;
        /**
         * Method信息
         */
        private final RequestMethod method;

        public RequestBaseInfo(String url, RequestMethod method) {
            this.url = url;
            this.method = method;
        }

        public Request createRequest() {
            return Request.builder(url, method);
        }

    }

}
