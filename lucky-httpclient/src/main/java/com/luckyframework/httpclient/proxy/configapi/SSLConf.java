package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;
import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;

/**
 * SSL相关配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/12 01:32
 */
public class SSLConf {

    /**
     * 是否启用重试
     */
    private Boolean enable;

    /**
     * 使用的SSL协议
     */
    private String protocol;

    /**
     * 主机名验证器
     */
    @TargetField("hostname-verifier")
    private String hostnameVerifier;

    /**
     * SSL Socket Factory
     */
    @TargetField("ssl-socket-factory")
    private String sslSocketFactory;

    @TargetField("key-store")
    private KeyStoreInfo keyStore;

    @TargetField("trust-store")
    private KeyStoreInfo trustStore;

    @TargetField("key-store-id")
    private String keyStoreId;

    @TargetField("trust-store-id")
    private String trustStoreId;

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHostnameVerifier() {
        return hostnameVerifier;
    }

    public KeyStoreInfo getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStoreInfo keyStore) {
        this.keyStore = keyStore;
    }

    public KeyStoreInfo getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(KeyStoreInfo trustStore) {
        this.trustStore = trustStore;
    }

    public void setHostnameVerifier(String hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public String getSslSocketFactory() {
        return sslSocketFactory;
    }

    public void setSslSocketFactory(String sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public String getKeyStoreId() {
        return keyStoreId;
    }

    public void setKeyStoreId(String keyStoreId) {
        this.keyStoreId = keyStoreId;
    }

    public String getTrustStoreId() {
        return trustStoreId;
    }

    public void setTrustStoreId(String trustStoreId) {
        this.trustStoreId = trustStoreId;
    }
}
