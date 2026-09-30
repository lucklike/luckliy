package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.TextEventStream;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.exeception.RequestConstructionException;
import com.luckyframework.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * 用于处理{@link  Flux}类型的包装类型解析器
 * <p>
 * 普通List聚合返回值为热执行：代理方法被调用时（wrap阶段）立即提交异步任务，任务只会被执行一次，与
 * {@link com.luckyframework.httpclient.proxy.annotations.Async @Async}、{@link java.util.concurrent.Future}的语义保持一致；
 * 多个订阅者共享同一个执行结果，要求数据源支持重复迭代（响应转换的结果为List，满足该要求）。
 * <p>
 * {@link TextEventStream @TextEventStream}（SSE流式）为冷流：订阅时才建立HTTP连接并读取数据；
 * 受限于方法上下文（MethodContext）在代理方法返回后即被销毁，同一个Flux仅支持一次订阅，
 * 重复订阅会直接报错，需要重新调用代理方法获取新的Flux。
 * <p>
 * 注：该解析器未默认注册，使用前需手动注册到{@link com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory HttpClientProxyObjectFactory}。
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
        // 同一Flux（同一MethodContext）仅支持一次订阅：代理方法返回后MethodContext即被销毁，
        // 重复执行时无法重新构建请求（内置SpEL变量只允许首次定义），这里提前拦截并给出明确提示
        AtomicBoolean executedOnce = new AtomicBoolean(false);
        return Flux.create(sink -> {

            AtomicBoolean isCancelled = new AtomicBoolean(false);
            AtomicReference<InputStream> streamRef = new AtomicReference<>();
            mc.getAsyncTaskExecutor().execute(() -> {

                // 订阅后立即取消时，跳过请求的执行
                if (isCancelled.get()) {
                    return;
                }

                // 已执行过时不再重复执行，直接向下游发送明确的错误信息
                if (!executedOnce.compareAndSet(false, true)) {
                    sink.error(new RequestConstructionException("The SSE Flux of the method '{}' has already been subscribed once, repeated subscription is not supported. Please invoke the proxy method again to create a new Flux.",
                            MethodUtils.getLocation(mc.getCurrentAnnotatedElement())));
                    return;
                }

                Response response;
                try {
                    response = (Response) supplier.get();
                } catch (Throwable e) {
                    if (!isCancelled.get()) {
                        sink.error(e);
                    }
                    return;
                }

                try (
                        InputStream in = response.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, response.getContentType().getCharset()))
                ) {
                    // 记录输入流，取消订阅时会主动关闭该流以中断阻塞中的读取操作
                    streamRef.set(in);
                    // 说明：读取循环不受下游请求节奏约束（无背压），数据依赖Flux.create默认的BUFFER策略进行缓冲
                    String line;
                    while (!isCancelled.get() && (line = reader.readLine()) != null) {
                        sink.next(line);
                    }
                    if (!isCancelled.get()) {
                        sink.complete();
                    }
                } catch (Throwable e) {
                    // 取消订阅导致的读取中断属于正常现象，无需向下游传递异常
                    if (!isCancelled.get()) {
                        sink.error(e);
                    }
                }
            });

            // 注册取消订阅时的回调：设置取消标记并关闭输入流，使阻塞中的读取操作尽快退出
            sink.onCancel(() -> {
                isCancelled.set(true);
                closeQuietly(streamRef.get());
            });

            // 注册dispose时的回调（与onCancel类似）
            sink.onDispose(() -> {
                isCancelled.set(true);
                closeQuietly(streamRef.get());
            });

        });
    }

    private Flux<?> convertFlux(MethodContext mc, ResultSupplier supplier) {
        // 热执行：wrap时立即提交异步任务，任务只会被执行一次
        CompletableFuture<Iterable<?>> completableFuture = mc.getAsyncTaskExecutor().supplyAsync(() -> {
            try {
                return (Iterable<?>) supplier.get();
            } catch (Throwable e) {
                throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
            }
        });

        return Flux.create(sink -> {
            // 当前订阅者独立的取消标记，用于停止向当前订阅者发射元素
            AtomicBoolean isCancelled = new AtomicBoolean(false);

            // 将异步执行结果桥接到当前订阅者：
            // 所有元素直接写入sink，发射节奏与背压由Flux.create默认的BUFFER策略统一管理
            // （按下游请求节奏转发、未请求时缓冲、取消时丢弃），因此无需自行监听onRequest，
            // 也不依赖onRequest的注册时序
            completableFuture.whenComplete((iterable, throwable) -> {
                // 已取消的订阅者不再进行任何发射
                if (isCancelled.get()) {
                    return;
                }

                if (throwable != null) {
                    Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
                    sink.error(cause);
                    return;
                }

                if (iterable == null) {
                    sink.complete();
                    return;
                }

                try {
                    for (Object element : iterable) {
                        if (isCancelled.get()) {
                            return;
                        }
                        sink.next(element);
                    }
                    sink.complete();
                } catch (Throwable e) {
                    // 迭代过程出现异常时向下游传播
                    if (!isCancelled.get()) {
                        sink.error(e);
                    }
                }
            });

            // 取消订阅时的清理：停止向当前订阅者发射并尝试取消未完成的任务
            sink.onCancel(() -> {
                isCancelled.set(true);
                cancelIfNotDone(completableFuture);
            });

            sink.onDispose(() -> {
                isCancelled.set(true);
                cancelIfNotDone(completableFuture);
            });
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

    /**
     * 静默关闭输入流
     *
     * @param in 输入流
     */
    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (Throwable ignored) {
                // ignore
            }
        }
    }
}
