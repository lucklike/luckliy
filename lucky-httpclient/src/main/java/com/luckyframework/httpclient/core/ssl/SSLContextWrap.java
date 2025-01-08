package com.luckyframework.httpclient.core.ssl;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * SSLContext包装类
 */
public class SSLContextWrap {

    private final SSLContext sslContext;
    private final KeyManager[] keyManagers;
    private final TrustManager[] trustManagers;


    public SSLContextWrap(SSLContext sslContext, KeyManager[] keyManagers, TrustManager[] trustManagers) {
        this.sslContext = sslContext;
        this.keyManagers = keyManagers;
        this.trustManagers = trustManagers;
    }

    public static SSLContextWrap wrap(SSLContext sslContext, KeyManager[] keyManagers, TrustManager[] trustManagers) {
        return new SSLContextWrap(sslContext, keyManagers, trustManagers);
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public KeyManager[] getKeyManagers() {
        return keyManagers;
    }

    public TrustManager[] getTrustManagers() {
        return trustManagers;
    }
}
