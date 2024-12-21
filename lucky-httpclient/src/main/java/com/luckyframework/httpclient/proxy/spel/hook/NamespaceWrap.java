package com.luckyframework.httpclient.proxy.spel.hook;

import java.lang.reflect.AnnotatedElement;

public class NamespaceWrap {
    private final String namespace;
    private final AnnotatedElement source;

    private NamespaceWrap(String namespace, AnnotatedElement source) {
        this.namespace = namespace;
        this.source = source;
    }

    public static NamespaceWrap wrap(String namespace, AnnotatedElement source) {
        return new NamespaceWrap(namespace, source);
    }

    public String getNamespace() {
        return namespace;
    }

    public AnnotatedElement getSource() {
        return source;
    }
}
