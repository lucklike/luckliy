package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;

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
    private String protocol = "TLS";

    /**
     * SSLContext配置(ID)
     */
    @TargetField("ssl-context-id")
    private String sslContextId;

    /**
     * SSLContext配置
     */
    @TargetField("ssl-context")
    private SSLContextConf sslContext;

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

    public String getSslContextId() {
        return sslContextId;
    }

    public void setSslContextId(String sslContextId) {
        this.sslContextId = sslContextId;
    }

    public SSLContextConf getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContextConf sslContext) {
        this.sslContext = sslContext;
    }

    public String getHostnameVerifier() {
        return hostnameVerifier;
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
}
