package com.luckyframework.httpclient.proxy.spel;

public class NamespaceWrap {
    private final String namespace;
    private final Object source;

    private NamespaceWrap(String namespace, Object source) {
        this.namespace = namespace;
        this.source = source;
    }

    public static NamespaceWrap wrap(String namespace, Object source) {
        return new NamespaceWrap(namespace, source);
    }

    public String getNamespace() {
        return namespace;
    }

    public Object getSource() {
        return source;
    }
}
