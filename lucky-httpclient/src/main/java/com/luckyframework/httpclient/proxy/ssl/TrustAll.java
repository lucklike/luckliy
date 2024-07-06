package com.luckyframework.httpclient.proxy.ssl;

import com.luckyframework.httpclient.core.exception.HttpExecutorException;
import com.luckyframework.httpclient.core.ssl.SSLUtils;
import com.luckyframework.httpclient.core.ssl.TrustAllHostnameVerifier;
import com.luckyframework.httpclient.proxy.annotations.IgnoreVerifySSL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/28 00:35
 */
public class TrustAll {

    public static class TrustAllHostnameVerifierBuilder implements HostnameVerifierBuilder {

        @Override
        public HostnameVerifier getHostnameVerifier(SSLAnnotationContext sslAnnContext) {
            return TrustAllHostnameVerifier.DEFAULT_INSTANCE;
        }
    }

    public static class IgnoreVerifySSLSocketFactoryBuilder implements SSLSocketFactoryBuilder {


        @Override
        public SSLSocketFactory getSSLSocketFactory(SSLAnnotationContext sslAnnContext) {
            try {
                IgnoreVerifySSL ignoreVerifySSL = sslAnnContext.toAnnotation(IgnoreVerifySSL.class);
                return SSLUtils.createIgnoreVerifySSL(ignoreVerifySSL.sslProtocol()).getSocketFactory();
            } catch (Exception e) {
                throw new HttpExecutorException(e);
            }
        }
    }
}
