package com.luckyframework.httpclient.proxy.spel.var;

import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 变量作用域
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/15 02:11
 */
public enum VarScope {

    /**
     * {@link ClassContext}初始化时加载变量
     */
    CLASS,

    /**
     * {@link MethodContext}初始化时加载变量
     */
    METHOD,

    /**
     * 执行请求之前加载变量
     */
    REQUEST,

    /**
     * 获取响应之后加载变量
     */
    RESPONSE,

    /**
     * 发生异常时加载变量
     */
    THROWABLE,

    /**
     * 默认场景
     */
    DEFAULT
}
