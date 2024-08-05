package com.luckyframework.httpclient.proxy.ssl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;
import com.luckyframework.httpclient.core.ssl.SSLException;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import javax.net.ssl.SSLSocketFactory;

/**
 * {@link SSLSocketFactory}构建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/27 23:40
 */
public interface SSLSocketFactoryBuilder {

    SSLSocketFactory getSSLSocketFactory(SSLAnnotationContext sslAnnContext);


    /**
     * 获取KeyStoreInfo对象
     *
     * @param context         方法上下文
     * @param keyStoreInfoStr KeyStoreInfo对象表达式
     * @return KeyStoreInfo对象
     */
    static KeyStoreInfo getKeyStoreInfo(MethodContext context, String keyStoreInfoStr) {
        if (StringUtils.hasText(keyStoreInfoStr)) {
            Object keyStoreInfoObject = context.parseExpression(keyStoreInfoStr, Object.class);
            if (keyStoreInfoObject instanceof KeyStoreInfo) {
                return (KeyStoreInfo) keyStoreInfoObject;
            } else if (keyStoreInfoObject instanceof String) {
                KeyStoreInfo keyStoreInfo = context.getHttpProxyFactory().getKeyStoreInfo((String) keyStoreInfoObject);
                if (keyStoreInfo != null) {
                    return keyStoreInfo;
                }
                throw new SSLException("Not found in the HttpClientProxyObjectFactory KeyStoreInfo object called '{}'", keyStoreInfoObject);
            } else {
                throw new SSLException("Failed to get KeyStoreInfo with incorrect expression: '{}'", keyStoreInfoStr);
            }
        }
        return null;
    }
}
