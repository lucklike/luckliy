package com.luckyframework.httpclient.proxy.context;

/**
 * 函数执行器
 */
@FunctionalInterface
public interface FunExecutor {

    /**
     * 函数调用
     *
     * @param args 函数参数
     * @param <T>  结果泛型
     * @return 运行结果
     */
    <T> T call(Object... args);
}
