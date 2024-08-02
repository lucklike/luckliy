package com.luckyframework.httpclient.proxy.ssl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.ssl.SSLUtils;
import com.luckyframework.httpclient.proxy.annotations.SSL;
import com.luckyframework.spel.LazyValue;
import org.springframework.util.Assert;

import javax.net.ssl.SSLContext;
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

        // 其次使用SSLContext来生成SSLSocketFactory
        String sslContextId = sslAnn.sslContext();
        if (StringUtils.hasText(sslContextId)) {
            LazyValue<SSLContext> sslContext = sslAnnContext.getHttpProxyFactory().getSSLContext(sslAnn.sslContext());
            Assert.notNull(sslContext, "SSLContext not found, id = " + sslContextId);
            return sslContext.getValue().getSocketFactory();
        }

        // 最后使用默认的单向认证
        return SSLUtils.createIgnoreVerifySSL(sslAnnContext.parseExpression(sslAnn.protocol())).getSocketFactory();
    }
}
