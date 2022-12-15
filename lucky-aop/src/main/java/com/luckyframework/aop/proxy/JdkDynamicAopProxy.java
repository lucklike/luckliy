package com.luckyframework.aop.proxy;

import com.luckyframework.aop.advisor.Advisor;
import com.luckyframework.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * 基于JDK的动态代理
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 11:55
 */
public class JdkDynamicAopProxy implements InvocationHandler, AopProxy {

    private final static  Logger logger = LoggerFactory.getLogger(JdkDynamicAopProxy.class);

    private final String beanName;
    private final Object target;
    private final List<Advisor> matchAdvisors;

    public JdkDynamicAopProxy(String beanName, Object target, List<Advisor> matchAdvisors) {
        this.beanName = beanName;
        this.target = target;
        this.matchAdvisors = matchAdvisors;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method implMethod = MethodUtils.getMethod(target.getClass(),method.getName(),method.getParameterTypes());
        return AopProxyUtils.applyAdvices(proxy,beanName,target,implMethod,args,matchAdvisors);
    }

    public String getBeanName() {
        return beanName;
    }

    @Override
    public Object getProxy() {
        return this.getProxy(target.getClass().getClassLoader());
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating jdk proxy for '"+target+"'");
        }
        return Proxy.newProxyInstance(classLoader,target.getClass().getInterfaces(),this);
    }

    @Override
    public boolean isCanProxy() {
        return true;
    }


}
