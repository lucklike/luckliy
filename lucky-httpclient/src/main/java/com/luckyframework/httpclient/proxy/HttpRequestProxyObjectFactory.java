package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.BytesResultConvert;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.StringResultConvert;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.impl.SaveResultResponseProcessor;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.InputStreamSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static com.luckyframework.httpclient.core.ResponseProcessor.DO_NOTHING_PROCESSOR;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 04:36
 */
public class HttpRequestProxyObjectFactory {

    private final HttpExecutor httpExecutor;

    private RequestProcessor requestProcessor;

    public HttpRequestProxyObjectFactory(HttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    public RequestProcessor getRequestProcessor() {
        if (requestProcessor == null) {
            requestProcessor = new SpELRequestProcessor();
        }
        return requestProcessor;
    }

    public void setRequestProcessor(RequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }

    @SuppressWarnings("unchecked")
    public <T> T getCglibProxyObject(Class<T> interfaceClass) {
        return (T) ProxyFactory.getCglibProxyObject(interfaceClass, Enhancer::create, new CglibHttpRequestMethodInterceptor(interfaceClass));
    }

    @SuppressWarnings("unchecked")
    public <T> T getJdkProxyObject(Class<T> interfaceClass) {
        return (T) ProxyFactory.getJdkProxyObject(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new JdkHttpRequestInvocationHandler(interfaceClass));
    }


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

    class HttpRequestProxy {
        private final Class<?> interfaceClass;

        HttpRequestProxy(Class<?> interfaceClass) {
            this.interfaceClass = interfaceClass;
        }

        public Object methodProxy(Method method, Object[] objects) throws Exception {
            if (AnnotationUtils.isAnnotated(method, HttpRequest.class)) {

                HttpProxyAnnotationUtils.HttpRequestConfigContent reqConfigContent
                        = HttpProxyAnnotationUtils.getHttpRequestConfigContent(interfaceClass, method);
                Request request = HttpProxyAnnotationUtils.getRequest(interfaceClass, method, objects);

                RequestProcessor requestProcessor = reqConfigContent.getRequestProcessor() == null
                        ? getRequestProcessor()
                        : reqConfigContent.getRequestProcessor();
                requestProcessor.process(request);
                SaveResultResponseProcessor configProcessor = reqConfigContent.getResponseProcessor();

                ResolvableType methodReturnType = ResolvableType.forMethodReturnType(method);
                Type methodResultType = methodReturnType.getType();
                Class<?> returnTypeRawClass = methodReturnType.getRawClass();

                ResponseProcessor methodArgsResponseProcessor = getMethodArgsResponseProcessor(objects);

                if (returnTypeRawClass == void.class) {
                    httpExecutor.execute(request, methodArgsResponseProcessor == null ? DO_NOTHING_PROCESSOR : methodArgsResponseProcessor);
                    return null;
                }

                Response response = httpExecutor.execute(request, ((methodArgsResponseProcessor instanceof SaveResultResponseProcessor))
                        ? (SaveResultResponseProcessor) methodArgsResponseProcessor : configProcessor);

                boolean ignoreClassConvert = HttpProxyAnnotationUtils.isIgnoreClassConvert(interfaceClass, method);
                BytesResultConvert bytesResultConvert = reqConfigContent.getBytesConvert();
                if (bytesResultConvert != null && !ignoreClassConvert) {
                    return response.toEntity(bytesResultConvert, methodResultType);
                }

                StringResultConvert stringResultConvert = reqConfigContent.getStringConvert();
                if (stringResultConvert != null && !ignoreClassConvert) {
                    return response.toEntity(stringResultConvert, methodResultType);
                }

                if (returnTypeRawClass == MultipartFile.class) {
                    return response.getMultipartFile();
                }
                if (InputStream.class == returnTypeRawClass || ByteArrayInputStream.class == returnTypeRawClass) {
                    return response.getInputStream();
                }
                if (returnTypeRawClass == InputStreamSource.class) {
                    return response.getInputStreamSource();
                }
                if (returnTypeRawClass == String.class) {
                    return response.getStringResult();
                }
                if (returnTypeRawClass == byte[].class) {
                    return response.getResult();
                }
                return response.getEntity(methodReturnType.getType());
            }
            throw new HttpExecutorException("The interface method is not an HTTP method: " + method);
        }
    }

    private ResponseProcessor getMethodArgsResponseProcessor(Object[] args) {
        if (ContainerUtils.isEmptyArray(args)) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof ResponseProcessor) {
                return (ResponseProcessor) arg;
            }
        }
        return null;
    }
}
