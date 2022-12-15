package com.luckyframework.proxy.scope;

import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.definition.BeanFactoryCglibObjectCreator;
import com.luckyframework.proxy.CglibObjectCreator;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 用于生产Scope代理对象的工厂
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/20 23:56
 */
public abstract class ScopeProxyObjectFactory {

    public static Object createCglibScopeProxy(String beanName, Class<?> targetObjectClass, VersatileBeanFactory beanFactory){
        ScopeCglibMethodInterceptor cglibCallback = new ScopeCglibMethodInterceptor(beanName, beanFactory);
        CglibObjectCreator cglibObjectCreator = new BeanFactoryCglibObjectCreator(targetObjectClass, beanFactory, beanFactory.getEnvironment());
        return ProxyFactory.getCglibProxyObject(targetObjectClass, cglibObjectCreator, cglibCallback);
    }

    public static Object createJdkScopeProxy(String beanName, Class<?> targetObjectClass, VersatileBeanFactory beanFactory){
        ScopeJdkInvocationHandler invocationHandler = new ScopeJdkInvocationHandler(beanName, beanFactory);
        return ProxyFactory.getJdkProxyObject(targetObjectClass.getClassLoader(),targetObjectClass.getInterfaces(), invocationHandler);
    }


    static abstract class ScopeEnhancer{
        protected final String targetBaneName;
        protected final VersatileBeanFactory beanFactory;

        public ScopeEnhancer(String targetBaneName, VersatileBeanFactory beanFactory) {
            this.targetBaneName = targetBaneName;
            this.beanFactory = beanFactory;
        }

        protected Object getScopeTargetObject(){
            return beanFactory.getBean(targetBaneName);
        }
    }


    static class ScopeJdkInvocationHandler extends ScopeEnhancer implements InvocationHandler{

        public ScopeJdkInvocationHandler(String targetBaneName, VersatileBeanFactory beanFactory) {
            super(targetBaneName, beanFactory);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return MethodUtils.invoke(getScopeTargetObject(), method, args);
        }
    }

    static class ScopeCglibMethodInterceptor extends ScopeEnhancer implements MethodInterceptor{

        public ScopeCglibMethodInterceptor(String targetBaneName, VersatileBeanFactory beanFactory) {
            super(targetBaneName, beanFactory);
        }

        @Override
        public Object intercept(Object proxyObject, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            return methodProxy.invoke(getScopeTargetObject(), args);
        }
    }
}
