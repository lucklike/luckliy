package com.luckyframework.httpclient.proxy.spel;

import org.springframework.util.Assert;

/**
 * 禁止覆盖的参数标志
 */
public interface ProhibitCoverParamMark {

    /**
     * 前缀
     */
    String getPrefix();

    /**
     * 后缀
     */
    String getSuffix();

    /**
     * 判断某个参数名是否符合‘禁止覆盖’的条件
     *匹配
     * @param param 参数名
     * @return 是否符合‘禁止覆盖’的条件
     */
    default boolean match(String param) {
        Assert.notNull(param, "param must not be null");
        return param.startsWith(getPrefix()) && param.endsWith(getSuffix());
    }
}
