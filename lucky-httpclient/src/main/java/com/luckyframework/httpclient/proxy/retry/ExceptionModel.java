package com.luckyframework.httpclient.proxy.retry;

public enum ExceptionModel {

    /***
     * 检验根异常
     */
    CHECK_ROOT_CAUSE,

    /**
     * 检验顶层异常
     */
    CHECK_TOP_CAUSE,

    /**
     * 检验所有异常堆栈中出现的异常
     */
    CHECK_ALL_STACK
}
