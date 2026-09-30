package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.exeception.RequestConstructionException;
import com.luckyframework.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


/**
 * 用于处理{@link  Future}类型的包装类型解析器
 * <p>
 * 仅支持可以被{@link CompletableFuture}或者{@link ListenableFuture}赋值的返回类型，
 * 其他具体的Future类型（如FutureTask、ScheduledFuture）会直接抛出异常提示。
 */
public class FutureMethodPackTypeParser extends SingleGenericPackTypeParser {

    private static final Logger log = LoggerFactory.getLogger(FutureMethodPackTypeParser.class);

    @Override
    public boolean canHandle(MethodContext mc) {
        return mc.isFutureMethod();
    }

    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        Class<?> returnType = mc.getReturnType();
        boolean supportCompletableFuture = returnType.isAssignableFrom(CompletableFuture.class);
        boolean supportListenableFuture = returnType.isAssignableFrom(ListenableFuture.class);

        // 返回类型无法被CompletableFuture或者ListenableFuture赋值时，给出明确的错误提示，避免最终出现难以排查的类型转换异常
        if (!supportCompletableFuture && !supportListenableFuture) {
            throw new RequestConstructionException("Unsupported Future return type: '{}', the return type must be assignable from '{}' or '{}'. Reference method: '{}'",
                    returnType.getName(), CompletableFuture.class.getName(), ListenableFuture.class.getName(), MethodUtils.getLocation(mc.getCurrentAnnotatedElement()));
        }

        CompletableFuture<?> completableFuture = mc.getAsyncTaskExecutor().supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
            }
        });
        return supportListenableFuture
                ? new CompletableToListenableFutureAdapter<>(completableFuture)
                : completableFuture;
    }
}
