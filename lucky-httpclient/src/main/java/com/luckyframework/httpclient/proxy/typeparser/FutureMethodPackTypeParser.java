package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


/**
 * 用于处理{@link  Future}类型的包装类型解析器
 */
public class FutureMethodPackTypeParser extends SingleGenericPackTypeParser {

    private static final Logger log = LoggerFactory.getLogger(FutureMethodPackTypeParser.class);

    @Override
    public boolean canHandle(MethodContext mc) {
        return mc.isFutureMethod();
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
