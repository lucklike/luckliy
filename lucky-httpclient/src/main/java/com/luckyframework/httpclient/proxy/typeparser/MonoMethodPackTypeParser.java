package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;


/**
 * 用于处理{@link  Mono}类型的包装类型解析器
 */
public class MonoMethodPackTypeParser extends SingleGenericPackTypeParser {

    private static final Logger log = LoggerFactory.getLogger(MonoMethodPackTypeParser.class);

    @Override
    public boolean canHandle(MethodContext mc) {
        return Mono.class.isAssignableFrom(mc.getReturnType());
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
        return Mono.fromFuture(completableFuture);
    }
}
