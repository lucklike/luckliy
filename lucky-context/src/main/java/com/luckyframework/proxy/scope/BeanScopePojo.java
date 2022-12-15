package com.luckyframework.proxy.scope;

import com.luckyframework.annotations.ProxyMode;

public class BeanScopePojo {

    public final static BeanScopePojo DEF_SINGLETON;
    public final static BeanScopePojo DEF_PROTOTYPE;

    static {
        DEF_SINGLETON = new BeanScopePojo(BeanScope.SINGLETON, ProxyMode.NO);
        DEF_PROTOTYPE = new BeanScopePojo(BeanScope.PROTOTYPE, ProxyMode.NO);
    }

    private final String scope;
    private final ProxyMode proxyMode;

    public BeanScopePojo(String scope, ProxyMode proxyMode) {
        this.scope = scope;
        this.proxyMode = proxyMode;
    }


    public String getScope() {
        return scope;
    }

    public ProxyMode getProxyMode() {
        return proxyMode;
    }

    public boolean isSingleton(){
        return BeanScope.SINGLETON.equals(scope);
    }

    public boolean isPrototype(){
        return BeanScope.PROTOTYPE.equals(scope);
    }

    public boolean isNeedProxy(){
        return isJdkProxy() || isCglibProxy() || ProxyMode.AUTO == proxyMode;
    }

    public boolean isJdkProxy(){
        return ProxyMode.INTERFACES == proxyMode;
    }

    public boolean isCglibProxy(){
        return ProxyMode.TARGET_CLASS == proxyMode;
    }

    @Override
    public String toString() {
        return "BeanScopePojo{" +
                "scope='" + scope + '\'' +
                ", scopedProxyMode=" + proxyMode +
                '}';
    }
}
