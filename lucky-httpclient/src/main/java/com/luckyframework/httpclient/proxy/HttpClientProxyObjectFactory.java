package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.impl.DefaultRequest;
import com.luckyframework.httpclient.core.impl.SaveResultResponseProcessor;
import com.luckyframework.httpclient.proxy.annotations.Async;
import com.luckyframework.httpclient.proxy.annotations.ConvertProhibition;
import com.luckyframework.httpclient.proxy.annotations.DomainName;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.RequestProcessor;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.impl.CachedReflectObjectCreator;
import com.luckyframework.httpclient.proxy.impl.DefaultHttpExceptionHandle;
import com.luckyframework.httpclient.proxy.impl.NotRequestAfterProcessor;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.ResolvableType;
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
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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
     * SpEL转换器
     */
    private static SpELConvert spELConverter = new SpELConvert();

    /**
     * 用于异步执行的Http任务的线程池
     */
    private Executor executor;

    /**
     * 对象创建器
     */
    private ObjectCreator objectCreator = new CachedReflectObjectCreator();

    /**
     * 公共请求，用于保存一些公用的请求参数
     */
    private final DefaultRequest commonRequest = Request.get("");

    /**
     * Http请求执行器
     */
    private HttpExecutor httpExecutor = new JdkHttpExecutor();

    /**
     * 异常处理器
     */
    private HttpExceptionHandle exceptionHandle = new DefaultHttpExceptionHandle();

    /**
     * 请求处理器
     */
    private RequestAfterProcessor requestAfterProcessor = new NotRequestAfterProcessor();

    /**
     * 响应装换器
     */
    private ResponseConvert responseConvert;

    public HttpClientProxyObjectFactory(Executor executor) {
        this.executor = executor;
    }

    public HttpClientProxyObjectFactory() {

    }

    public static SpELConvert getSpELConverter() {
        return spELConverter;
    }

    public static void setSpELConverter(SpELConvert spELConverter) {
        HttpClientProxyObjectFactory.spELConverter = spELConverter;
    }

    public String getBaseUrl() {
        return this.commonRequest.getUrlTemplate();
    }

    public void setBaseUrl(String baseUrl) {
        this.commonRequest.setUrlTemplate(baseUrl);
    }

    public Executor getExecutor() {
        if (executor == null) {
            executor = new SimpleAsyncTaskExecutor("http-task-");
        }
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setObjectCreator(ObjectCreator objectCreator) {
        this.objectCreator = objectCreator;
    }

    public Integer getConnectionTimeout() {
        return this.commonRequest.getConnectTimeout();
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.commonRequest.setConnectTimeout(connectionTimeout);
    }

    public Integer getReadTimeout() {
        return this.commonRequest.getReadTimeout();
    }

    public void setReadTimeout(int readTimeout) {
        this.commonRequest.setReadTimeout(readTimeout);
    }

    public Integer getWriteTimeout() {
        return this.commonRequest.getWriterTimeout();
    }

    public void setWriteTimeout(int writeTimeout) {
        this.commonRequest.setWriterTimeout(writeTimeout);
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

    public RequestAfterProcessor getRequestAfterProcessor() {
        return requestAfterProcessor;
    }

    public void setRequestAfterProcessor(RequestAfterProcessor requestAfterProcessor) {
        this.requestAfterProcessor = requestAfterProcessor;
    }

    public ResponseConvert getResponseConvert() {
        return responseConvert;
    }

    public void setResponseConvert(ResponseConvert responseConvert) {
        this.responseConvert = responseConvert;
    }

    public void addHeader(String name, Object value) {
        this.commonRequest.addHeader(name, value);
    }

    public void addPathParameter(String name, Object value) {
        this.commonRequest.addPathParameter(name, value);
    }

    public void addQueryParameter(String name, Object value) {
        this.commonRequest.addQueryParameter(name, value);
    }

    public void addRequestParameter(String name, Object value) {
        this.commonRequest.addRequestParameter(name, value);
    }

    public void setBody(BodyObject body) {
        this.commonRequest.setBody(body);
    }

    public void addInputStream(String name, String fileName, InputStream inputStream) {
        MultipartFile mf = new MultipartFile(inputStream, fileName);
        addRequestParameter(name, mf);
    }

    public void addFiles(String name, File... files) {
        addRequestParameter(name, files);
    }

    public void addFiles(String name, String... filePaths) {
        addFiles(name, ConversionUtils.conversion(filePaths, File[].class));
    }

    public void addResources(String name, Resource... resources) {
        addRequestParameter(name, resources);
    }

    public void addResources(String name, String... resourcePaths) {
        addResources(name, ConversionUtils.conversion(resourcePaths, Resource[].class));
    }

    public void addMultipartFiles(String name, MultipartFile... multipartFiles) {
        addRequestParameter(name, multipartFiles);
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
        if (this.executor instanceof ExecutorService) {
            ((ExecutorService) this.executor).shutdown();
        }
    }

    public void shutdownNow() {
        if (this.executor instanceof ExecutorService) {
            ((ExecutorService) this.executor).shutdownNow();
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

    class HttpRequestProxy {

        /**
         * 被代理的接口Class
         */
        private final Class<?> interfaceClass;

        /**
         * 构造方法，使用一个接口Class来初始化请求代理器
         *
         * @param interfaceClass 被代理的接口Class
         */
        HttpRequestProxy(Class<?> interfaceClass) {
            this.interfaceClass = interfaceClass;
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
                return proxy.getClass().toString();
            }
            return invokeHttpProxyMethod(method, args);
        }

        /**
         * 执行Http代码方法
         *
         * @param method 接口方法
         * @param args   执行方法时的参数列表
         * @return 方法执行结果，即Http请求的结果
         * @throws IOException 执行时可能会发生IO异常
         */
        public Object invokeHttpProxyMethod(Method method, Object[] args) throws IOException {

            // 获取基本请求体
            Request request = createBaseRequest(method);
            // 公共参数设置
            commonParamSetting(request);
            // 接口级别静态参数设置
            staticParamSetting(request, interfaceClass);
            // 方法级别静态参数设置
            staticParamSetting(request, method);
            // 方法参数级别参数设置
            methodArgsParamSetting(request, method, args);

            // 对请求实例进行最后的设置
            getFinallyRequestAfterProcessor(method).process(request);

            // 获取异常处理器
            HttpExceptionHandle finalExceptionHandle = getFinallyHttpExceptionHandle(method);

            // 执行void方法
            if (isVoidMethod(method)) {
                ResponseProcessor finalRespProcessor = getFinalVoidResponseProcessor(args);
                if (isAsyncMethod(method)) {
                    getExecutor().execute(() -> executeVoidRequest(request, finalRespProcessor, finalExceptionHandle));
                } else {
                    executeVoidRequest(request, finalRespProcessor, finalExceptionHandle);
                }
                return null;
            }

            // 执行返回值类型为Future的方法
            if (isFutureMethod(method)) {
                CompletableFuture<?> completableFuture = CompletableFuture.supplyAsync(
                        () -> executeNonVoidRequest(method, request, getRealMethodReturnType(method), finalExceptionHandle),
                        getExecutor()
                );
                return ListenableFuture.class.isAssignableFrom(method.getReturnType())
                        ? new CompletableToListenableFutureAdapter<>(completableFuture)
                        : completableFuture;
            }
            // 执行具有返回值的普通方法
            return executeNonVoidRequest(method, request, getRealMethodReturnType(method), finalExceptionHandle);
        }

        //----------------------------------------------------------------
        //         Request instance creation and parameter setting
        //----------------------------------------------------------------

        /**
         * 创建一个基本的请求实例
         *
         * @param method Http接口方法
         * @return 基本的请求实例
         */
        private Request createBaseRequest(Method method) {
            DomainName domainNameAnn = AnnotationUtils.findMergedAnnotation(interfaceClass, DomainName.class);

            // 获取域名
            String classUrl = "";
            if (domainNameAnn != null) {
                classUrl = StringUtils.joinUrlPath(getBaseUrl(), domainNameAnn.value());
                Class<? extends DomainNameGetter> getterClass = domainNameAnn.getter();
                String getterMsg = domainNameAnn.getterMsg();
                if (getterClass != DomainNameGetter.class || StringUtils.hasText(getterMsg)) {
                    classUrl = objectCreator.newObject(getterClass, getterMsg).getDomainName(classUrl);
                }
            }

            // 获取接口路径
            HttpRequest httpReqAnn = AnnotationUtils.findMergedAnnotation(method, HttpRequest.class);
            if (httpReqAnn == null) {
                throw new HttpExecutorException("The interface method is not an HTTP method: " + method);
            }

            return Request.builder(StringUtils.joinUrlPath(classUrl, httpReqAnn.url()), httpReqAnn.method());
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

            request.setHeaders(commonRequest.getHeaderMap());
            request.setQueryParameters(commonRequest.getQueryParameters());
            request.setPathParameter(commonRequest.getPathParameters());
            request.setRequestParameter(commonRequest.getRequestParameters());
        }

        /**
         * 静态参数设置
         *
         * @param request          请求实例
         * @param annotatedElement 注解元素
         */
        @SuppressWarnings("unchecked")
        private void staticParamSetting(Request request, AnnotatedElement annotatedElement) {
            Set<Annotation> staticParamAnnSet = AnnotationUtils.getAnnotationsByContain(annotatedElement, StaticParam.class);
            for (Annotation staticParamAnn : staticParamAnnSet) {
                Class<? extends ParameterSetter> paramSetterClass = (Class<? extends ParameterSetter>) AnnotationUtils.getValue(staticParamAnn, "paramSetter");
                String paramSetterMsg = (String) AnnotationUtils.getValue(staticParamAnn, "paramSetterMsg");
                ParameterSetter parameterSetter = objectCreator.newObject(paramSetterClass, paramSetterMsg);

                Class<? extends StaticParamResolver> paramResolverClass = (Class<? extends StaticParamResolver>) AnnotationUtils.getValue(staticParamAnn, "paramResolver");
                String paramResolverMsg = (String) AnnotationUtils.getValue(staticParamAnn, "paramResolverMsg");
                StaticParamResolver staticParamResolver = objectCreator.newObject(paramResolverClass, paramResolverMsg);

                for (TempPair<String, Object> nameValuePair : staticParamResolver.parser(staticParamAnn)) {
                    parameterSetter.set(request, nameValuePair.getOne(), nameValuePair.getTwo());
                }
            }
        }

        /**
         * 解析方法运行时参数列表，并将其设置到请求实例中
         *
         * @param request 请求实例
         * @param method  当前方法实例
         * @param args    方法运行时参数列表
         * @throws IOException 可能会出现IO异常
         */
        private void methodArgsParamSetting(Request request, Method method, Object[] args) throws IOException {
            List<String> asmParamNameList = ASMUtil.getClassOrInterfaceMethodParamNames(method);
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < method.getParameterCount(); i++) {
                Object parameterValue = args[i];
                if (parameterValue instanceof ResponseProcessor) {
                    continue;
                }
                Parameter parameter = parameters[i];
                String paramName = getParamName(parameter, asmParamNameList.get(i));
                ParameterSetterWrapper finalParamSetterWrapper = ParameterSetterWrapper.createByAnnotatedElement(parameter, objectCreator);
                finalParamSetterWrapper.setRequest(request, paramName, parameterValue, ResolvableType.forMethodParameter(method, i));
            }
        }


        //----------------------------------------------------------------
        //               Extension component acquisition
        //----------------------------------------------------------------

        /**
         * 尝试使用{@link AnnotationUtils#getCombinationAnnotation(AnnotatedElement, Class)}的方式获取方法上的{@link ResultConvert}
         * 组合注解，如果可以获取到则使用注解中的配置来构造，否则使用默认构造
         *
         * @param method 当前方法实例
         * @return 响应解析器ResponseConvert和由ResultConvert注解产生的组合注解组成的TempPair
         */
        private TempPair<ResponseConvert, Annotation> getFinalResponseConvertPair(Method method) {
            ResultConvert combinationAnnotation = AnnotationUtils.getCombinationAnnotation(method, ResultConvert.class);
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
         * @param method 当前方法实例
         * @return 异常处理器HttpExceptionHandle
         */
        private HttpExceptionHandle getFinallyHttpExceptionHandle(Method method) {
            ExceptionHandle combinationAnnotation = AnnotationUtils.getCombinationAnnotation(method, ExceptionHandle.class);
            if (combinationAnnotation != null) {
                return objectCreator.newObject(combinationAnnotation.value(), combinationAnnotation.handleMsg());
            }
            return getExceptionHandle();
        }

        /**
         * 获取最终请求处理器{@link RequestAfterProcessor}，检测方法或接口上是否存被{@link RequestProcessor @RequestProcessor}
         * 注解标注，如果被标记则解析出{@link RequestProcessor#value()}属性和{@link RequestProcessor#processorMsg()}属性
         * 并使用{@link ObjectCreator#newObject(Class, String)}方法创建出{@link RequestAfterProcessor}实例后进行返回
         *
         * @param method 当前方法实例
         * @return 请求处理器RequestAfterProcessor
         */
        private RequestAfterProcessor getFinallyRequestAfterProcessor(Method method) {
            RequestProcessor combinationReqProcessAnn = AnnotationUtils.getCombinationAnnotation(method, RequestProcessor.class);
            if (combinationReqProcessAnn != null && RequestAfterProcessor.class != combinationReqProcessAnn.value()) {
                return objectCreator.newObject(combinationReqProcessAnn.value(), combinationReqProcessAnn.processorMsg());
            }
            return getRequestAfterProcessor();
        }

        //----------------------------------------------------------------
        //             Execute the http proxy method logic
        //----------------------------------------------------------------


        /**
         * 执行void方法，出现异常时使用异常处理器处理异常
         *
         * @param request           请求实例
         * @param responseProcessor 响应处理器
         * @param handle            异常处理器
         */
        private void executeVoidRequest(Request request, ResponseProcessor responseProcessor, HttpExceptionHandle handle) {
            try {
                if (responseProcessor instanceof SaveResultResponseProcessor) {
                    getHttpExecutor().execute(request, (SaveResultResponseProcessor) responseProcessor);
                } else {
                    getHttpExecutor().execute(request, responseProcessor);
                }
            } catch (Exception e) {
                handle.exceptionHandler(request, e);
            }
        }

        /**
         * 执行非void有返回值的方法，出现异常时使用异常处理器处理异常
         *
         * @param method           当前方法实例
         * @param request          请求实例
         * @param methodResultType 方法返回值类型
         * @param handle           异常处理器
         * @return 请求转换结果
         */
        private Object executeNonVoidRequest(Method method, Request request, Type methodResultType, HttpExceptionHandle handle) {
            try {
                Response response = getHttpExecutor().execute(request);

                // 是否配置了禁用转换器
                boolean isProhibition = isConvertProhibition(method);
                if (isProhibition) {
                    // 默认结果处理方法
                    return response.getEntity(methodResultType);
                }

                // 如果存在ResponseConvert优先使用该转换器转换结果
                TempPair<ResponseConvert, Annotation> finalResponseConvertPair = getFinalResponseConvertPair(method);
                ResponseConvert convert = finalResponseConvertPair.getOne();
                Annotation annotation = finalResponseConvertPair.getTwo();
                if (convert != null) {
                    return convert.convert(response, methodResultType, annotation);
                }
                return response.getEntity(methodResultType);
            } catch (Exception e) {
                handle.exceptionHandler(request, e);
            }
            return null;
        }

        //----------------------------------------------------------------
        //                     Instrumental method
        //----------------------------------------------------------------

        /**
         * 获取方法真实返回值类型，如果方法返回值类型为{@link Future}类型需要返回其泛型类型
         * 如果是其他类型则直接返回
         *
         * @param method 当前方法实例
         * @return 方法的真实返回值类型
         */
        private Type getRealMethodReturnType(Method method) {
            if (isFutureMethod(method)) {
                ResolvableType methodReturnType = ResolvableType.forMethodReturnType(method);
                return methodReturnType.hasGenerics() ? methodReturnType.getGeneric(0).getType() : Object.class;
            }
            return ResolvableType.forMethodReturnType(method).getType();
        }


        /**
         * 判断某个方法实例是否为void方法
         *
         * @param method 待校验的方法实例
         * @return 是否是void方法
         */
        private boolean isVoidMethod(Method method) {
            return method.getReturnType() == void.class;
        }

        /**
         * 判断某个方法实例是否为异步方法，接口或方法被{@link Async @Async}注解标注的方法
         * 会被认为是一个异步方法
         *
         * @param method 待校验的方法实例
         * @return 方法是否为异步方法
         */
        private boolean isAsyncMethod(Method method) {
            return AnnotationUtils.isAnnotated(method, Async.class) || AnnotationUtils.isAnnotated(interfaceClass, Async.class);
        }

        /**
         * 判断某个方法实例是否为返回值为{@link Future}方法
         *
         * @param method 待校验的方法实例
         * @return 是否为返回值为{@link Future}方法
         */
        private boolean isFutureMethod(Method method) {
            return Future.class.isAssignableFrom(method.getReturnType());
        }

        /**
         * 是否禁用想响应结果转换器{@link ResultConvert#convert()}
         *
         * @param method 当前方法实例
         * @return 是否禁用转换器
         */
        private boolean isConvertProhibition(Method method) {
            return AnnotationUtils.isAnnotated(method, ConvertProhibition.class);
        }

        /**
         * 获取参数名称，如果参数被{@link DynamicParam @DynamicParam}注解标注了，而且{@link DynamicParam#name()}属性
         * 存在值时则优先使用该配置值，否则则使用ASM框架解析出来的名称，最后使用参数名称
         *
         * @param parameter    参数实例
         * @param asmParamName ASM框架解析得到的参数名
         * @return 参数名称
         */
        private String getParamName(Parameter parameter, String asmParamName) {
            DynamicParam dynamicParamAnn = AnnotationUtils.findMergedAnnotation(parameter, DynamicParam.class);
            String paramName = StringUtils.hasText(asmParamName) ? asmParamName : parameter.getName();
            return (dynamicParamAnn != null && StringUtils.hasText(dynamicParamAnn.name())) ? dynamicParamAnn.name() : paramName;
        }
    }


}
