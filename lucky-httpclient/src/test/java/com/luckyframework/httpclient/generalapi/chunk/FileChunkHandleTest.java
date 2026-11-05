package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.httpclient.generalapi.chunk.support.ChunkTestSupport;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.async.JavaThreadAsyncTaskExecutor;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * 文件分片处理器的同步与异步契约测试。
 * <pre>
 * 异步相关的用例统一用单线程执行器或串行断言来消除时序不确定性，覆盖：
 * 1.同步handle：按序处理，任一分片失败立即中断且不执行onComplete；
 * 2.asyncHandle：无论成败onComplete必被执行一次，失败只通过返回的future传播；
 * 3.失败隔离：首个失败之后的分片被跳过，失败与被跳过的分片都不进入结果集合；
 * 4.异常层级：两条异步路径的future都以单层CompletionException完成，不做双层包装；
 * 5.maxConcurrency重载：线程命名file-chunk-N、守护线程、并发受限，空文件不建线程池；
 * 6.调用方传入的执行器不会被本处理器关闭；
 * 7.asyncSettleHandle（失败收集模式）：成功/异常/跳过三种形态全进结果集，future正常完成，
 *   静态列表入口只处理给定的分片子集，供失败重传使用；
 * 8.传入AsyncTaskExecutor的入口：不创建也不关闭该执行器，并发靠提交窗口限定，
 *   执行器拒绝任务也落进结果集而不外抛异常。
 * </pre>
 */
public class FileChunkHandleTest {

    /**
     * 26字节按5字节切分，共6片
     */
    private static final long CHUNK_SIZE = 5L;

    @Test
    public void syncHandleProcessesChunksInOrder() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26)), CHUNK_SIZE);
        List<Long> handled = new ArrayList<>();
        boolean[] completed = {false};

        handle.handle(fileChunk -> handled.add(fileChunk.getIndex()), () -> completed[0] = true);

        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), handled);
        assertTrue(completed[0]);
    }

    @Test
    public void syncHandleStopsAtFirstFailureAndSkipsOnComplete() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26)), CHUNK_SIZE);
        List<Long> handled = new ArrayList<>();
        boolean[] completed = {false};

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> handle.handle(fileChunk -> {
            handled.add(fileChunk.getIndex());
            if (fileChunk.getIndex() == 2) {
                throw new IllegalStateException("chunk-2-failed");
            }
        }, () -> completed[0] = true));

        assertEquals("chunk-2-failed", thrown.getMessage());
        assertEquals(Arrays.asList(1L, 2L), handled);
        // 同步模型的约定：失败即中断，收尾逻辑交给调用方的catch处理
        assertFalse(completed[0]);
    }

    @Test
    public void syncHandleWithFunctionCollectsResultsInChunkOrder() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);
        List<String> collected = new ArrayList<>();

        handle.handle(fileChunk -> "chunk-" + fileChunk.getIndex() + "(" + fileChunk.getSize() + "B)", collected::addAll);

        assertEquals(Arrays.asList("chunk-1(5B)", "chunk-2(5B)", "chunk-3(1B)"), collected);
    }

    @Test
    public void syncHandleOnEmptyFileStillRunsOnComplete() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.emptyFile(), CHUNK_SIZE);
        AtomicInteger handledCount = new AtomicInteger();
        boolean[] completed = {false};
        List<String> results = new ArrayList<>();

        handle.handle(fileChunk -> handledCount.incrementAndGet(), () -> completed[0] = true);
        handle.handle(fileChunk -> "never", results::addAll);

        assertEquals(0, handledCount.get());
        assertTrue(completed[0]);
        assertTrue(results.isEmpty());
        assertTrue(handle.getFileChunks().isEmpty());
    }

    @Test(timeout = 10000)
    public void asyncHandleRunsOnCompleteOnceWhenChunkFails() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<Long> entered = new ArrayList<>();
        boolean[] completed = {false};

        CompletableFuture<Void> future = handle.asyncHandle(executor, fileChunk -> {
            entered.add(fileChunk.getIndex());
            if (fileChunk.getIndex() == 1) {
                throw new IllegalStateException("boom-1");
            }
        }, () -> completed[0] = true);

        CompletionException thrown = assertThrows(CompletionException.class, future::join);
        assertTrue(thrown.getCause() instanceof IllegalStateException);
        assertEquals("boom-1", rootMessage(thrown));
        assertEquals("异步路径不应出现双层CompletionException包装", 1, causeDepth(thrown));
        assertTrue(completed[0]);
        // 单线程执行器下，失败后尚未开始的两个分片被直接跳过
        assertEquals(Arrays.asList(1L), entered);

        executor.shutdown();
    }

    @Test(timeout = 10000)
    public void asyncHandleReturnsResultsOfCompletedChunksOnly() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<ChunkResults<String>> received = new ArrayList<>();

        CompletableFuture<ChunkResults<String>> future = handle.asyncHandle(
                executor,
                fileChunk -> {
                    if (fileChunk.getIndex() == 2) {
                        throw new IllegalStateException("upload-refused");
                    }
                    return "chunk-" + fileChunk.getIndex();
                },
                received::add);

        CompletionException thrown = assertThrows(CompletionException.class, future::join);
        assertEquals("upload-refused", rootMessage(thrown));
        assertEquals(1, causeDepth(thrown));
        // onComplete仍能拿到已成功的那一片，供上层上报进度
        assertEquals(1, received.size());
        ChunkResults<String> results = received.get(0);
        assertEquals(1, results.size());
        assertEquals("chunk-1", results.getResults().get(0).getResult());
        assertEquals(1L, results.getResults().get(0).getFileChunk().getIndex());

        executor.shutdown();
    }

    @Test(timeout = 10000)
    public void asyncHandleKeepsChunkOrderInResults() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26)), CHUNK_SIZE);
        List<ChunkResults<String>> received = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(3);

        ChunkResults<String> results = handle.asyncHandle(
                executor,
                fileChunk -> "chunk-" + fileChunk.getIndex(),
                received::add).join();

        // 传入的执行器由调用方关闭，本处理器不会接管
        executor.shutdown();
        assertEquals(1, received.size());
        List<Long> indices = new ArrayList<>();
        results.forEach(result -> indices.add(result.getFileChunk().getIndex()));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), indices);
        assertThrows(UnsupportedOperationException.class, () -> results.getResults().clear());
        assertTrue(results.isAllSuccess(s -> true));
    }

    @Test(timeout = 10000)
    public void asyncHandleWithMaxConcurrencyLimitsParallelismAndNamesThreads() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26)), CHUNK_SIZE);
        List<String> threadNames = new ArrayList<>();
        List<Boolean> daemonFlags = new ArrayList<>();
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();

        handle.asyncHandle(fileChunk -> {
            // 结果由多个分片线程并发写入，必须加锁，否则会丢失记录
            synchronized (threadNames) {
                threadNames.add(Thread.currentThread().getName());
                daemonFlags.add(Thread.currentThread().isDaemon());
            }
            int current = running.incrementAndGet();
            maxRunning.updateAndGet(max -> Math.max(max, current));
            ChunkTestSupport.sleep(30L);
            running.decrementAndGet();
        }, () -> {
        }, 2).join();

        Set<String> distinctThreads = new LinkedHashSet<>(threadNames);
        assertEquals(6, threadNames.size());
        // 线程数被收敛到min(分片数, maxConcurrency)
        assertTrue("实际并发数" + maxRunning.get() + "不应超过2", maxRunning.get() <= 2);
        assertTrue(distinctThreads.size() <= 2);
        for (String name : distinctThreads) {
            assertTrue(name, name.startsWith("file-chunk-"));
        }
        assertTrue(daemonFlags.stream().allMatch(Boolean::booleanValue));
    }

    @Test(timeout = 10000)
    public void asyncHandleOnEmptyFileSkipsThreadPoolCreation() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.emptyFile(), CHUNK_SIZE);
        boolean[] completed = {false};
        List<ChunkResults<String>> received = new ArrayList<>();

        CompletableFuture<Void> voidFuture = handle.asyncHandle(fileChunk -> {
        }, () -> completed[0] = true, 2);
        CompletableFuture<ChunkResults<String>> resultsFuture = handle.asyncHandle(
                fileChunk -> "never", received::add, 2);

        assertTrue(voidFuture.isDone());
        assertTrue(completed[0]);
        assertEquals(1, received.size());
        assertTrue(received.get(0).isEmpty());
        assertEquals(0, resultsFuture.join().size());
    }

    @Test
    public void asyncHandleRejectsIllegalArguments() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);

        assertThrows(IllegalArgumentException.class, () -> handle.asyncHandle(fileChunk -> {
        }, () -> {
        }, 0));
        assertThrows(IllegalArgumentException.class, () -> handle.asyncHandle(fileChunk -> {
        }, () -> {
        }, -1));
        assertThrows(IllegalArgumentException.class, () -> handle.asyncHandle(
                (Function<FileChunk, String>) fileChunk -> null, (Consumer<ChunkResults<String>>) null, 2));
        assertThrows(IllegalArgumentException.class, () -> handle.asyncHandle((Executor) null, fileChunk -> {
        }, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> handle.asyncHandle(
                Executors.newSingleThreadExecutor(), (Consumer<FileChunk>) null, () -> {
                }));
    }

    @Test
    public void syncHandleRejectsNullCallbacks() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);

        assertThrows(IllegalArgumentException.class, () -> handle.handle((Consumer<FileChunk>) null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> handle.handle((Consumer<FileChunk>) fileChunk -> {
        }, null));
    }

    @Test(timeout = 10000)
    public void callerSuppliedExecutorIsNotShutdown() throws Exception {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        handle.asyncHandle(executor, fileChunk -> {
        }, () -> {
        }).join();

        // 本处理器不接管调用方执行器的生命周期
        assertFalse(executor.isShutdown());
        assertEquals("still-usable", executor.submit(() -> "still-usable").get());
        executor.shutdown();
    }

    @Test
    public void defaultChunkSizeAndAccessors() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(22));

        FileChunkHandle handle = FileChunkHandle.of(file);

        assertEquals(ChunkConstants.DEFAULT_CHUNK_SIZE, handle.getChunkSize());
        assertEquals(file, handle.getFile());
        assertEquals(1, handle.getFileChunks().size());
        assertEquals(22L, handle.getFileChunks().get(0).getSize());
    }

    @Test
    public void handleRejectsMissingFile() {
        File missing = new File(ChunkTestSupport.tempDirectory(), "absent.bin");
        FileChunkHandle handle = FileChunkHandle.of(missing, CHUNK_SIZE);

        assertThrows(IllegalArgumentException.class, () -> handle.handle(fileChunk -> {
        }, () -> {
        }));
    }

    /**
     * 失败收集模式：分片异常不再使future异常完成，异常与止损跳过的分片都以独立形态进入结果集
     */
    @Test(timeout = 10000)
    public void asyncSettleHandleCollectsFailedAndSkippedChunks() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<Long> entered = new ArrayList<>();

        ChunkResults<String> results = handle.asyncSettleHandle(executor, fileChunk -> {
            entered.add(fileChunk.getIndex());
            if (fileChunk.getIndex() == 1) {
                throw new IllegalStateException("settle-boom");
            }
            return "chunk-" + fileChunk.getIndex();
        }).join();

        // 单线程执行器下，首片异常后剩余分片被止损跳过，三种形态合计仍是全部分片
        assertEquals(Arrays.asList(1L), entered);
        assertEquals(3, results.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), indicesOf(results));
        assertTrue(results.hasExecutionFailure());
        assertEquals(0, results.getResults(ChunkResultStatus.SUCCESS).size());
        assertEquals(1, results.getResults(ChunkResultStatus.FAILED).size());
        assertEquals(2, results.getResults(ChunkResultStatus.SKIPPED).size());

        ChunkResult<String> failed = results.getResults().get(0);
        assertEquals(ChunkResultStatus.FAILED, failed.getStatus());
        assertFalse(failed.isSuccess());
        assertEquals("settle-boom", failed.getError().getMessage());
        assertNull(failed.getResult());
        ChunkResult<String> skipped = results.getResults().get(1);
        assertEquals(ChunkResultStatus.SKIPPED, skipped.getStatus());
        assertNull(skipped.getError());

        executor.shutdown();
    }

    /**
     * 成功分片照常进入结果集，未完成的分片可通过{@code getUncompletedChunks()}直接取到待重传集合
     */
    @Test(timeout = 10000)
    public void asyncSettleHandleExposesUncompletedChunksForRetry() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        ChunkResults<String> results = handle.asyncSettleHandle(executor, fileChunk -> {
            if (fileChunk.getIndex() == 2) {
                throw new IllegalStateException("upload-refused");
            }
            return "ok";
        }).join();

        assertEquals(3, results.size());
        assertEquals(1, results.getResults(ChunkResultStatus.SUCCESS).size());
        assertEquals("ok", results.getResults().get(0).getResult());
        assertEquals(Arrays.asList(2L, 3L), indicesOf(results.getUncompletedChunks()));
        // 业务判据只作用于执行完毕的分片，失败集合包含异常与被跳过的分片
        ChunkResultClassify<String> classify = results.classify("ok"::equals);
        assertEquals(1, classify.successSize());
        assertEquals(2, classify.failureSize());
        assertFalse(classify.isAllSuccess());

        executor.shutdown();
    }

    /**
     * 静态列表入口只处理传入的分片子集，重传时无需重新切分整个文件
     */
    @Test(timeout = 10000)
    public void staticAsyncSettleHandleProcessesGivenChunkSubset() {
        List<FileChunk> allChunks = ChunkUtils.shard(
                ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);
        List<FileChunk> subset = new ArrayList<>(allChunks.subList(1, allChunks.size()));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<Long> entered = new ArrayList<>();

        ChunkResults<String> results = FileChunkHandle
                .asyncSettleHandle(subset, executor, fileChunk -> {
                    entered.add(fileChunk.getIndex());
                    return "chunk-" + fileChunk.getIndex();
                })
                .join();

        assertEquals(Arrays.asList(2L, 3L), entered);
        assertEquals(Arrays.asList("chunk-2", "chunk-3"), valuesOf(results));
        assertFalse(results.hasExecutionFailure());
        assertFalse(executor.isShutdown());

        executor.shutdown();
    }

    @Test
    public void staticAsyncSettleHandleOnEmptyListSkipsScheduling() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        CompletableFuture<ChunkResults<String>> future =
                FileChunkHandle.asyncSettleHandle(Collections.emptyList(), executor, fileChunk -> "never");

        assertTrue("空分片集合应直接返回已完成的future", future.isDone());
        assertTrue(future.join().isEmpty());
        assertFalse(executor.isShutdown());
        executor.shutdown();
    }

    /**
     * 限并发的失败收集模式与{@code asyncHandle}共用同一套线程池约定
     */
    @Test(timeout = 10000)
    public void asyncSettleHandleWithMaxConcurrencyLimitsParallelismAndNamesThreads() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26)), CHUNK_SIZE);
        Set<String> threadNames = new LinkedHashSet<>();
        List<Boolean> daemonFlags = new ArrayList<>();
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();

        ChunkResults<String> results = handle.asyncSettleHandle(fileChunk -> {
            synchronized (threadNames) {
                threadNames.add(Thread.currentThread().getName());
                daemonFlags.add(Thread.currentThread().isDaemon());
            }
            int current = running.incrementAndGet();
            maxRunning.updateAndGet(max -> Math.max(max, current));
            ChunkTestSupport.sleep(20L);
            running.decrementAndGet();
            return "chunk-" + fileChunk.getIndex();
        }, 2).join();

        assertEquals(6, results.size());
        assertTrue("实际并发数" + maxRunning.get() + "不应超过2", maxRunning.get() <= 2);
        assertTrue(threadNames.toString(), threadNames.size() <= 2);
        for (String name : threadNames) {
            assertTrue(name, name.startsWith("file-chunk-"));
        }
        assertTrue(daemonFlags.stream().allMatch(Boolean::booleanValue));
    }

    @Test(timeout = 10000)
    public void asyncSettleHandleOnEmptyFileReturnsCompletedFuture() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.emptyFile(), CHUNK_SIZE);

        CompletableFuture<ChunkResults<String>> future = handle.asyncSettleHandle(fileChunk -> "never", 2);

        assertTrue(future.isDone());
        assertTrue(future.join().isEmpty());
    }

    /**
     * 使用框架执行器时，并发数由提交窗口限定：即使线程池有6个线程，在途任务也不会超过maxConcurrency；
     * 同时本方法不得关闭调用方传入的执行器
     */
    @Test(timeout = 10000)
    public void asyncSettleHandleWithAsyncTaskExecutorLimitsInFlightTasksByWindow() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26)), CHUNK_SIZE);
        ExecutorService threadPool = Executors.newFixedThreadPool(6);
        AsyncTaskExecutor executor = JavaThreadAsyncTaskExecutor.createByExecutor(threadPool);
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();

        ChunkResults<String> results = handle.asyncSettleHandle(executor, fileChunk -> {
            // 结果由多个分片线程并发写入，必须加锁，否则会丢失记录
            synchronized (running) {
                int current = running.incrementAndGet();
                maxRunning.updateAndGet(max -> Math.max(max, current));
            }
            ChunkTestSupport.sleep(20L);
            running.decrementAndGet();
            return "chunk-" + fileChunk.getIndex();
        }, 2).join();

        assertEquals(6, results.size());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), indicesOf(results));
        assertEquals(Arrays.asList("chunk-1", "chunk-2", "chunk-3", "chunk-4", "chunk-5", "chunk-6"), valuesOf(results));
        assertFalse(results.hasExecutionFailure());
        assertTrue("实际并发数" + maxRunning.get() + "不应超过2", maxRunning.get() <= 2);
        // 共享执行器的生命周期归创建方，本处理器不得接管
        assertFalse(threadPool.isShutdown());
        threadPool.shutdown();
    }

    /**
     * 异步任务执行器版本的失败收集语义与{@code Executor}版本一致：首片异常后剩余分片被止损跳过，future仍正常完成
     */
    @Test(timeout = 10000)
    public void asyncSettleHandleWithAsyncTaskExecutorCollectsFailedAndSkippedChunks() {
        List<FileChunk> fileChunks = ChunkUtils.shard(
                ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        AsyncTaskExecutor executor = JavaThreadAsyncTaskExecutor.createByExecutor(threadPool);
        List<Long> entered = new ArrayList<>();

        ChunkResults<String> results = FileChunkHandle.asyncSettleHandle(fileChunks, executor, fileChunk -> {
            entered.add(fileChunk.getIndex());
            if (fileChunk.getIndex() == 1) {
                throw new IllegalStateException("executor-settle-boom");
            }
            return "chunk-" + fileChunk.getIndex();
        }, 1).join();

        assertEquals(Arrays.asList(1L), entered);
        assertEquals(3, results.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), indicesOf(results));
        assertTrue(results.hasExecutionFailure());
        assertEquals(ChunkResultStatus.FAILED, results.getResults().get(0).getStatus());
        assertEquals("executor-settle-boom", results.getResults().get(0).getError().getMessage());
        assertEquals(2, results.getResults(ChunkResultStatus.SKIPPED).size());
        assertFalse(threadPool.isShutdown());
        threadPool.shutdown();
    }

    /**
     * 执行器拒绝提交时不得抛出异常，而是把该分片记为FAILED并停止后续提交，保证调用方拿到齐全的结果集
     */
    @Test(timeout = 10000)
    public void asyncSettleHandleWithAsyncTaskExecutorMarksRejectedSubmissionAsFailed() {
        List<FileChunk> fileChunks = ChunkUtils.shard(
                ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);

        ChunkResults<String> results = FileChunkHandle
                .asyncSettleHandle(fileChunks, rejectingExecutor(), fileChunk -> "never", 2)
                .join();

        assertEquals(3, results.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), indicesOf(results));
        assertTrue(results.hasExecutionFailure());
        ChunkResult<String> rejected = results.getResults().get(0);
        assertEquals(ChunkResultStatus.FAILED, rejected.getStatus());
        assertTrue(rejected.getError() instanceof RejectedExecutionException);
        assertEquals(2, results.getResults(ChunkResultStatus.SKIPPED).size());
    }

    /**
     * 空分片集合不会触碰执行器，也不创建任何线程池
     */
    @Test
    public void asyncSettleHandleWithAsyncTaskExecutorOnEmptyListSkipsScheduling() {
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        AsyncTaskExecutor executor = JavaThreadAsyncTaskExecutor.createByExecutor(threadPool);

        CompletableFuture<ChunkResults<String>> future =
                FileChunkHandle.asyncSettleHandle(Collections.<FileChunk>emptyList(), executor, fileChunk -> "never", 2);

        assertTrue("空分片集合应直接返回已完成的future", future.isDone());
        assertTrue(future.join().isEmpty());
        assertFalse(threadPool.isShutdown());
        threadPool.shutdown();
    }

    @Test
    public void asyncSettleHandleRejectsIllegalArguments() {
        FileChunkHandle handle = FileChunkHandle.of(ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11)), CHUNK_SIZE);
        AsyncTaskExecutor taskExecutor = JavaThreadAsyncTaskExecutor.createByExecutor(Executors.newSingleThreadExecutor());

        assertThrows(IllegalArgumentException.class, () -> handle.asyncSettleHandle((Function<FileChunk, String>) null, 2));
        assertThrows(IllegalArgumentException.class, () -> handle.asyncSettleHandle(fileChunk -> "ok", 0));
        assertThrows(IllegalArgumentException.class, () -> handle.asyncSettleHandle(fileChunk -> "ok", -1));
        assertThrows(IllegalArgumentException.class, () -> handle.asyncSettleHandle((Executor) null, fileChunk -> "ok"));
        assertThrows(IllegalArgumentException.class, () -> FileChunkHandle
                .asyncSettleHandle((List<FileChunk>) null, Executors.newSingleThreadExecutor(), fileChunk -> "ok"));
        assertThrows(IllegalArgumentException.class, () -> FileChunkHandle
                .asyncSettleHandle(handle.getFileChunks(), (Executor) null, fileChunk -> "ok"));
        // AsyncTaskExecutor入口的入参校验
        assertThrows(IllegalArgumentException.class, () -> FileChunkHandle
                .asyncSettleHandle(handle.getFileChunks(), (AsyncTaskExecutor) null, fileChunk -> "ok", 2));
        assertThrows(IllegalArgumentException.class, () -> FileChunkHandle
                .asyncSettleHandle(handle.getFileChunks(), taskExecutor, fileChunk -> "ok", 0));
        assertThrows(IllegalArgumentException.class, () -> handle.asyncSettleHandle(taskExecutor, (Function<FileChunk, String>) null, 2));
    }

    /**
     * 按提交顺序取出结果集里的分片索引
     */
    private static List<Long> indicesOf(ChunkResults<String> results) {
        List<Long> indices = new ArrayList<>();
        results.forEach(result -> indices.add(result.getFileChunk().getIndex()));
        return indices;
    }

    /**
     * 任何任务都直接拒绝执行的异步任务执行器，用于验证“执行器拒绝任务也落入结果集”
     */
    private static AsyncTaskExecutor rejectingExecutor() {
        return new AsyncTaskExecutor() {
            @Override
            public void execute(Runnable command) {
                throw new RejectedExecutionException("executor-rejected");
            }

            @Override
            public <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
                throw new RejectedExecutionException("executor-rejected");
            }

            @Override
            public Executor getExecutor() {
                return Runnable::run;
            }

            @Override
            public void shutdown() {
            }

            @Override
            public void shutdownNow() {
            }
        };
    }

    private static List<Long> indicesOf(List<FileChunk> fileChunks) {
        List<Long> indices = new ArrayList<>();
        for (FileChunk fileChunk : fileChunks) {
            indices.add(fileChunk.getIndex());
        }
        return indices;
    }

    private static List<String> valuesOf(ChunkResults<String> results) {
        List<String> values = new ArrayList<>();
        results.forEach(result -> values.add(result.getResult()));
        return values;
    }

    /**
     * 取异常链最深层的原因描述
     */
    private static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertNotNull(cause.getMessage());
        return cause.getMessage();
    }

    /**
     * 统计异常链上cause的层数，用于锁定「不重复包装CompletionException」这条约定
     */
    private static int causeDepth(Throwable throwable) {
        int depth = 0;
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            depth++;
            cause = cause.getCause();
        }
        return depth;
    }
}
