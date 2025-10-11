package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;


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
        return Mono.create(sink -> {
            AtomicBoolean isCancelled = new AtomicBoolean(false);

            CompletableFuture<?> completableFuture = mc.getAsyncTaskExecutor().supplyAsync(() -> {
                if (isCancelled.get()) {
                    return null; // 如果已取消，直接返回
                }
                try {
                    Object result = supplier.get();
                    if (!isCancelled.get()) {
                        return result;
                    }
                    return null;
                } catch (Throwable e) {
                    if (!isCancelled.get()) {
                        throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
                    }
                    return null;
                }
            });

            completableFuture.whenComplete((result, throwable) -> {
                if (isCancelled.get()) {
                    return; // 如果已取消，忽略结果
                }

                if (throwable != null) {
                    Throwable cause = throwable instanceof CompletionException ?
                            throwable.getCause() : throwable;
                    sink.error(cause);
                } else {
                    if (result != null) {
                        sink.success(result);
                    } else {
                        sink.success(); // Mono.empty()
                    }
                }
            });

            sink.onCancel(() -> {
                isCancelled.set(true);
                if (!completableFuture.isDone()) {
                    completableFuture.cancel(true);
                }
            });

            sink.onDispose(() -> {
                isCancelled.set(true);
                if (!completableFuture.isDone()) {
                    completableFuture.cancel(true);
                }
            });
        });
    }
}
