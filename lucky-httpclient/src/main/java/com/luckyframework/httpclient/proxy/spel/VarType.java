package com.luckyframework.httpclient.proxy.spel;

/**
 * 变量类型
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/16 02:20
 */
public enum VarType {

    /**
     * ROOT变量
     */
    ROOT,

    /**
     * 环境变量
     */
    ENVIRONMENT,

    /**
     * IOC容器变量
     */
    BEAN,

    /**
     * 普通变量
     */
    NORMAL;
}
