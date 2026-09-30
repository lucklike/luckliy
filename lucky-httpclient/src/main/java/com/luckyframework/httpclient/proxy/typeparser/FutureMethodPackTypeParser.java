package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.exeception.RequestConstructionException;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;


/**
 * 用于处理{@link  Future}类型的包装类型解析器
 * <p>
 * 支持的返回类型（按优先级匹配）：
 * <pre>
 *     1.可以被{@link CompletableFuture}赋值的类型（如Future、CompletableFuture），直接返回CompletableFuture
 *     2.可以被{@link ListenableFuture}赋值的类型（如ListenableFuture），返回{@link CompletableToListenableFutureAdapter}
 *     3.可以被{@link FutureTask}赋值的类型（如FutureTask、RunnableFuture），返回已提交执行的FutureTask
 *     4.其余Future子接口（如ScheduledFuture、自定义Future接口），使用动态代理适配到CompletableFuture
 *     5.其余具体的Future实现类无法保证被正确构造，将抛出异常提示
 * </pre>
 * 所有分支均为热执行：代理方法被调用时（wrap阶段）立即提交异步任务，任务只会被执行一次。
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

        // 可以被CompletableFuture赋值的类型（如Future、CompletableFuture）
        if (returnType.isAssignableFrom(CompletableFuture.class)) {
            return supplyAsync(mc, supplier);
        }

        // 可以被Spring ListenableFuture赋值的类型（如ListenableFuture）
        if (returnType.isAssignableFrom(ListenableFuture.class)) {
            return new CompletableToListenableFutureAdapter<>(supplyAsync(mc, supplier));
        }

        // 可以被FutureTask赋值的类型（如FutureTask、RunnableFuture）
        if (returnType.isAssignableFrom(FutureTask.class)) {
            return executeFutureTask(mc, supplier);
        }

        // 其余Future子接口（如ScheduledFuture、自定义Future接口），使用动态代理适配到CompletableFuture
        if (returnType.isInterface()) {
            return futureProxy(returnType, supplyAsync(mc, supplier));
        }

        // 无法保证被正确构造的具体Future实现类，给出明确的错误提示，避免运行后期出现难以排查的类型转换异常
        throw new RequestConstructionException("Unsupported Future return type: '{}', the return type must be assignable from '{}', '{}' or '{}', or be a Future interface. Reference method: '{}'",
                returnType.getName(), CompletableFuture.class.getName(), ListenableFuture.class.getName(), FutureTask.class.getName(), MethodUtils.getLocation(mc.getCurrentAnnotatedElement()));
    }

    /**
     * 热执行任务，并返回CompletableFuture包装
     *
     * @param mc       方法上下文
     * @param supplier 获取真实对象的方法
     * @return 任务执行结果的CompletableFuture
     */
    private CompletableFuture<?> supplyAsync(MethodContext mc, ResultSupplier supplier) {
        return mc.getAsyncTaskExecutor().supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
            }
        });
    }

    /**
     * 热执行任务，并返回已提交执行的FutureTask
     *
     * @param mc       方法上下文
     * @param supplier 获取真实对象的方法
     * @return 已提交执行的FutureTask
     */
    private FutureTask<?> executeFutureTask(MethodContext mc, ResultSupplier supplier) {
        FutureTask<Object> futureTask = new FutureTask<>(() -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
            }
        });
        mc.getAsyncTaskExecutor().execute(futureTask);
        return futureTask;
    }

    /**
     * 为Future子接口创建动态代理，将Future协议方法转发到CompletableFuture
     *
     * @param returnType 返回类型（Future子接口）
     * @param future     实际执行结果的CompletableFuture
     * @return 实现了returnType接口的代理对象
     */
    private Object futureProxy(Class<?> returnType, CompletableFuture<?> future) {
        return ProxyFactory.getJdkProxyObject(returnType, new FutureInvocationHandler(future));
    }

    /**
     * Future协议的动态代理处理器：将Future协议方法转发到CompletableFuture
     */
    private static class FutureInvocationHandler implements InvocationHandler {

        private final CompletableFuture<?> future;

        FutureInvocationHandler(CompletableFuture<?> future) {
            this.future = future;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "get":
                    return args == null || args.length == 0
                            ? future.get()
                            : future.get((Long) args[0], (TimeUnit) args[1]);
                case "isDone":
                    return future.isDone();
                case "isCancelled":
                    return future.isCancelled();
                case "cancel":
                    boolean mayInterruptIfRunning = args != null && args.length > 0 && (Boolean) args[0];
                    return future.cancel(mayInterruptIfRunning);
                case "run":
                    // RunnableFuture协议：任务在代理方法返回前已提交异步执行，这里保持幂等的no-op语义
                    return null;
                case "getDelay":
                    // Delayed协议：任务已提交，剩余延迟视为0
                    return 0L;
                case "compareTo":
                    return compareDelay(args);
                case "toString":
                    return "FutureProxy(" + future + ")";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    throw new UnsupportedOperationException("The method '" + method.getName() + "()' of the Future interface is not supported by the proxy");
            }
        }

        /**
         * Delayed协议的compareTo：按剩余延迟比较，本对象（已提交任务）的延迟视为0
         *
         * @param args 方法参数
         * @return 比较结果
         */
        private int compareDelay(Object[] args) {
            if (args != null && args.length == 1 && args[0] instanceof Delayed) {
                try {
                    return Long.compare(0L, ((Delayed) args[0]).getDelay(TimeUnit.NANOSECONDS));
                } catch (Throwable ignored) {
                    // ignore
                }
            }
            return 0;
        }
    }
}
