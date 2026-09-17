package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.typeparser.support.ReactiveTestApi;
import com.luckyframework.httpclient.proxy.typeparser.support.RecordingAsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.httpclient.proxy.typeparser.support.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.MONO_EMPTY_PATH;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.MONO_HELLO_BODY;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.MONO_HELLO_PATH;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.MONO_SLOW_BODY;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.MONO_SLOW_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link MonoMethodPackTypeParser}的集成测试
 * <pre>
 *     验证点：
 *       A.热执行语义（P1）：调用代理方法立即提交任务并只执行一次，与订阅解耦
 *       B.多订阅者共享同一执行结果
 *       C.空结果桥接为Mono.empty
 *       D.异常解包（P2）：CompletionException解包后向下游传递AsyncTaskExecutorException
 *       E.取消语义（P3）：未完成时取消会取消任务，已完成时取消不影响任务
 * </pre>
 * 测试手段：
 *   1.使用本地HttpServer统计各路径请求命中数，验证"是否发出请求"以及"请求次数"
 *   2.通过方法参数注入{@link RecordingAsyncTaskExecutor}，记录任务Future并可手动驱动执行
 */
public class MonoMethodPackTypeParserTest {

    private TestHttpServer server;
    private RecordingAsyncTaskExecutor executor;
    private ReactiveTestApi api;

    @Before
    public void setUp() throws Exception {
        server = new TestHttpServer();
        executor = new RecordingAsyncTaskExecutor();
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.addPackTypeParser(new MonoMethodPackTypeParser());
        api = factory.getProxyObject(ReactiveTestApi.class);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        server.close();
    }

    /**
     * 热执行：代理方法被调用时（未订阅）就应该已发出HTTP请求，且只执行一次
     */
    @Test
    public void hotExecutionShouldSendRequestBeforeSubscribeAndExecuteOnlyOnce() {
        Mono<String> mono = api.monoHello(TestUtils.request(server, MONO_HELLO_PATH), executor);

        // 未订阅时请求已发出
        TestUtils.awaitTrue("热执行：未订阅时请求就应该已发出", 5000, () -> server.hit(MONO_HELLO_PATH) == 1);

        // 订阅后能拿到结果，且不会再次发起请求
        assertEquals(MONO_HELLO_BODY, mono.block());
        assertEquals("任务只应执行一次", 1, server.hit(MONO_HELLO_PATH));
        assertEquals("只应提交一个异步任务", 1, executor.submittedCount());
    }

    /**
     * 多个订阅者共享同一执行结果，HTTP请求只发生一次
     */
    @Test
    public void multipleSubscribersShouldShareSingleExecution() {
        Mono<String> mono = api.monoHello(TestUtils.request(server, MONO_HELLO_PATH), executor);

        assertEquals(MONO_HELLO_BODY, mono.block());
        assertEquals(MONO_HELLO_BODY, mono.block());
        assertEquals("任务只应执行一次", 1, server.hit(MONO_HELLO_PATH));
        assertEquals(1, executor.submittedCount());
    }

    /**
     * 空响应桥接：结果为null时发射空序列（Mono.empty），而不是携带null值的序列
     */
    @Test
    public void nullResultShouldBeBridgedToEmptyMono() {
        Mono<Void> mono = api.monoEmpty(TestUtils.request(server, MONO_EMPTY_PATH), executor);

        AtomicBoolean onNextCalled = new AtomicBoolean(false);
        mono.subscribe(v -> onNextCalled.set(true));

        assertNull(mono.block());
        assertFalse("空响应应发射空序列，onNext不应被调用", onNextCalled.get());
        // defaultIfEmpty可以生效说明确实是empty而不是null值
        assertEquals("fallback", mono.map(v -> "mapped").defaultIfEmpty("fallback").block());
        assertEquals(1, server.hit(MONO_EMPTY_PATH));
    }

    /**
     * 异常解包（P2）：任务异常被CompletionException包装后，订阅者应收到解包的AsyncTaskExecutorException
     */
    @Test
    public void errorShouldBeUnwrappedFromCompletionException() {
        Mono<String> mono = api.monoHello(TestUtils.badRequest(), executor);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        mono.subscribe(v -> {
        }, errorRef::set);
        TestUtils.awaitTrue("错误应传播给订阅者", 5000, () -> errorRef.get() != null);

        Throwable error = errorRef.get();
        assertTrue("异常应被包装为AsyncTaskExecutorException，实际为: " + error, error instanceof AsyncTaskExecutorException);
        assertFalse("异常不应是CompletionException（需要解包）", error instanceof CompletionException);
        assertTrue("根因链中应包含ConnectException", TestUtils.hasCause(error, ConnectException.class));

        // block()路径同样能观察到解包后的原始异常
        try {
            mono.block();
            fail("block() 应抛出异常");
        } catch (AsyncTaskExecutorException e) {
            assertTrue(TestUtils.hasCause(e, ConnectException.class));
        }
    }

    /**
     * 取消未完成的任务：订阅者取消订阅时，尚未完成的任务会被取消，且该过程静默（不向下游传播错误）
     */
    @Test
    public void cancelBeforeCompletionShouldCancelPendingTask() {
        executor.setManualMode(true);
        try {
            Mono<String> mono = api.monoSlow(TestUtils.request(server, MONO_SLOW_PATH), executor);
            CompletableFuture<?> future = executor.lastSupplyFuture();
            assertNotNull(future);
            assertFalse("手动模式下任务尚未执行", future.isDone());

            AtomicBoolean terminated = new AtomicBoolean(false);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            Disposable disposable = mono.subscribe(v -> terminated.set(true), errorRef::set, () -> terminated.set(true));

            disposable.dispose();

            assertTrue("取消订阅后应尝试取消未完成的任务", future.isCancelled());
            assertNull("取消订阅属于正常流程，不应向下游传播错误", errorRef.get());
            assertFalse("取消后不应收到任何数据", terminated.get());
            assertEquals("任务尚未执行，取消后也不应发出请求", 0, server.hit(MONO_SLOW_PATH));

            executor.discardPending();
        } finally {
            executor.setManualMode(false);
        }
    }

    /**
     * 取消已完成的任务：任务已完成时取消订阅不应影响（也不应取消）已结束的任务，后续订阅仍可获取结果
     */
    @Test
    public void cancelAfterCompletionShouldNotCancelFinishedTask() {
        Mono<String> mono = api.monoSlow(TestUtils.request(server, MONO_SLOW_PATH), executor);

        // 等待任务真正完成
        assertEquals(MONO_SLOW_BODY, mono.block());
        CompletableFuture<?> future = executor.lastSupplyFuture();
        assertNotNull(future);
        assertTrue(future.isDone());

        mono.subscribe().dispose();

        assertFalse("已完成的任务不应被取消", future.isCancelled());
        assertEquals("取消已完成任务的订阅不应影响后续订阅", MONO_SLOW_BODY, mono.block());
    }
}
