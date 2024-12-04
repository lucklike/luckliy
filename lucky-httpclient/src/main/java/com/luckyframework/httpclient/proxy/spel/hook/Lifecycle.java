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
     * ConfigApi类初始化时
     */
    CONFIG_API_INIT,

    /**
     * ConfigApi方法初始化时
     */
    CONFIG_API_METHOD_INIT,

    /**
     * ConfigApi方法初始化完成
     */
    CONFIG_API_INIT_COMPLETE,

    /**
     * 执行请求之前
     */
    REQUEST,

    /**
     * 获取响应之后
     */
    RESPONSE,

    /**
     * 发生异常时
     */
    THROWABLE,

    /**
     * 不执行
     */
    NON

}