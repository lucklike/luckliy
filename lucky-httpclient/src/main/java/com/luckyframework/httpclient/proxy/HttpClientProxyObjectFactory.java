package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseConvert;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.impl.DefaultRequest;
import com.luckyframework.httpclient.core.impl.SaveResultResponseProcessor;
import com.luckyframework.httpclient.proxy.annotations.Async;
import com.luckyframework.httpclient.proxy.annotations.DomainName;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.annotations.HttpParam;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.RequestProcessor;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.impl.CachedReflectObjectCreator;
import com.luckyframework.httpclient.proxy.impl.DefaultHttpExceptionHandle;
import com.luckyframework.httpclient.proxy.impl.NotRequestAfterProcessor;
import com.luckyframework.httpclient.proxy.impl.ParameterSetterWrapper;
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
import java.util.concurrent.Executor;

import static com.luckyframework.httpclient.core.ResponseProcessor.DO_NOTHING_PROCESSOR;

/**
 * Http客户端代理对象生成工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/28 15:57
 */
public class HttpClientProxyObjectFactory {

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
    private final DefaultRequest commonRequest = (DefaultRequest) Request.get("");

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


    //------------------------------------------------------------------------------------------------
    //                                 Cglib/Jdk方法拦截器
    //------------------------------------------------------------------------------------------------

    class CglibHttpRequestMethodInterceptor extends HttpRequestProxy implements MethodInterceptor {

        CglibHttpRequestMethodInterceptor(Class<?> interfaceClass) {
            super(interfaceClass);
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            return methodProxy(method, objects);
        }
    }

    class JdkHttpRequestInvocationHandler extends HttpRequestProxy implements InvocationHandler {

        JdkHttpRequestInvocationHandler(Class<?> interfaceClass) {
            super(interfaceClass);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return methodProxy(method, args);
        }
    }


    //------------------------------------------------------------------------------------------------
    //                                 Http请求逻辑封装
    //------------------------------------------------------------------------------------------------

    class HttpRequestProxy {

        private final Class<?> interfaceClass;

        HttpRequestProxy(Class<?> interfaceClass) {
            this.interfaceClass = interfaceClass;
        }

        public Object methodProxy(Method method, Object[] args) throws IOException {
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
            if (method.getReturnType() == void.class) {
                ResponseProcessor finalRespProcessor = getFinalVoidResponseProcessor(args);
                if (isAsyncMethod(method)) {
                    getExecutor().execute(() -> executeVoidRequest(request, finalRespProcessor, finalExceptionHandle));
                } else {
                    executeVoidRequest(request, finalRespProcessor, finalExceptionHandle);
                }
                return null;
            }

            // 执行非void方法
            try {
                Response response = getHttpExecutor().execute(request);
                Type methodResultType = ResolvableType.forMethodReturnType(method).getType();
                boolean ignoreClassConvert = isIgnoreClassConvert(method);

                // 如果存在ResponseConvert优先使用该转换器转换结果
                ResponseConvert bytesResponseConvert = getFinalResponseConvert(method);
                if (bytesResponseConvert != null && !ignoreClassConvert) {
                    return response.toEntity(bytesResponseConvert, methodResultType);
                }

                // 默认结果处理方法
                return response.getEntity(methodResultType);
            } catch (Exception e) {
                finalExceptionHandle.exceptionHandler(request, e);
            }
            return null;
        }

        private boolean isAsyncMethod(Method method) {
            return AnnotationUtils.isAnnotated(method, Async.class) || AnnotationUtils.isAnnotated(interfaceClass, Async.class);
        }

        private ResponseConvert getFinalResponseConvert(Method method) {
            ResultConvert methodRespConfAnn = AnnotationUtils.findMergedAnnotation(method, ResultConvert.class);
            if (methodRespConfAnn != null) {
                return objectCreator.newObject(methodRespConfAnn.value(), methodRespConfAnn.convertMsg());
            }

            ResultConvert interfaceRespConfAnn = AnnotationUtils.findMergedAnnotation(interfaceClass, ResultConvert.class);
            if (interfaceRespConfAnn != null) {
                return objectCreator.newObject(interfaceRespConfAnn.value(), interfaceRespConfAnn.convertMsg());
            }
            return getResponseConvert();
        }

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

        private boolean isIgnoreClassConvert(Method method) {
            HttpRequest httpRequestAnn = AnnotationUtils.findMergedAnnotation(method, HttpRequest.class);
            return httpRequestAnn != null && httpRequestAnn.ignoreClassConvert();
        }


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

        private HttpExceptionHandle getFinallyHttpExceptionHandle(Method method) {
            ExceptionHandle methodExceptionHandleAnn = AnnotationUtils.findMergedAnnotation(method, ExceptionHandle.class);
            if (methodExceptionHandleAnn != null) {
                return objectCreator.newObject(methodExceptionHandleAnn.value(), methodExceptionHandleAnn.handleMsg());
            }

            ExceptionHandle interfaceExceptionHandleAnn = AnnotationUtils.findMergedAnnotation(interfaceClass, ExceptionHandle.class);
            if (interfaceExceptionHandleAnn != null) {
                return objectCreator.newObject(interfaceExceptionHandleAnn.value(), interfaceExceptionHandleAnn.handleMsg());
            }
            return getExceptionHandle();
        }


        private RequestAfterProcessor getFinallyRequestAfterProcessor(Method method) {
            RequestProcessor methodReqConfAnn = AnnotationUtils.findMergedAnnotation(method, RequestProcessor.class);
            if (methodReqConfAnn != null && RequestAfterProcessor.class != methodReqConfAnn.value()) {
                return objectCreator.newObject(methodReqConfAnn.value(), methodReqConfAnn.processorMsg());
            }

            RequestProcessor interfaceReqConfAnn = AnnotationUtils.findMergedAnnotation(interfaceClass, RequestProcessor.class);
            if (interfaceReqConfAnn != null && RequestAfterProcessor.class != interfaceReqConfAnn.value()) {
                return objectCreator.newObject(interfaceReqConfAnn.value(), interfaceReqConfAnn.processorMsg());
            }
            return getRequestAfterProcessor();
        }


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

        private void methodArgsParamSetting(Request request, Method method, Object[] args) throws IOException {
            List<String> asmParamNameList = ASMUtil.getClassOrInterfaceMethodParamNames(method);
            Parameter[] parameters = method.getParameters();

            ParameterSetterWrapper paramSetterWrapper = getParameterSetterWrapperOrDefault(method, new ParameterSetterWrapper(objectCreator));
            for (int i = 0; i < method.getParameterCount(); i++) {
                Object parameterValue = args[i];
                if (parameterValue instanceof ResponseProcessor) {
                    continue;
                }

                Parameter parameter = parameters[i];
                String paramName = getParamName(parameter, asmParamNameList.get(i));
                ParameterSetterWrapper finalParamSetterWrapper = getParameterSetterWrapperOrDefault(parameter, paramSetterWrapper);
                finalParamSetterWrapper.setRequest(request, paramName, parameterValue, ResolvableType.forMethodParameter(method, i));
            }
        }

        private String getParamName(Parameter parameter, String asmParamName) {
            HttpParam httpParamAnn = AnnotationUtils.findMergedAnnotation(parameter, HttpParam.class);
            String paramName = StringUtils.hasText(asmParamName) ? asmParamName : parameter.getName();
            return (httpParamAnn != null && StringUtils.hasText(httpParamAnn.name())) ? httpParamAnn.name() : paramName;
        }

        private ParameterSetterWrapper getParameterSetterWrapperOrDefault(AnnotatedElement annotatedElement, ParameterSetterWrapper defParamSetterWrapper) {
            HttpParam httpParamAnn = AnnotationUtils.findMergedAnnotation(annotatedElement, HttpParam.class);
            if (httpParamAnn == null) {
                return defParamSetterWrapper;
            }

            ParameterSetter parameterSetter = objectCreator.newObject(httpParamAnn.paramSetter(), httpParamAnn.paramSetterMsg());
            ParameterProcessor parameterProcessor = objectCreator.newObject(httpParamAnn.paramProcessor(), httpParamAnn.paramProcessorMsg());
            return new ParameterSetterWrapper(objectCreator, parameterSetter, parameterProcessor, httpParamAnn);
        }
    }


}
