package com.luckyframework.proxy.async;

import com.luckyframework.annotations.Async;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.definition.BeanFactoryCglibObjectCreator;
import com.luckyframework.exception.BeansException;
import com.luckyframework.proxy.CglibObjectCreator;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;

import javax.ejb.Asynchronous;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/21 11:18
 */
public class AsyncProxyObjectFactory {


    private final String targetBeanName;
    private final Class<?> targetObjectClass;
    private final Executor defaultExecutor;
    private final AsyncUncaughtExceptionHandler exceptionHandler;
    private final VersatileBeanFactory beanFactory;

    public AsyncProxyObjectFactory(String targetBeanName, Class<?> targetObjectClass, Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler, VersatileBeanFactory beanFactory) {
        this.targetBeanName = targetBeanName;
        this.targetObjectClass = targetObjectClass;
        this.defaultExecutor = defaultExecutor;
        this.exceptionHandler = exceptionHandler;
        this.beanFactory = beanFactory;
    }

    public Object getAsyncProxyObject(){
        AsyncCglibMethodInterceptor cglibCallback = new AsyncCglibMethodInterceptor();
        CglibObjectCreator cglibObjectCreator = new BeanFactoryCglibObjectCreator(targetObjectClass, beanFactory, beanFactory.getEnvironment());
        return ProxyFactory.getCglibProxyObject(targetObjectClass, cglibObjectCreator, cglibCallback);
    }


    class AsyncCglibMethodInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            Object targetObject = beanFactory.getBean(targetBeanName);
            return isAsyncMethod(method)
                   ? invokeAsyncMethod(targetObject, method, args, methodProxy)
                   : invokeTargetMethod(targetObject, args, methodProxy);
        }

        private boolean isAsyncMethod(Method method){
            boolean isExistAsyncAnnotation =
                    AnnotationUtils.isAnnotated(targetObjectClass, Async.class) ||
                    AnnotationUtils.isAnnotated(targetObjectClass, Asynchronous.class) ||
                    AnnotationUtils.isAnnotated(method, Asynchronous.class) ||
                    AnnotationUtils.isAnnotated(method, Async.class);

            Class<?> returnType = method.getReturnType();

            boolean returnTypeIsLegal = (returnType == void.class)
                    || (Future.class.isAssignableFrom(returnType));

            return isExistAsyncAnnotation && returnTypeIsLegal;
        }

        private Object invokeTargetMethod(Object targetObject, Object[] args, MethodProxy methodProxy) throws Throwable {
            return methodProxy.invoke(targetObject, args);
        }

        private Object invokeAsyncMethod(Object targetObject, Method method, Object[] args, MethodProxy methodProxy){
            Class<?> returnType = method.getReturnType();
            String confExecutorBeanName = AnnotationUtils.isAnnotated(method, Asynchronous.class)
                                        ? ""
                                        : AnnotationUtils.findMergedAnnotation(method, Async.class).value();
            Executor executor;
            try {
                executor = StringUtils.hasText(confExecutorBeanName)
                        ? beanFactory.getBean(confExecutorBeanName,Executor.class)
                        : defaultExecutor;
            }catch (BeansException e){
                throw new AsyncExecutorBuildException(e,"Could not get Executor with bean name '"+confExecutorBeanName+"'.");
            }

            // void
            if(returnType == void.class){
                executor.execute(()-> {
                    try {
                        methodProxy.invoke(targetObject, args);
                    } catch (Throwable e) {
                        exceptionHandler.handleUncaughtException(ExceptionUtils.getCauseThrowable(e),method,args);
                    }
                });
                return null;

            }
            // Future
            else{
                Supplier<?> supplier = () -> {
                    try {
                        return ((Future<?>)methodProxy.invoke(targetObject, args)).get();
                    } catch (Throwable e) {
                        throw new AsyncExecutorException(ExceptionUtils.getCauseThrowable(e));
                    }
                };
                CompletableFuture<?> completableFuture = CompletableFuture.supplyAsync(supplier, executor);
                return ListenableFuture.class.isAssignableFrom(returnType)
                        ? new CompletableToListenableFutureAdapter<>(completableFuture)
                        : completableFuture;
            }
        }
    }

}
