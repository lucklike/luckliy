package com.luckyframework.httpclient.core;

import com.luckyframework.common.StringUtils;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;

/**
 * 代理信息
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/14 23:38
 */
public class ProxyInfo {

    private Proxy proxy;
    private String username = "";
    private String password = "";

    public Proxy getProxy() {
        return proxy;
    }

    public ProxyInfo setProxy(Proxy.Type type, String ip, Integer port) {
        return setProxy(new Proxy(type,  new InetSocketAddress(ip, port)));
    }

    public ProxyInfo setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public ProxyInfo setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ProxyInfo setPassword(String password) {
        this.password = password;
        return this;
    }

    public void setProxyAuthenticator(Request request) {
        if (StringUtils.hasText(username)) {
            request.setProxyAuthorization(username, password);
        }
    }

    public void setAuthenticator() {
        if (StringUtils.hasText(username)) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });
        }
    }

    public void resetAuthenticator() {
        Authenticator.setDefault(null);
    }
}
