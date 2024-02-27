package com.luckyframework.httpclient.proxy.ssl;

import javax.net.ssl.HostnameVerifier;

/**
 * {@link HostnameVerifier}构建器
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/27 23:32
 */
@FunctionalInterface
public interface HostnameVerifierBuilder {

    HostnameVerifier getHostnameVerifier(SSLAnnotationContext sslAnnContext);

}
