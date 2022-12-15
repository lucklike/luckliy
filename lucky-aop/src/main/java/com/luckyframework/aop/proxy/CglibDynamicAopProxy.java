package com.luckyframework.aop.proxy;

import com.luckyframework.aop.advisor.Advisor;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.*;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 基于CGLIB的动态代理
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 14:28
 */
public class CglibDynamicAopProxy implements AopProxy, MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(CglibDynamicAopProxy.class);

    private final String beanName;
    private final Object target;
    private final List<Advisor> matchAdvisors;
    private final ApplicationContext applicationContext;
    private final boolean isSupportNestedProxy;

    public CglibDynamicAopProxy(ApplicationContext applicationContext,
                                String beanName,
                                Object target,
                                List<Advisor> matchAdvisors,
                                boolean isSupportNestedProxy
    ) {
        this.applicationContext = applicationContext;
        this.beanName = beanName;
        this.target = target;
        this.matchAdvisors = matchAdvisors;
        this.isSupportNestedProxy = isSupportNestedProxy;
    }

    public String getBeanName() {
        return beanName;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        return isSupportNestedProxy
               ? AopProxyUtils.applySupportNestingAdvices(proxy, beanName,target, method,methodProxy, args,matchAdvisors)
               : AopProxyUtils.applyAdvices(proxy,beanName, target, method, args,matchAdvisors);
    }

    @Override
    public Object getProxy() {
        return this.getProxy(target.getClass().getClassLoader());
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating cglib proxy for '"+target+"'");
        }
        return getProxyObject();
    }

    @Override
    public boolean isCanProxy() {
        return true;
    }

    private Object getProxyObject(){
        Class<?> targetClass = this.target.getClass();
        while (ClassUtils.isJDKProxy(targetClass) || ClassUtils.isCglibProxy(targetClass)){
            if(ClassUtils.isJDKProxy(targetClass)){
                return  ProxyFactory.getCglibProxyObject(targetClass.getInterfaces(),new ApplicationContextCglibObjectCreator(targetClass,applicationContext),this);
            }
            targetClass = targetClass.getSuperclass();
        }
        return  ProxyFactory.getCglibProxyObject(targetClass,new ApplicationContextCglibObjectCreator(targetClass,applicationContext),this);
    }
}
