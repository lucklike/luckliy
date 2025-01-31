package com.luckyframework.httpclient.proxy.plugin;

import com.luckyframework.httpclient.proxy.context.MethodMetaContext;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * 执行元数据
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/28 00:58
 */
public class ExecuteMeta {

    /**
     * 方法元数据上下文
     */
    private final MethodMetaContext metaContext;

    /**
     * 真实类的Class
     */
    private final Class<?> targetClass;

    /**
     * 代理对象
     */
    private final Object proxy;

    /**
     * 当前执行的代理方法
     */
    private final Method method;

    /**
     * 当前执行的代理方法代理（Cglib场景）
     */
    private final MethodProxy methodProxy;

    /**
     * 方法参数列表
     */
    private Object[] args;

    /**
     * 方法执行函数，负责执行这个方法
     */
    private final ExecuteFunction exeFunc;

    /**
     * 执行元数据构造函数
     *
     * @param metaContext 方法元数据上下文
     * @param targetClass 真实类的Class
     * @param proxy       代理对象
     * @param method      当前执行的代理方法
     * @param methodProxy 当前执行的代理方法代理（Cglib场景）
     * @param args        方法参数列表
     * @param exeFunc     方法执行函数，负责执行这个方法
     */
    public ExecuteMeta(MethodMetaContext metaContext,
                       Class<?> targetClass,
                       Object proxy,
                       Method method,
                       MethodProxy methodProxy,
                       Object[] args,
                       ExecuteFunction exeFunc
    ) {
        this.metaContext = metaContext;
        this.targetClass = targetClass;
        this.proxy = proxy;
        this.method = method;
        this.methodProxy = methodProxy;
        this.args = args;
        this.exeFunc = exeFunc;
    }

    /**
     * 获取方法元数据上下文
     *
     * @return 方法元数据上下文
     */
    public MethodMetaContext getMetaContext() {
        return metaContext;
    }

    /**
     * 获取真实类的Class
     *
     * @return 真实类的Class
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }

    /**
     * 获取当前代理对象
     *
     * @return 当前代理对象
     */
    public Object getProxy() {
        return proxy;
    }

    /**
     * 获取当前执行的方法
     *
     * @return 当前执行的方法
     */
    public Method getMethod() {
        return method;
    }

    /**
     * 获取当前执行的方法代理
     *
     * @return 当前执行的方法代理
     */
    public MethodProxy getMethodProxy() {
        return methodProxy;
    }

    /**
     * 获取当前方法的参数列表
     *
     * @return 当前方法的参数列表
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * 设置当前方法的参数列表
     *
     * @param args 方法参数列表
     */
    public void setArgs(Object[] args) {
        this.args = args;
    }

    /**
     * 执行当前正在运行的方法
     *
     * @return 运行结果
     * @throws Throwable 执行过程中可能产生的异常
     */
    public Object proceed() throws Throwable {
        return exeFunc.execute(this);
    }

    /**
     * 使用新的参数来执行当前正在运行的方法
     *
     * @param args 方法运行所需要的参数
     * @return 运行结果
     * @throws Throwable 执行过程中可能产生的异常
     */
    public Object proceed(Object[] args) throws Throwable {
        setArgs(args);
        return proceed();
    }
}
