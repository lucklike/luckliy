package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;


/**
 * 用于处理{@link  Mono}类型的包装类型解析器
 * <p>
 * 执行语义为热执行：代理方法被调用时（wrap阶段）立即提交异步任务，任务只会被执行一次，与
 * {@link com.luckyframework.httpclient.proxy.annotations.Async @Async}、{@link java.util.concurrent.Future}的语义保持一致。
 * <p>
 * 多个订阅者共享同一个执行结果；任意订阅者取消订阅时，若任务尚未完成则会尝试取消任务。
 * <p>
 * 注：该解析器未默认注册，使用前需手动注册到{@link com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory HttpClientProxyObjectFactory}。
 */
public class MonoMethodPackTypeParser extends SingleGenericPackTypeParser {

    private static final Logger log = LoggerFactory.getLogger(MonoMethodPackTypeParser.class);

    @Override
    public boolean canHandle(MethodContext mc) {
        return Mono.class.isAssignableFrom(mc.getReturnType());
    }


    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        // 热执行：wrap时立即提交异步任务，任务只会被执行一次
        CompletableFuture<?> completableFuture = mc.getAsyncTaskExecutor().supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
            }
        });

        return Mono.create(sink -> {
            // 将异步执行结果桥接到当前订阅者
            completableFuture.whenComplete((result, throwable) -> {
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

            sink.onCancel(() -> cancelIfNotDone(completableFuture));
            sink.onDispose(() -> cancelIfNotDone(completableFuture));
        });
    }

    /**
     * 取消未完成的任务
     *
     * @param future 任务
     */
    private static void cancelIfNotDone(CompletableFuture<?> future) {
        if (!future.isDone()) {
            future.cancel(true);
        }
    }
}
