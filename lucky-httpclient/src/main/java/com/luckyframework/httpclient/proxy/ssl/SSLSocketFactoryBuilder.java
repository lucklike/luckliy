package com.luckyframework.httpclient.proxy.ssl;

import javax.net.ssl.SSLSocketFactory;

/**
 * {@link SSLSocketFactory}构建器
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/27 23:40
 */
public interface SSLSocketFactoryBuilder {

    SSLSocketFactory getSSLSocketFactory(SSLAnnotationContext sslAnnContext);
}
