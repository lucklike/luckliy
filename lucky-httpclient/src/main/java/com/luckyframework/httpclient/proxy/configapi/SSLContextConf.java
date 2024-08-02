package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;

public class SSLContextConf {

    /**
     * 使用的协议
     */
    private String protocol = "TLS";

    /**
     * cert秘钥
     */
    @TargetField("cert-password")
    private String certPass = "";

    /**
     * KeyStore类型
     */
    @TargetField("keystore-type")
    private String keyStoreType = "JKS";

    /**
     * KeyStore公钥文件地址
     */
    @TargetField("keystore-file")
    private String keyStoreFile;

    /**
     * KeyStore秘钥
     */
    @TargetField("keystore-password")
    private String keyStorePass;


    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCertPass() {
        return certPass;
    }

    public void setCertPass(String certPass) {
        this.certPass = certPass;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    public String getKeyStorePass() {
        return keyStorePass;
    }

    public void setKeyStorePass(String keyStorePass) {
        this.keyStorePass = keyStorePass;
    }
}
