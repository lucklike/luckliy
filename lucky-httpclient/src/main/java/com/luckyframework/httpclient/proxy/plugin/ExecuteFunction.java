package com.luckyframework.httpclient.proxy.plugin;

/**
 * 执行方法函数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/28 01:44
 */
@FunctionalInterface
public interface ExecuteFunction {

    /**
     * 执行执行元，返回执行结果
     *
     * @param meta 执行元
     * @return 执行结果
     * @throws Throwable 执行过程中可能产生的异常
     */
    Object execute(ExecuteMeta meta) throws Throwable;
}
