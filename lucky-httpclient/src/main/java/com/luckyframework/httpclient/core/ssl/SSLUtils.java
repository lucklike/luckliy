package com.luckyframework.httpclient.core.ssl;


import com.luckyframework.common.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
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
}
