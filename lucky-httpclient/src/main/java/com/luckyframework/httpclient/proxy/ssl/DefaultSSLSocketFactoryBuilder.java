package com.luckyframework.httpclient.proxy.ssl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;
import com.luckyframework.httpclient.core.ssl.SSLException;
import com.luckyframework.httpclient.core.ssl.SSLUtils;
import com.luckyframework.httpclient.proxy.annotations.SSL;

import javax.net.ssl.SSLSocketFactory;
import java.security.KeyStore;

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
        KeyStoreInfo keyStoreInfo = getKeyStoreInfo(sslAnnContext, keyStoreStr);
        KeyStoreInfo trustStoreInfo = getKeyStoreInfo(sslAnnContext, trustStoreStr);

        // 构建KeyStore和TrustStore
        KeyStore keyStore = keyStoreInfo != null ? SSLUtils.createKeyStore(keyStoreInfo) : null;
        KeyStore trustStore = trustStoreInfo != null ? SSLUtils.createKeyStore(trustStoreInfo) : null;
        String certPassword = keyStoreInfo != null ? keyStoreInfo.getCertPassword() : null;
        String sslProtocol = StringUtils.hasText(sslAnn.protocol()) ? sslAnn.protocol() : (keyStoreInfo != null ? keyStoreInfo.getProtocol() : null);

        return SSLUtils.createSSLContext(sslProtocol, certPassword, keyStore, trustStore).getSocketFactory();
    }

    private KeyStoreInfo getKeyStoreInfo(SSLAnnotationContext sslAnnContext, String keyStoreInfoStr) {
        if (StringUtils.hasText(keyStoreInfoStr)) {
            Object keyStoreInfoObject = sslAnnContext.parseExpression(keyStoreInfoStr, Object.class);
            if (keyStoreInfoObject instanceof KeyStoreInfo) {
                return (KeyStoreInfo) keyStoreInfoObject;
            } else if (keyStoreInfoObject instanceof String){
                KeyStoreInfo keyStoreInfo = sslAnnContext.getHttpProxyFactory().getKeyStoreInfo((String) keyStoreInfoObject);
                if (keyStoreInfo != null) {
                    return keyStoreInfo;
                }
                throw new SSLException("Not found in the HttpClientProxyObjectFactory KeyStoreInfo object called {}", keyStoreInfoObject);
            } else {
                throw new SSLException("Failed to get KeyStoreInfo with incorrect expression: {}", keyStoreInfoStr);
            }
        }
        return null;
    }
}
