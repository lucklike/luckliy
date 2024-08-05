package com.luckyframework.httpclient.proxy.ssl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;
import com.luckyframework.httpclient.core.ssl.SSLUtils;
import com.luckyframework.httpclient.proxy.annotations.SSL;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import javax.net.ssl.SSLSocketFactory;

/**
 * 基于SSLContext的SSLSocketFactory构建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/28 00:35
 */
public class DefaultSSLSocketFactoryBuilder implements SSLSocketFactoryBuilder {

    @Override
    public SSLSocketFactory getSSLSocketFactory(SSLAnnotationContext sslAnnContext) {
        SSL sslAnn = sslAnnContext.toAnnotation(SSL.class);

        // 优先使用SpEL表达式来生成SSLSocketFactory
        if (StringUtils.hasText(sslAnn.sslSocketFactory())) {
            return sslAnnContext.parseExpression(sslAnn.sslSocketFactory(), SSLSocketFactory.class);
        }

        // 获取KeyStoreInfo和TrustStoreInfo
        String keyStoreStr = sslAnn.keyStore();
        String trustStoreStr = sslAnn.trustStore();

        MethodContext methodContext = sslAnnContext.getContext();
        KeyStoreInfo keyStoreInfo = SSLSocketFactoryBuilder.getKeyStoreInfo(methodContext, keyStoreStr);
        KeyStoreInfo trustStoreInfo = SSLSocketFactoryBuilder.getKeyStoreInfo(methodContext, trustStoreStr);

        return SSLUtils.createSSLContext(sslAnn.protocol(), keyStoreInfo, trustStoreInfo).getSocketFactory();
    }

}
