package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.core.ResolvableType;

import java.util.Optional;

public class OptionalMethodPackTypeParser implements PackTypeParser {
    @Override
    public boolean canHandle(MethodContext mc) {
        return mc.isOptionalMethod();
    }

    @Override
    public ResolvableType getRealType(ResolvableType packType) {
        return packType.hasGenerics() ? packType.getGeneric(0) : ResolvableType.forClass(Object.class);
    }

    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        return Optional.ofNullable(supplier.get());
    }
}
