package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.util.Optional;

/**
 * 用于处理{@link Optional}类型的包装类型解析器
 */
public class OptionalMethodPackTypeParser extends SingleGenericPackTypeParser {

    @Override
    public boolean canHandle(MethodContext mc) {
        return mc.isOptionalMethod();
    }

    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        return Optional.ofNullable(supplier.get());
    }

}
