package com.luckyframework.httpclient.proxy.logging;

@FunctionalInterface
public interface CustomMasker {

    String mask(String value);
}
