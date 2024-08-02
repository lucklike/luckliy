package com.luckyframework.httpclient.proxy.ssl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.exception.HttpExecutorException;
import com.luckyframework.httpclient.core.ssl.SSLUtils;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.SSL;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * 基于SSLContext的SSLSocketFactory构建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/28 00:35
 */
public class SSLContextSSLSocketFactoryBuilder implements SSLSocketFactoryBuilder {

    @Override
    public SSLSocketFactory getSSLSocketFactory(SSLAnnotationContext sslAnnContext) {
        try {
            SSLContext sslContext = getSSLContext(sslAnnContext);
            if (sslContext != null) {
                return sslContext.getSocketFactory();
            }
            return SSLUtils.createIgnoreVerifySSL(getProtocol(sslAnnContext)).getSocketFactory();
        } catch (Exception e) {
            throw new HttpExecutorException(e);
        }
    }

    private String getProtocol(SSLAnnotationContext sslAnnContext) {
        SSL sslAnn = sslAnnContext.toAnnotation(SSL.class);
        if (sslAnn != null) {
            return sslAnnContext.parseExpression(sslAnn.protocol());
        }
        return null;
    }


    private SSLContext getSSLContext(SSLAnnotationContext sslAnnContext) {
        // 尝试从注解中获取
        SSL sslAnn = sslAnnContext.toAnnotation(SSL.class);
        if (sslAnn == null) {
            return null;
        }

        ObjectGenerate sslContextBuilderOg = sslAnn.sslContextBuilder();
        if (sslContextBuilderOg.clazz() != SSLContextBuilder.class) {
            return sslAnnContext.generateObject(sslContextBuilderOg);
        }

        String annSSLContextExpression = sslAnn.sslContextExpression();
        if (StringUtils.hasText(annSSLContextExpression)) {
            return sslAnnContext.parseExpression(annSSLContextExpression, SSLContext.class);
        }

        String annSSLContextId = sslAnn.sslContext();
        if (StringUtils.hasText(annSSLContextId)) {
            return sslAnnContext.getHttpProxyFactory().getSSLContext(annSSLContextId).getValue();
        }

        return null;
    }
}
