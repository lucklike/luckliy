package com.luckyframework.httpclient.proxy.spel;

/**
 * 生命周期钩子类型
 */
public enum HookType {

    /**
     * ROOT变量
     */
    ROOT,

    /**
     * 普通变量
     */
    VAR,

    /**
     * ROOT字面量
     */
    ROOT_LITERAL,

    /**
     * 普通字面量
     */
    VAR_LITERAL,

    /**
     * 函数
     */
    FUNCTION,

    /**
     * 回调
     */
    CALLBACK,

}
