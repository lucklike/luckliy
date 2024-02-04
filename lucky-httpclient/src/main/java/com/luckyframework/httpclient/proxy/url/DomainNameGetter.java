package com.luckyframework.httpclient.proxy.url;

/**
 * 域名获取器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/4 16:51
 */
public interface DomainNameGetter {

    /**
     * 返回一个带有协议信息和域名信息的完整URL
     *
     * @param context 域名注解上下文
     * @return 带有协议信息和域名信息的完整URL
     */
    String getDomainName(DomainNameContext context);

}
