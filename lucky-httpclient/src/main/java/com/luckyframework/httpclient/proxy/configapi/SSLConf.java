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
     * KeyStore配置(ID)
     */
    @TargetField("ssl-context-id")
    private String sslContextId;

    /**
     * KeyStore配置(表达式)
     */
    @TargetField("ssl-context-expression")
    private String sslContextExpression;

    /**
     * KeyStore配置(Builder)
     */
    @TargetField("ssl-context-builder")
    private SSLContextBuilderConf sslContextBuilder;

    /**
     * KeyStore配置
     */
    @TargetField("ssl-context")
    private SSLContextConf sslContext;


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

    public String getSslContextExpression() {
        return sslContextExpression;
    }

    public void setSslContextExpression(String sslContextExpression) {
        this.sslContextExpression = sslContextExpression;
    }

    public SSLContextBuilderConf getSslContextBuilder() {
        return sslContextBuilder;
    }

    public void setSslContextBuilder(SSLContextBuilderConf sslContextBuilder) {
        this.sslContextBuilder = sslContextBuilder;
    }

    public SSLContextConf getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContextConf sslContext) {
        this.sslContext = sslContext;
    }
}
