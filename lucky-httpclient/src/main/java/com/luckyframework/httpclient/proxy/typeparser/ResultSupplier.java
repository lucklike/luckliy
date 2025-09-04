package com.luckyframework.httpclient.proxy.typeparser;

@FunctionalInterface
public interface ResultSupplier {

    Object get() throws Throwable;
}
