package com.luckyframework.httpclient.core.ssl;


import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import org.springframework.core.io.Resource;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

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
     * 创建自定义的SSL上下文
     *
     * @param sslProtocol    使用的SSL协议
     * @param keyStoreInfo   密钥库信息，提供证书给服务器端验证
     * @param trustStoreInfo 信任库信息，验证服务端提供的证书
     * @return SSL上下文，{@link SSLContext}类实例
     */
    public static SSLContext createSSLContext(String sslProtocol, KeyStoreInfo keyStoreInfo, KeyStoreInfo trustStoreInfo) {
        try {

            // 密钥库KeyManager
            KeyManager[] keyManagers = null;
            if (keyStoreInfo != null) {
                String algorithm = keyStoreInfo.getAlgorithm();
                algorithm = StringUtils.hasText(algorithm) ? algorithm : KeyManagerFactory.getDefaultAlgorithm();

                String certPassword = keyStoreInfo.getCertPassword();
                char[] certPassCharArray = StringUtils.hasText(certPassword) ? certPassword.toCharArray() : new char[0];

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
                kmf.init(keyStoreInfo.getKeyStore(), certPassCharArray);

                keyManagers = kmf.getKeyManagers();
            }

            // 信任库TrustManager
            TrustManager[] trustManagers;
            if (trustStoreInfo != null) {
                String algorithm = trustStoreInfo.getAlgorithm();
                algorithm = StringUtils.hasText(algorithm) ? algorithm : TrustManagerFactory.getDefaultAlgorithm();

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init(trustStoreInfo.getKeyStore());
                trustManagers = tmf.getTrustManagers();
            } else {
                trustManagers = TRUST_ALL_TRUST_MANAGERS;
            }

            // 协议初始化
            SSLContext sc;
            if (StringUtils.hasText(sslProtocol)) {
                sc = SSLContext.getInstance(sslProtocol);
            } else {
                sc = SSLContext.getInstance("TLS");
            }
            sc.init(keyManagers, trustManagers, new SecureRandom());
            return sc;
        } catch (Exception e) {
            throw new SSLException("Description Failed to create an SSL context.", e);
        }
    }

    /**
     * 通过KeyStoreInfo创建KeyStore
     *
     * @param keyStoreInfo KeyStoreInfo
     * @return KeyStore
     */
    public static KeyStore createKeyStore(KeyStoreInfo keyStoreInfo) {
        try (InputStream in = ConversionUtils.conversion(keyStoreInfo.getKeyStoreFile(), Resource.class).getInputStream()) {
            String type = keyStoreInfo.getKeyStoreType();
            type = StringUtils.hasText(type) ? type : KeyStore.getDefaultType();

            KeyStore keyStore = KeyStore.getInstance(type);

            String keyStorePass = keyStoreInfo.getKeyStorePassword();
            if (StringUtils.hasText(keyStorePass)) {
                keyStore.load(in, keyStorePass.trim().toCharArray());
            } else {
                keyStore.load(in, null);
            }
            return keyStore;
        } catch (Exception e) {
            throw new SSLException(e, "An exception occurred when creating the KeyStore. path: {}, type: {}", keyStoreInfo.getKeyStoreFile(), keyStoreInfo.getKeyStoreType());
        }
    }
}
