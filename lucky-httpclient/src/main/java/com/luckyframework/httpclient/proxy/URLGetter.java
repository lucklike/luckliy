package com.luckyframework.httpclient.proxy;

/**
 * URL地址获取器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/28 22:43
 */
@FunctionalInterface
public interface URLGetter {

    /**
     * 获取URL的方法
     */
    String getUrl(String configValue);
}
