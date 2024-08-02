package com.luckyframework.httpclient.core.ssl;

import javax.net.ssl.HostnameVerifier;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/3 02:03
 */
@FunctionalInterface
public interface HostnameVerifierFactory {

    HostnameVerifier getHostnameVerifier();
}
