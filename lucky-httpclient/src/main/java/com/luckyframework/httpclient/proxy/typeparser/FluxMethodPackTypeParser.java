package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FluxMethodPackTypeParser implements PackTypeParser {

    private static final Logger log = LoggerFactory.getLogger(FluxMethodPackTypeParser.class);

    @Override
    public boolean canHandle(MethodContext mc) {
        return Flux.class.isAssignableFrom(mc.getReturnType());
    }

    @Override
    public ResolvableType getRealType(ResolvableType packType) {
        ResolvableType genericType = packType.hasGenerics() ? packType.getGeneric(0) : ResolvableType.forClass(Object.class);
        return ResolvableType.forClassWithGenerics(List.class, genericType);
    }

    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        CompletableFuture<Iterable<?>> completableFuture = mc.getAsyncTaskExecutor().supplyAsync(() -> {
            try {
                return (Iterable<?>) supplier.get();
            } catch (Throwable e) {
                throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
            }
        });
        return Mono.fromFuture(completableFuture).flatMapMany(Flux::fromIterable);
    }
}
