package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;

import java.security.KeyStore;

/**
 * SSL相关配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/12 01:32
 */
public class SSLConf {

    /**
     * 是否启用SSL功能
     */
    private Boolean enable;

    /**
     * 使用的SSL协议
     */
    private String protocol;

    /**
     * 主机名验证器
     */
    private String hostnameVerifier;

    /**
     * SSL Socket Factory
     */
    private String sslSocketFactory;

    /**
     * 构建证书{@link KeyStore}的关键信息
     */
    private KeyStoreInfo keyStoreInfo;

    /**
     * 构建证书{@link KeyStore}的关键信息，提供给服务器让服务器验证的证书配置
     */
    private KeyStoreInfo trustStoreInfo;

    /**
     * 使用配置全景配置中已经配置好的证书ID
     */
    private String keyStore;

    /**
     * 使用配置全景配置中已经配置好的证书ID，提供给服务器让服务器验证的证书配置
     */
    private String trustStore;

    /**
     * 是否启用SSL功能
     *
     * @return 是否启用SSL功能
     */
    public Boolean getEnable() {
        return enable;
    }

    /**
     * 设置是否启用SSL功能
     *
     * @param enable 是否启用SSL功能
     */
    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    /**
     * 获取使用的SSL协议
     *
     * @return 使用的SSL协议
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * 设置使用的SSL协议
     *
     * @param protocol 使用的SSL协议
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    /**
     * 获取构建证书{@link KeyStore}的关键信息
     *
     * @return 构建证书{@link KeyStore}的关键信息
     */
    public KeyStoreInfo getKeyStoreInfo() {
        return keyStoreInfo;
    }

    /**
     * 设置构建证书{@link KeyStore}的关键信息
     *
     * @param keyStoreInfo 构建证书{@link KeyStore}的关键信息
     */
    public void setKeyStoreInfo(KeyStoreInfo keyStoreInfo) {
        this.keyStoreInfo = keyStoreInfo;
    }


    /**
     * 获取构建证书{@link KeyStore}的关键信息，提供给服务器让服务器验证的证书配置
     *
     * @return 构建证书{@link KeyStore}的关键信息，提供给服务器让服务器验证的证书配置
     */
    public KeyStoreInfo getTrustStoreInfo() {
        return trustStoreInfo;
    }

    /**
     * 设置构建证书{@link KeyStore}的关键信息，提供给服务器让服务器验证的证书配置
     *
     * @param trustStoreInfo 构建证书{@link KeyStore}的关键信息，提供给服务器让服务器验证的证书配置
     */
    public void setTrustStoreInfo(KeyStoreInfo trustStoreInfo) {
        this.trustStoreInfo = trustStoreInfo;
    }

    /**
     * 设置主机名验证器
     *
     * @return 主机名验证器
     */
    public String getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * 设置主机名验证器
     *
     * @param hostnameVerifier 主机名验证器
     */
    public void setHostnameVerifier(String hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    /**
     * 获取 SSL Socket Factory
     *
     * @return SSL Socket Factory
     */
    public String getSslSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * 设置 SSL Socket Factory
     *
     * @param sslSocketFactory SSL Socket Factory
     */
    public void setSslSocketFactory(String sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     * 获取使用配置全景配置中已经配置好的证书ID
     *
     * @return 使用配置全景配置中已经配置好的证书ID
     */
    public String getKeyStore() {
        return keyStore;
    }

    /**
     * 设置使用配置全景配置中已经配置好的证书ID
     *
     * @param keyStore 使用配置全景配置中已经配置好的证书ID
     */
    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * 获取使用配置全景配置中已经配置好的证书ID，提供给服务器让服务器验证的证书配置
     *
     * @return 使用配置全景配置中已经配置好的证书ID，提供给服务器让服务器验证的证书配置
     */
    public String getTrustStore() {
        return trustStore;
    }

    /**
     * 设置使用配置全景配置中已经配置好的证书ID，提供给服务器让服务器验证的证书配置
     *
     * @param trustStore 使用配置全景配置中已经配置好的证书ID，提供给服务器让服务器验证的证书配置
     */
    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }
}
