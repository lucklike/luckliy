package com.luckyframework.httpclient.core.ssl;


import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.springframework.core.io.Resource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

/**
 * SSL相关的工具类
 *
 * @author fk7075
 * @version 1.0
 * @date 2024/2/27 11:00
 */
public abstract class SSLUtils {

    /**
     * 信任所有证书的证书管理器
     */
    private final static X509TrustManager[] TRUST_ALL_TRUST_MANAGERS = new X509TrustManager[]{
            new TrustAllManager()
    };

    /**
     * 默认的单向验证HTTPS请求绕过SSL验证，使用默认SSL协议
     *
     * @param sslProtocol SSL协议名称
     * @return SSL上下文，{@link SSLContext}类实例
     * @throws NoSuchAlgorithmException 没有对应加密算法异常
     * @throws KeyManagementException   Key管理异常
     */
    public static SSLContext createIgnoreVerifySSL(String sslProtocol) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc;
        if (StringUtils.hasText(sslProtocol)) {
            sc = SSLContext.getInstance(sslProtocol);
        } else {
            sc = SSLContext.getInstance("TLS");
        }
        sc.init(null, TRUST_ALL_TRUST_MANAGERS, null);
        return sc;
    }

    public static SSLContext customSSL(String sslProtocol, String certPass, String keystorePath, String keystoreType, String keystorePass) {
        return customSSL(sslProtocol, createKeyStore(keystorePath, keystoreType, keystorePass), certPass);
    }

    public static SSLContext customSSL(String sslProtocol, KeyStore keyStore, String certPass) {
        try {
            //密钥库
            char[] certPassCharArray = certPass.toCharArray();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("sunx509");
            kmf.init(keyStore, certPassCharArray);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContextBuilder scBuilder = SSLContexts.custom();
            if (StringUtils.hasText(sslProtocol)) {
                scBuilder.useProtocol(sslProtocol);
            }
            scBuilder.loadTrustMaterial(keyStore, new TrustSelfSignedStrategy());
            scBuilder.loadKeyMaterial(keyStore, certPassCharArray);
            return scBuilder.build();
        } catch (Exception e) {
            throw new LuckyRuntimeException("Description Failed to create an SSL context.", e);
        }
    }

    public static KeyStore createKeyStore(String keystorePath, String keystoreType, String keystorePass) {
        try (InputStream in = ConversionUtils.conversion(keystorePath, Resource.class).getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            if (StringUtils.hasText(keystorePass)) {
                keyStore.load(in, keystorePass.trim().toCharArray());
            } else {
                keyStore.load(in, null);
            }
            return keyStore;
        } catch (Exception e) {
            throw new LuckyRuntimeException(e, "An exception occurred when creating the KeyStore. path: {}, type: {}", keystorePath, keystoreType);
        }
    }
}
