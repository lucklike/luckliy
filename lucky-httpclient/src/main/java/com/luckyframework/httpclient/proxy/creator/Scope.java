package com.luckyframework.httpclient.proxy.creator;

/**
 * 对象作用域枚举
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/6 23:34
 */
public enum Scope {

    /**
     * 单例对象 -> 全局只会产生一个对象实例
     */
    SINGLETON,

    /**
     * 原型对象 -> 每次创建对象都会新生成一个实例
     */
    PROTOTYPE,

    /**
     * 方法对象 -> 同一个代理方法中只会产生一个对象实例
     */
    METHOD,

    /**
     * 类对象 -> 同一个代理类中只会产生一个对象实例
     */
    CLASS,

    /**
     * 方法上下文对象 -> 在一个代理方法的运行期间只会产生一个对象实例
     */
    METHOD_CONTEXT
}
