package com.luckyframework.httpclient.proxy.spel.hook;

import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;

/**
 * 生命周期
 */
public enum Lifecycle {

    /**
     * {@link ClassContext}初始化时
     */
    CLASS,

    /**
     * {@link MethodMetaContext}初始化时
     */
    METHOD_META,

    /**
     * {@link MethodContext}初始化时
     */
    METHOD,

    /**
     * 执行请求之前
     */
    REQUEST,

    /**
     * 获取响应之后
     */
    RESPONSE,

    /**
     * 发生异常时加载变量
     */
    THROWABLE,

    /**
     * 不执行
     */
    NON

}
