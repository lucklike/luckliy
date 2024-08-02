package com.luckyframework.httpclient.proxy.ssl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.ssl.TrustAllHostnameVerifier;
import com.luckyframework.httpclient.proxy.annotations.SSL;

import javax.net.ssl.HostnameVerifier;


/**
 * 信任所有主机名验证器构建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/28 00:35
 */
public class DefaultHostnameVerifierBuilder implements HostnameVerifierBuilder {

    @Override
    public HostnameVerifier getHostnameVerifier(SSLAnnotationContext sslAnnContext) {
        SSL sslAnn = sslAnnContext.toAnnotation(SSL.class);
        if (StringUtils.hasText(sslAnn.hostnameVerifier())) {
            return sslAnnContext.parseExpression(sslAnn.hostnameVerifier(), HostnameVerifier.class);
        }
        return TrustAllHostnameVerifier.DEFAULT_INSTANCE;
    }

}
