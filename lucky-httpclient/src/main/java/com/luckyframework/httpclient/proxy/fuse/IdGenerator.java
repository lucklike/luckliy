package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.reflect.Method;

@FunctionalInterface
public interface IdGenerator {

    Object generateId(MethodContext context, Request request);
}
