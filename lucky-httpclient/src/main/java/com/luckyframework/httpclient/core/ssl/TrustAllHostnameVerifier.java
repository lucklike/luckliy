package com.luckyframework.httpclient.core.ssl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * 信任所有域名的域名验证器
 *
 * @author fk7075
 * @version 1.0
 * @date 2024/2/26 14:56
 */
public class TrustAllHostnameVerifier implements HostnameVerifier {

    public static final HostnameVerifier DEFAULT_INSTANCE = new TrustAllHostnameVerifier();

    @Override
    public boolean verify(String s, SSLSession sslSession) {
        return true;
    }
}
