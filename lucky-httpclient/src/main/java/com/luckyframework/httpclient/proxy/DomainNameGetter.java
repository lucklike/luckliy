package com.luckyframework.httpclient.proxy;

/**
 * 域名获取器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/28 22:43
 */
public interface DomainNameGetter {

    /**
     * 获取域名的方法
     */
    String getDomainName(String configDomainName);
}
