package com.luckyframework.httpclient.proxy.typeparser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.typeparser.support.ReactiveTestApi;
import com.luckyframework.httpclient.proxy.typeparser.support.RecordingAsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.httpclient.proxy.typeparser.support.TestUtils;
import com.luckyframework.httpclient.proxy.typeparser.support.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.FLUX_BAD_JSON_PATH;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.FLUX_LIST_PATH;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.FLUX_SLOW_PATH;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.SSE_BASIC_PATH;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.SSE_BROKEN_PATH;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.SSE_LINES;
import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.SSE_STALL_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link FluxMethodPackTypeParser}的集成测试
 * <pre>
 *     验证点：
 *       A.普通List聚合模式为热执行（P1）：调用代理方法立即提交任务且只执行一次，与订阅解耦
 *       B.多订阅者共享同一执行结果；支持按下游请求节奏发射元素（背压）
 *       C.异常解包（P2）：任务异常解包CompletionException后传递给订阅者
 *       D.取消语义（P3）：任一订阅者取消未完成任务会尝试取消共享任务；
 *         已完成的任务不会被取消；取消不会向取消者传播错误
 *       E.SSE（@TextEventStream）保持冷流语义：订阅时才建立连接，每次订阅都新建连接；
 *         取消订阅能及时中断阻塞中的读取（P4：主动关闭输入流）
 * </pre>
 */
public class FluxMethodPackTypeParserTest {

    private TestHttpServer server;
    private RecordingAsyncTaskExecutor executor;
    private ReactiveTestApi api;

    @Before
    public void setUp() throws Exception {
        server = new TestHttpServer();
        executor = new RecordingAsyncTaskExecutor();
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.addPackTypeParser(new FluxMethodPackTypeParser());
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
        Flux<User> flux = api.fluxList(TestUtils.request(server, FLUX_LIST_PATH), executor);

        // 未订阅时请求已发出
        TestUtils.awaitTrue("热执行：未订阅时请求就应该已发出", 5000, () -> server.hit(FLUX_LIST_PATH) == 1);

        assertEquals(expectedUsers(), flux.collectList().block());
        assertEquals("任务只应执行一次", 1, server.hit(FLUX_LIST_PATH));
        assertEquals("只应提交一个异步任务", 1, executor.submittedCount());
    }

    /**
     * 多个订阅者共享同一执行结果，HTTP请求只发生一次
     */
    @Test
    public void multipleSubscribersShouldShareSingleExecution() {
        Flux<User> flux = api.fluxList(TestUtils.request(server, FLUX_LIST_PATH), executor);

        assertEquals(expectedUsers(), flux.collectList().block());
        assertEquals(expectedUsers(), flux.collectList().block());
        assertEquals("任务只应执行一次", 1, server.hit(FLUX_LIST_PATH));
        assertEquals(1, executor.submittedCount());
    }

    /**
     * 异常解包（P2）：任务异常被CompletionException包装后，订阅者应收到解包的AsyncTaskExecutorException
     */
    @Test
    public void errorShouldBeUnwrappedFromCompletionException() {
        Flux<User> flux = api.fluxList(TestUtils.badRequest(), executor);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        flux.subscribe(u -> {
        }, errorRef::set);
        TestUtils.awaitTrue("错误应传播给订阅者", 5000, () -> errorRef.get() != null);

        Throwable error = errorRef.get();
        assertTrue("异常应被包装为AsyncTaskExecutorException，实际为: " + error, error instanceof AsyncTaskExecutorException);
        assertFalse("异常不应是CompletionException（需要解包）", error instanceof CompletionException);
        assertTrue("根因链中应包含ConnectException", TestUtils.hasCause(error, ConnectException.class));

        // block()路径同样能观察到解包后的异常
        try {
            flux.collectList().block();
            fail("block() 应抛出异常");
        } catch (AsyncTaskExecutorException e) {
            assertTrue(TestUtils.hasCause(e, ConnectException.class));
        }
    }

    /**
     * 响应转换失败（非法JSON）应传播给订阅者，且根因链中包含Jackson解析异常
     */
    @Test
    public void conversionErrorShouldPropagateToSubscriber() {
        Flux<User> flux = api.fluxBadJson(TestUtils.request(server, FLUX_BAD_JSON_PATH), executor);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        flux.subscribe(u -> {
        }, errorRef::set);
        TestUtils.awaitTrue("转换错误应传播给订阅者", 5000, () -> errorRef.get() != null);

        Throwable error = errorRef.get();
        assertTrue("异常应被包装为AsyncTaskExecutorException，实际为: " + error, error instanceof AsyncTaskExecutorException);
        assertTrue("根因链中应包含Jackson解析异常", TestUtils.hasCause(error, JsonProcessingException.class));
    }

    /**
     * 背压：元素发射应遵循下游的请求节奏，未请求时不发射
     */
    @Test
    public void backpressureShouldEmitOnDemand() {
        Flux<User> flux = api.fluxList(TestUtils.request(server, FLUX_LIST_PATH), executor);

        List<User> received = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

        flux.subscribe(new BaseSubscriber<User>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                // 不自动请求，由测试手动控制节奏
                subscriptionRef.set(subscription);
            }

            @Override
            protected void hookOnNext(User value) {
                received.add(value);
            }

            @Override
            protected void hookOnComplete() {
                completed.set(true);
            }
        });

        // 等待任务完成（请求已发出），下游未请求时不发射任何元素
        TestUtils.awaitTrue("任务应已执行", 5000, () -> server.hit(FLUX_LIST_PATH) == 1);
        assertTrue("未请求时不应发射数据", received.isEmpty());
        assertNotNull(subscriptionRef.get());

        // 逐个请求，逐个发射
        subscriptionRef.get().request(1);
        TestUtils.awaitTrue("应发射第1个元素", 2000, () -> received.size() == 1);
        assertFalse(completed.get());

        subscriptionRef.get().request(1);
        TestUtils.awaitTrue("应发射第2个元素", 2000, () -> received.size() == 2);
        assertFalse(completed.get());

        subscriptionRef.get().request(1);
        TestUtils.awaitTrue("应发射第3个元素并完成", 3000, () -> received.size() == 3 && completed.get());
        assertEquals(expectedUsers(), received);
    }

    /**
     * 早取消（P3）：订阅者在任务完成前取消订阅，会尝试取消共享任务；
     * 由于任务执行只发生一次，后续订阅者会收到CancellationException
     */
    @Test
    public void earlyCancelShouldCancelSharedTaskAndLateSubscriberReceivesCancellation() {
        executor.setManualMode(true);
        try {
            Flux<User> flux = api.fluxSlow(TestUtils.request(server, FLUX_SLOW_PATH), executor);
            CompletableFuture<?> future = executor.lastSupplyFuture();
            assertNotNull(future);
            assertFalse("手动模式下任务尚未执行", future.isDone());

            // 订阅者A订阅后立即取消
            Disposable a = flux.subscribe(u -> {
            }, e -> {
            });
            a.dispose();
            assertTrue("取消订阅应尝试取消未完成的共享任务", future.isCancelled());

            // 订阅者B随后订阅，收到CancellationException
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            flux.subscribe(u -> {
            }, errorRef::set);
            TestUtils.awaitTrue("后订阅者应收到取消信号", 3000, () -> errorRef.get() != null);
            assertTrue("应为CancellationException，实际为: " + errorRef.get(), errorRef.get() instanceof CancellationException);

            // 任务从未真正执行
            assertEquals("被取消的任务不应发出HTTP请求", 0, server.hit(FLUX_SLOW_PATH));

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
        Flux<User> flux = api.fluxSlow(TestUtils.request(server, FLUX_SLOW_PATH), executor);

        // 等待任务真正完成
        assertEquals(expectedUsers(), flux.collectList().block());
        CompletableFuture<?> future = executor.lastSupplyFuture();
        assertNotNull(future);
        assertTrue(future.isDone());

        flux.subscribe().dispose();

        assertFalse("已完成的任务不应被取消", future.isCancelled());
        assertEquals("取消已完成任务的订阅不应影响后续订阅", expectedUsers(), flux.collectList().block());
    }

    /**
     * SSE冷流（P1）：未订阅时不提交任何任务、不发出任何请求；订阅后才建立连接并逐行接收数据
     */
    @Test
    public void sseShouldNotSubmitTaskBeforeSubscribe() {
        Flux<String> sse = api.sseBasic(TestUtils.request(server, SSE_BASIC_PATH), executor);

        TestUtils.sleepQuietly(300);
        assertEquals("未订阅时不应提交任务", 0, executor.submittedCount());
        assertEquals("未订阅时不应发出请求", 0, server.hit(SSE_BASIC_PATH));

        // 订阅后才会建立连接并逐行读取
        assertEquals(SSE_LINES, sse.collectList().block());
        assertEquals(1, server.hit(SSE_BASIC_PATH));
        assertEquals(1, executor.submittedCount());
    }

    /**
     * SSE冷流：每次调用/订阅都会执行完整的请求流程并建立新连接（不共享结果）
     * 说明：同一代理调用产生的MethodContext在wrap返回后会被销毁，且其SpEL变量空间在首次
     * 订阅执行完毕后不允许二次写入（受保护变量），因此通过两次独立调用+订阅来验证
     * "每次订阅建立新连接"这一冷流特征。
     */
    @Test
    public void sseMultipleInvocationsShouldConnectMultipleTimes() {
        Flux<String> sse1 = api.sseBasic(TestUtils.request(server, SSE_BASIC_PATH), executor);
        assertEquals(SSE_LINES, sse1.collectList().block());
        assertEquals(1, server.hit(SSE_BASIC_PATH));

        Flux<String> sse2 = api.sseBasic(TestUtils.request(server, SSE_BASIC_PATH), executor);
        assertEquals(SSE_LINES, sse2.collectList().block());
        assertEquals("每次订阅都应建立新连接", 2, server.hit(SSE_BASIC_PATH));
        assertEquals(2, executor.submittedCount());
    }

    /**
     * SSE取消（P4）：取消订阅时会主动关闭输入流，使阻塞中的读取任务最终退出，并保持静默
     * 实测说明：JDK HttpURLConnection 对chunked流的close()会先skip至EOF，
     * 因此取消操作的释放时机受读超时（sseStall的readTimeout=3s）约束；
     * 本用例验证：读取任务在读超时窗口内退出（不悬挂）、取消后不再发射数据、且不向下游传播错误
     */
    @Test
    public void sseCancelShouldTerminateBlockedReadQuietly() throws InterruptedException {
        Flux<String> sse = api.sseStall(TestUtils.request(server, SSE_STALL_PATH), executor);

        List<String> received = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        Disposable disposable = sse.subscribe(received::add, errorRef::set);

        // 等待第一行数据（此后读取任务将阻塞在readLine上）
        TestUtils.awaitTrue("应收到stall流的第一行数据", 5000, () -> !received.isEmpty());

        Thread disposer = new Thread(disposable::dispose, "sse-disposer");
        disposer.start();

        assertTrue("取消后读取任务应在读超时窗口内退出，而不是悬挂", executor.awaitIdle(8000));
        assertNull("取消导致的中断不应向下游传播错误", errorRef.get());
        assertEquals("取消后不应再发射数据", 1, received.size());

        // 等待dispose返回，避免残留线程影响后续用例
        disposer.join(8000);
    }

    /**
     * SSE取消：订阅后立即取消时，读取任务应跳过HTTP请求的执行
     */
    @Test
    public void sseCancelImmediatelyAfterSubscribeShouldSkipRequest() {
        executor.setManualMode(true);
        try {
            Flux<String> sse = api.sseBasic(TestUtils.request(server, SSE_BASIC_PATH), executor);

            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            Disposable disposable = sse.subscribe(v -> {
            }, errorRef::set);

            // 任务已提交（手动模式下尚未执行），请求未发出
            assertEquals("订阅应提交读取任务", 1, executor.submittedCount());
            assertEquals(0, server.hit(SSE_BASIC_PATH));

            // 立即取消后再执行任务：应跳过HTTP请求
            disposable.dispose();
            executor.drain();

            assertEquals("订阅后立即取消，不应发出HTTP请求", 0, server.hit(SSE_BASIC_PATH));
            assertNull(errorRef.get());
        } finally {
            executor.setManualMode(false);
            executor.discardPending();
        }
    }

    /**
     * SSE请求失败（如连接被拒绝）时应向下游传播错误
     */
    @Test
    public void sseSupplierErrorShouldPropagateToSubscriber() {
        Flux<String> sse = api.sseBasic(TestUtils.badRequest(), executor);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        sse.subscribe(v -> {
        }, errorRef::set);
        TestUtils.awaitTrue("请求错误应传播给订阅者", 5000, () -> errorRef.get() != null);

        assertTrue("根因链中应包含ConnectException", TestUtils.hasCause(errorRef.get(), ConnectException.class));
    }

    /**
     * SSE响应被服务端提前截断时，读取流应正常终结（不悬挂）
     * 实测行为：JDK HttpURLConnection 将"声明长度未写满即断开"的提前EOF视为流结束，
     * 因此该场景下流以完成告终，而不是抛出读取错误
     */
    @Test
    public void sseBrokenResponseShouldTerminateStreamInsteadOfHanging() {
        Flux<String> sse = api.sseBroken(TestUtils.request(server, SSE_BROKEN_PATH), executor);

        List<String> received = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        sse.subscribe(received::add, errorRef::set, () -> completed.set(true));

        TestUtils.awaitTrue("截断的流应终结（完成或错误）", 8000, () -> errorRef.get() != null || completed.get());
        assertTrue("提前EOF应被视为流结束，流正常完成", completed.get());
        assertNull(errorRef.get());
    }

    private static List<User> expectedUsers() {
        return Arrays.asList(new User(1, "alice"), new User(2, "bob"), new User(3, "carol"));
    }
}
