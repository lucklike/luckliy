package com.luckyframework.httpclient.core.ssl;

import java.security.KeyStore;

/**
 * 构建证书{@link KeyStore}的关键信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/4 11:09
 */
public class KeyStoreInfo {

    /**
     * 使用的协议
     */
    private String protocol = "TLS";

    /**
     * cert秘钥
     */
    private String certPassword = "";

    /**
     * KeyStore类型
     */
    private String keyStoreType = "JKS";

    /**
     * KeyStore公钥文件地址
     */
    private String keyStoreFile;

    /**
     * KeyStore秘钥
     */
    private String keyStorePassword;


    /**
     * 获取使用的SSL协议，默认为TLS
     *
     * @return SSL协议
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * 设置使用的SSL协议，默认为TLS
     *
     * @param protocol SSL协议
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * 获取cert秘钥
     *
     * @return cert秘钥
     */
    public String getCertPassword() {
        return certPassword;
    }

    /**
     * 设置cert秘钥
     *
     * @param certPassword cert秘钥
     */
    public void setCertPassword(String certPassword) {
        this.certPassword = certPassword;
    }

    /**
     * 获取KeyStore类型，默认为JKS
     *
     * @return KeyStore类型
     */
    public String getKeyStoreType() {
        return keyStoreType;
    }

    /**
     * 设置KeyStore类型，默认为JKS
     *
     * @param keyStoreType KeyStore类型
     */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    /**
     * 获取KeyStore公钥文件地址
     *
     * @return KeyStore公钥文件地址
     */
    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    /**
     * 设置KeyStore公钥文件地址
     *
     * @param keyStoreFile KeyStore公钥文件地址
     */
    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    /**
     * 获取KeyStore秘钥
     *
     * @return KeyStore秘钥
     */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * 设置KeyStore秘钥
     *
     * @param keyStorePassword KeyStore秘钥
     */
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }
}
