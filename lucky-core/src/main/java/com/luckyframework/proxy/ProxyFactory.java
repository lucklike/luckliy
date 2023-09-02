package com.luckyframework.proxy;

import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.CallbackFilter;
import org.springframework.cglib.proxy.Enhancer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * 代理对象生成工厂
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/5/15 05:31
 */
public class ProxyFactory {

    public static Object getCglibProxyObject(Class<?> targetClass, CglibObjectCreator cglibObjectCreator, Callback[] callback, CallbackFilter callbackFilter) {
        final Enhancer enhancer = new Enhancer();
        if (targetClass.isInterface()) {
            Class<?>[] interfaces = targetClass.getInterfaces();
            Class<?>[] interfaceClasses = new Class[interfaces.length + 1];
            interfaceClasses[0] = targetClass;
            System.arraycopy(interfaces, 0, interfaceClasses, 1, interfaces.length);
            enhancer.setInterfaces(interfaceClasses);
        } else {
            enhancer.setSuperclass(targetClass);
            enhancer.setInterfaces(targetClass.getInterfaces());
        }
        enhancer.setNamingPolicy(new LuckyNamingPolicy());
        enhancer.setCallbacks(callback);
        enhancer.setCallbackFilter(callbackFilter);
        return cglibObjectCreator.createProxyObject(enhancer);
    }

    public static Object getCglibProxyObject(Class<?>[] interfaces, CglibObjectCreator cglibObjectCreator, Callback[] callback) {
        final Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces);
        enhancer.setNamingPolicy(new LuckyNamingPolicy());
        enhancer.setCallbacks(callback);
        return cglibObjectCreator.createProxyObject(enhancer);
    }

    public static Object getCglibProxyObject(Class<?> targetClass, CglibObjectCreator cglibObjectCreator, Callback callback) {
        return getCglibProxyObject(targetClass, cglibObjectCreator, new Callback[]{callback}, m -> 0);
    }

    public static Object getCglibProxyObject(Class<?>[] interfaces, CglibObjectCreator cglibObjectCreator, Callback callback) {
        return getCglibProxyObject(interfaces, cglibObjectCreator, new Callback[]{callback});
    }

    public static Object getJdkProxyObject(ClassLoader classLoader, Class<?>[] interfacesClasses, InvocationHandler invocationHandler) {
        return Proxy.newProxyInstance(classLoader, interfacesClasses, invocationHandler);
    }

    public static Object getJdkProxyObject(Class<?> interfacesClass, InvocationHandler invocationHandler) {
        return getJdkProxyObject(interfacesClass.getClassLoader(), new Class[]{interfacesClass}, invocationHandler);
    }

}
