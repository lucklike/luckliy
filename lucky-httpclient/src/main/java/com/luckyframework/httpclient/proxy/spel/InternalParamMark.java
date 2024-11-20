package com.luckyframework.httpclient.proxy.spel;

import org.springframework.util.Assert;

/**
 * 内部变量标志
 */
public interface InternalParamMark {

    String getPrefix();

    String getSuffix();

    default boolean internalParam(String param) {
        Assert.notNull(param, "param must not be null");
        return param.startsWith(getPrefix()) && param.endsWith(getSuffix());
    }
}
