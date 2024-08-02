package com.luckyframework.httpclient.proxy.ssl;

import com.luckyframework.httpclient.core.ssl.TrustAllHostnameVerifier;

import javax.net.ssl.HostnameVerifier;


/**
 * 信任所有主机名验证器构建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/28 00:35
 */
public class TrustAllHostnameVerifierBuilder implements HostnameVerifierBuilder {

    @Override
    public HostnameVerifier getHostnameVerifier(SSLAnnotationContext sslAnnContext) {
        return TrustAllHostnameVerifier.DEFAULT_INSTANCE;
    }

}
