package com.luckyframework.httpclient.proxy.configapi;

import java.net.Proxy;

public class ProxyConf {

    private Proxy.Type type = Proxy.Type.HTTP;

    private String ip = "";

    private String port = "";

    private String username = "";

    private String password = "";


    public Proxy.Type getType() {
        return type;
    }

    public void setType(Proxy.Type type) {
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
