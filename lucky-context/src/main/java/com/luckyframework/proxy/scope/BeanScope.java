package com.luckyframework.proxy.scope;

/**
 * bean的作用域
 * @author fk
 * @version 1.0
 * @date 2021/4/9 0009 11:51
 */
public class BeanScope {

    /** 单例*/
    public final static String SINGLETON            = "singleton";
    /** 原型*/
    public final static String PROTOTYPE            = "prototype";
    /** 线程单例*/
    public final static String THREAD_LOCAL         = "threadLocal";

    public final static String REFRESH              = "refresh";

}
