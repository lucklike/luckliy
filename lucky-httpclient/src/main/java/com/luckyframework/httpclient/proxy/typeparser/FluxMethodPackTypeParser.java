package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.TextEventStream;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 用于处理{@link  Flux}类型的包装类型解析器
 */
public class FluxMethodPackTypeParser implements PackTypeParser {

    private static final Logger log = LoggerFactory.getLogger(FluxMethodPackTypeParser.class);

    @Override
    public boolean canHandle(MethodContext mc) {
        return Flux.class.isAssignableFrom(mc.getReturnType());
    }

    @Override
    public ResolvableType getRealType(MethodContext mc, ResolvableType packType) {
        if (mc.isAnnotatedCheckParent(TextEventStream.class)) {
            return ResolvableType.forClass(Response.class);
        }
        ResolvableType genericType = packType.hasGenerics() ? packType.getGeneric(0) : ResolvableType.forClass(Object.class);
        return ResolvableType.forClassWithGenerics(List.class, genericType);
    }

    @Override
    public Flux<?> wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        if (mc.isAnnotatedCheckParent(TextEventStream.class)) {
            return wrapEventStream(mc, supplier);
        }
        return convertFlux(mc, supplier);
    }

    private Flux<?> wrapEventStream(MethodContext mc, ResultSupplier supplier) {
        return Flux.create(sink -> {

            AtomicBoolean isCancelled = new AtomicBoolean(false);
            mc.getAsyncTaskExecutor().execute(() -> {

                Response response;
                try {
                    response = (Response) supplier.get();
                } catch (Throwable e) {
                    sink.error(e);
                    return;
                }

                try (
                        InputStream in = response.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, response.getContentType().getCharset()))
                ) {
                    String line;
                    while (!isCancelled.get() && (line = reader.readLine()) != null) {
                        if (isCancelled.get()) {
                            break;
                        }
                        sink.next(line);
                    }
                    if (!isCancelled.get()) {
                        sink.complete();
                    }
                } catch (Throwable e) {
                    sink.error(e);
                }
            });

            // 注册取消订阅时的回调
            sink.onCancel(() -> {
                isCancelled.set(true);
            });

            // 注册dispose时的回调（与onCancel类似）
            sink.onDispose(() -> {
                isCancelled.set(true);
            });

        });
    }

    private Flux<?> convertFlux(MethodContext mc, ResultSupplier supplier) {
        return Flux.create(sink -> {
            AtomicBoolean isCancelled = new AtomicBoolean(false);

            // 在异步线程中获取Iterable结果
            CompletableFuture<Iterable<?>> completableFuture = mc.getAsyncTaskExecutor().supplyAsync(() -> {
                try {
                    return (Iterable<?>) supplier.get();
                } catch (Throwable e) {
                    throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
                }
            });

            // 处理异步结果
            completableFuture.whenComplete((iterable, throwable) -> {
                if (throwable != null) {
                    sink.error(throwable);
                    return;
                }

                if (iterable == null) {
                    sink.complete();
                    return;
                }

                // 使用背压友好的方式发射元素
                Iterator<?> iterator = iterable.iterator();

                sink.onRequest(n -> {
                    // n 是下游请求的元素数量
                    long emitted = 0;
                    while (emitted < n && iterator.hasNext() && !isCancelled.get()) {
                        Object item = iterator.next();
                        if (!isCancelled.get()) {
                            sink.next(item);
                            emitted++;
                        }
                    }

                    // 如果已经迭代完成且没有取消
                    if (!iterator.hasNext() && !isCancelled.get()) {
                        sink.complete();
                    }
                });
            });

            // 取消订阅时的清理
            sink.onCancel(() -> {
                isCancelled.set(true);
                completableFuture.cancel(true);
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
