package com.luckyframework.proxy.scope;

public interface ScopeRegistry {

    void registerScope(String scopeName,Scope scope);

    boolean containsScope(String scopeName);

    Scope getScope(String scopeName);
}
