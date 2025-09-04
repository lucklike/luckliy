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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
                isCancelled.set(true); // 设置取消标志，让读取循环退出
                // 注意：这里无法直接关闭HttpURLConnection的流，但标志位会让循环在下次检查时退出
            });

            // 注册dispose时的回调（与onCancel类似）
            sink.onDispose(() -> {
                isCancelled.set(true);
            });

        });
    }

    private Flux<?> convertFlux(MethodContext mc, ResultSupplier supplier) {
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
