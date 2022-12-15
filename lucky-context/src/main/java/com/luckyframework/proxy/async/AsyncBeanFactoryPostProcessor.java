package com.luckyframework.proxy.async;

import com.luckyframework.annotations.Async;
import com.luckyframework.annotations.Configuration;
import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.FunctionalFactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.common.TempPair;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.exception.BeansException;
import com.luckyframework.proxy.scope.BeanScopePojo;
import com.luckyframework.proxy.scope.NonSupportAopScopeProxyBeanDefinition;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.ejb.Asynchronous;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static com.luckyframework.definition.BeanDefinition.TARGET_TEMP_BEAN;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/21 11:16
 */
public class AsyncBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public final static String ASYNC_TARGET_BEAN_NAME_PREFIX = "asyncTarget.";
    public final static String DEFAULT_EXECUTOR_BEAN_NAME = "taskExecutor";
    private static Executor defaultExecutor = null;
    private static AsyncUncaughtExceptionHandler exceptionHandler = null;

    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        initializeDefaultExecutorParams(listableBeanFactory);

        String[] beanDefinitionNames = listableBeanFactory.getBeanDefinitionNames();
        for (String definitionName : beanDefinitionNames) {
            if(BeanFactoryPostProcessor.isTempTargetBeanName(listableBeanFactory, definitionName)){
                continue;
            }
            BeanDefinition beanDefinition = listableBeanFactory.getBeanDefinition(definitionName);
            ResolvableType resolvableType = beanDefinition.getResolvableType();
            Class<?> beanClass = resolvableType.getRawClass();
            if(isAsyncClass(beanClass)){
                String asyncTargetBeanName = getAsyncTargetBeanName(definitionName);

                BeanDefinition asyncProxyDefinition = beanDefinition.copy();
                FunctionalFactoryBean factoryBean = () -> TempPair.of(new AsyncProxyObjectFactory(asyncTargetBeanName, beanClass, defaultExecutor, exceptionHandler, listableBeanFactory).getAsyncProxyObject(), resolvableType);
                asyncProxyDefinition.setFactoryBean(factoryBean);
                asyncProxyDefinition.setScope(BeanScopePojo.DEF_SINGLETON);
                asyncProxyDefinition.setProxyDefinition(true);
                listableBeanFactory.removeBeanDefinition(definitionName);
                listableBeanFactory.registerBeanDefinition(definitionName, new NonSupportAopScopeProxyBeanDefinition(asyncProxyDefinition));

                beanDefinition.setRole(TARGET_TEMP_BEAN);
                listableBeanFactory.registerBeanDefinition(asyncTargetBeanName, beanDefinition);
            }

        }
    }


    private boolean isAsyncClass(Class<?> beanClass){
        if(AnnotationUtils.isAnnotated(beanClass, Configuration.class)){
            return false;
        }
        if(AnnotationUtils.isAnnotated(beanClass, Async.class) || AnnotationUtils.isAnnotated(beanClass, Asynchronous.class)){
            return true;
        }

        List<Method> asyncMethods = new ArrayList<>();
        asyncMethods.addAll(ClassUtils.getMethodByStrengthenAnnotation(beanClass, Async.class));
        asyncMethods.addAll(ClassUtils.getMethodByStrengthenAnnotation(beanClass, Asynchronous.class));
        for (Method asyncMethod : asyncMethods) {
            Class<?> returnType = asyncMethod.getReturnType();
            if(Future.class.isAssignableFrom(returnType) || void.class == returnType){
                return true;
            }
        }
        return false;
    }

    /**
     * 初始化默认线程池、异常处理器等组件
     * @param listableBeanFactory BeanFactory
     */
    public void initializeDefaultExecutorParams(VersatileBeanFactory listableBeanFactory) {

        /*
            初始化默认线程池：
            1.检测IOC容器中是否有名称为'taskExecutor'且类型为Executor的Bean，如果有则使用该线程池作为默认线程池,否则进入第二步
            2.检测IOC容器中是否存在类型为TaskExecutor的Bean，如果有则使用该线程池作为默认线程池,否则进入第三步
            3.初始化一个SimpleAsyncTaskExecutor实例作为默认线程池
         */
        if(listableBeanFactory.containsBean(DEFAULT_EXECUTOR_BEAN_NAME)
                && listableBeanFactory.isTypeMatch(DEFAULT_EXECUTOR_BEAN_NAME, Executor.class)){
            defaultExecutor = listableBeanFactory.getBean(DEFAULT_EXECUTOR_BEAN_NAME,Executor.class);
        }else{
            try {
                defaultExecutor = listableBeanFactory.getBean(TaskExecutor.class);
            }catch (BeansException ignored){
                // ignored this exception
            }
            if(defaultExecutor == null){
                defaultExecutor = new SimpleAsyncTaskExecutor("task-");
            }
        }

        /*
            初始化void方法的异常处理器：
            1.检测IOC容器中是否存在类型为AsyncUncaughtExceptionHandler的Bean，如果有则使用该异常处理器,否则进入第二步
            2.初始化一个SimpleAsyncUncaughtExceptionHandler实例来作为默认的异常处理器
         */
        try {
            exceptionHandler = listableBeanFactory.getBean(AsyncUncaughtExceptionHandler.class);
        }catch (BeansException e){
            // ignored this exception
        }
        if(exceptionHandler == null){
            exceptionHandler = new SimpleAsyncUncaughtExceptionHandler();
        }
    }

    /**
     * 使用固定前缀为原有的BeanDefinition生成新的名称
     * @param targetBeanName 真实BeanDefinition的名称
     * @return 代理BeanDefinition的bean名称
     */
    private String getAsyncTargetBeanName(String targetBeanName){
        return TEMP_BEAN_NAME_PREFIX + ASYNC_TARGET_BEAN_NAME_PREFIX + targetBeanName;
    }

}
