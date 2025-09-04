package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

public class FutureMethodPackTypeParser implements PackTypeParser {

    private static final Logger log = LoggerFactory.getLogger(FutureMethodPackTypeParser.class);

    @Override
    public boolean canHandle(MethodContext mc) {
        return mc.isFutureMethod();
    }

    @Override
    public ResolvableType getRealType(MethodContext mc, ResolvableType packType) {
        return packType.hasGenerics() ? packType.getGeneric(0) : ResolvableType.forClass(Object.class);
    }

    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        CompletableFuture<?> completableFuture = mc.getAsyncTaskExecutor().supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
            }
        });
        return ListenableFuture.class.isAssignableFrom(mc.getReturnType())
                ? new CompletableToListenableFutureAdapter<>(completableFuture)
                : completableFuture;
    }
}
