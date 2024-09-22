package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.core.meta.Request;

public interface BodyProcessor<T> {

    T convert(Request request);

    String process(T body);
}
