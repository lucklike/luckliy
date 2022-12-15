package com.luckyframework.aop.proxy;

/**
 * AOP代理接口，用于创建一个代理对象
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 11:54
 */
public interface AopProxy {

    /**
     * 获取一个代理对象
     * @return 代理对象
     */
    Object getProxy();

    /**
     * 传入一个类加载器，返回一个代理对象
     * @param classLoader 类加载器
     * @return 代理对象
     */
    Object getProxy(ClassLoader classLoader);

    /**
     * 是否可以被代理
     * @return true/false
     */
    boolean isCanProxy();

}
