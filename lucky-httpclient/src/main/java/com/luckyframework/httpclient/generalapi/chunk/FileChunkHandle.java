package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 文件分片处理器。
 * <pre>
 *     本类是「本地大文件切片 + 逐片处理」的骨架，典型用途是声明式分片上传：
 *     把文件按固定字节数切成若干 {@link FileChunk}，再依次（或并发）把每一片交给回调处理。
 *
 *     提供两种执行模型：
 *     1.同步模型 {@code handle}：按分片顺序在当前线程逐个处理；
 *     2.异步模型 {@code asyncHandle}/{@code asyncSettleHandle}：把每个分片提交为独立任务并发执行，
 *       返回 {@link CompletableFuture} 供调用方自行 join、编排超时或注册异常处理。
 *
 *     分片语义（与 {@link ChunkUtils} 保持一致）：
 *     1.分片区间为[start,end)，即end不属于本分片；
 *     2.分片索引index从1开始；
 *     3.总大小恰为分片大小整数倍时不会产生末尾空分片，空文件（长度为0）没有任何分片。
 *
 *     异步模型的失败语义（四个asyncHandle入口一致）：
 *     1.所有分片任务落定（无论成功、失败还是被跳过）之后，onComplete一定会被执行一次；
 *     2.任一分片处理抛出异常时，会置中断标志，尚未开始执行的分片直接跳过，
 *       已经开始执行的分片仍会跑完（无法中断已在运行的任务），以此减少对服务端的无效请求；
 *     3.存在失败分片时，返回的future以{@link CompletionException}完成，cause为首个失败异常，
 *       因此<b>调用方必须消费返回的future</b>（join/get/exceptionally/handle任选其一），
 *       否则失败会被静默丢弃；不需要异步结果时可直接调用同步handle。
 *
 *     两种异步收集模式（区别在于失败分片去哪儿）：
 *     1.{@code asyncHandle}只把执行成功的分片放进{@link ChunkResults}，失败原因仅通过future传播；
 *     2.{@code asyncSettleHandle}把成功、异常失败、被跳过三种分片全部放进{@link ChunkResults}，
 *       分片逻辑的异常不再使future异常完成（执行器拒绝任务等调度层面的异常除外），
 *       调用方可用{@link ChunkResults#getUncompletedChunks()}直接拿到待重传的分片，
 *       适用于分片上传这类需要失败重试的场景。
 *
 *     线程池约定：
 *     1.由{@code maxConcurrency}重载自建的线程池命名为file-chunk-N、为守护线程，
 *       并在全部分片任务落定后自动shutdown，调用方无需也无法持有该线程池；
 *     2.由调用方传入{@link Executor}的版本不负责关闭该执行器；
 *     3.传入{@link AsyncTaskExecutor}的版本复用框架（或调用方）持有的执行器，
 *       既不创建线程池也不关闭线程池，并发数由提交窗口严格限定，
 *       适合分片上传这类需要与HTTP请求共用执行器的场景。
 *
 *     分片内容的两种取法：
 *     1.{@link FileChunk#getContent()}：整片载入内存，适用于需要字节数组的场景；
 *     2.{@link FileChunk#writeContentTo(java.io.OutputStream)}：分块写出，内存占用恒定，大分片推荐。
 *     每个分片均自带{@code getTotalSize()/getChunkSize()/getTotalChunks()}元数据，
 *     可直接用于上报分片上传协议参数，也可脱离分片集合对单片做独立重试。
 *
 *     用法示例：
 *     {@code
 *     // 同步顺序上传
 *     FileChunkHandle.of(file, 5 * 1024 * 1024)
 *             .handle(chunk -> uploadService.upload(chunk.getFile(), chunk.getContent(), chunk.getIndex()),
 *                     () -> log.info("Chunk upload completed"));
 *
 *     // 限并发上传并收集每片结果，失败分片不会进入结果集，失败原因从future获取
 *     FileChunkHandle.of(file)
 *             .asyncHandle(chunk -> uploadService.upload(chunk.getFile(), chunk.getContent(), chunk.getIndex()),
 *                          results -> log.info("Success chunk count: " + results.size()),
 *                          8)
 *             .join();
 *
 *     // 失败分片也进入结果集，拿到未上传成功的分片直接重传
 *     ChunkResults<Boolean> results = FileChunkHandle.of(file)
 *             .asyncSettleHandle(chunk -> uploadService.upload(chunk.getFile(), chunk.getContent(), chunk.getIndex()), 8)
 *             .join();
 *     if (results.hasExecutionFailure()) {
 *         results.getUncompletedChunks().forEach(chunk -> retryQueue.add(chunk));
 *     }
 *
 *     // 复用框架执行器做限并发上传，并把待重传的分片单独再传一轮
 *     AsyncTaskExecutor taskExecutor = methodContext.getAsyncTaskExecutor();
 *     ChunkResults<Boolean> results = FileChunkHandle.asyncSettleHandle(retryChunks, taskExecutor,
 *             chunk -> uploadService.upload(chunk.getFile(), chunk.getContent(), chunk.getIndex()), 8)
 *             .join();
 *     }
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 05:05
 * @see ChunkUtils
 * @see FileChunk
 * @see ChunkResults
 * @see ChunkResultStatus
 */
public class FileChunkHandle {

    /**
     * 线程名前缀，便于在线程栈与日志中识别分片任务
     */
    private static final String THREAD_NAME_PREFIX = "file-chunk-";

    /**
     * 待分片的原始文件
     */
    private final File file;

    /**
     * 单个分片的大小（字节），必须大于0且不超过{@link ChunkConstants#MAX_CHUNK_SIZE}
     */
    private final long chunkSize;

    /**
     * 私有构造器，实例统一通过{@link #of(File, long)}或{@link #of(File)}创建
     *
     * @param file      原始文件
     * @param chunkSize 分片大小
     */
    private FileChunkHandle(File file, long chunkSize) {
        this.file = file;
        this.chunkSize = chunkSize;
    }

    /**
     * 获取一个文件分片处理器
     *
     * @param file      原始文件对象
     * @param chunkSize 分片大小，需大于0且不超过{@link ChunkConstants#MAX_CHUNK_SIZE}，
     *                  超限时在{@link #getFileChunks()}阶段抛出{@link IllegalArgumentException}
     * @return 文件分片处理器
     */
    public static FileChunkHandle of(File file, long chunkSize) {
        return new FileChunkHandle(file, chunkSize);
    }

    /**
     * 获取一个文件分片处理器，使用默认分片大小（5M，即{@link ChunkConstants#DEFAULT_CHUNK_SIZE}）
     *
     * @param file 原始文件对象
     * @return 文件分片处理器
     */
    public static FileChunkHandle of(File file) {
        return new FileChunkHandle(file, ChunkConstants.DEFAULT_CHUNK_SIZE);
    }

    /**
     * 同步分片文件处理，按分片顺序在当前线程逐个处理
     * <pre>
     *     任一分片处理抛出异常时立即中断本次循环，此时onComplete不会被执行；
     *     若希望「失败也一定收到完成回调」，请使用异步版本{@link #asyncHandle(Consumer, Runnable, int)}。
     * </pre>
     *
     * @param onHandle   单个分片的处理逻辑，如上传该分片
     * @param onComplete 全部分片处理完成后的收尾逻辑，为空文件时也会被执行一次
     * @throws IllegalArgumentException 文件不存在、不是常规文件，或分片大小非法
     * @throws RuntimeException         分片处理逻辑抛出的异常，原样向上传播
     */
    public void handle(Consumer<FileChunk> onHandle, Runnable onComplete) {

        Assert.notNull(onHandle, "OnHandle consumer must not be null");
        Assert.notNull(onComplete, "OnComplete runnable must not be null");

        List<FileChunk> fileChunks = getFileChunks();
        for (FileChunk fileChunk : fileChunks) {
            onHandle.accept(fileChunk);
        }
        onComplete.run();
    }

    /**
     * 同步分片文件处理，收集每个分片的处理结果并一次性交给回调
     * <pre>
     *     结果顺序与分片索引顺序一致；处理逻辑返回null时结果列表中对应位置也为null。
     *     任一分片抛异常时立即中断，onComplete不会被执行。
     * </pre>
     *
     * @param onHandle   单个分片的处理逻辑，其返回值即为该分片的处理结果
     * @param onComplete 收到全部分片结果集合的收尾逻辑
     * @param <T>        分片文件处理结果类型
     * @throws IllegalArgumentException 文件不存在、不是常规文件，或分片大小非法
     * @throws RuntimeException         分片处理逻辑抛出的异常，原样向上传播
     */
    public <T> void handle(Function<FileChunk, T> onHandle, Consumer<List<T>> onComplete) {

        Assert.notNull(onHandle, "OnHandle function must not be null");
        Assert.notNull(onComplete, "OnComplete consumer must not be null");

        List<FileChunk> fileChunks = getFileChunks();
        List<T> handleResultList = new ArrayList<>(fileChunks.size());
        for (FileChunk fileChunk : fileChunks) {
            handleResultList.add(onHandle.apply(fileChunk));
        }
        onComplete.accept(handleResultList);
    }

    /**
     * 异步分片文件处理，使用调用方提供的执行器
     * <pre>
     *     每个分片被提交为一个独立的异步任务，本方法立即返回而不等待任务完成。
     *     失败语义见类文档：onComplete一定会被执行，异常只通过返回的future传播，
     *     因此调用方需要消费返回的future（例如{@code .join()}）。
     * </pre>
     *
     * @param executor   异步执行器，本方法不会负责关闭它
     * @param onHandle   单个分片的处理逻辑
     * @param onComplete 全部分片任务落定后的收尾逻辑，无论成败都会被执行一次
     * @return 全部分片任务完成时完成的future；存在失败分片时以{@link CompletionException}完成
     * @throws IllegalArgumentException executor/onHandle/onComplete为null，或文件与分片大小非法
     */
    public CompletableFuture<Void> asyncHandle(Executor executor, Consumer<FileChunk> onHandle, Runnable onComplete) {
        return asyncHandle(getFileChunks(), executor, onHandle, onComplete);
    }

    /**
     * 异步分片文件处理并收集每片结果，使用调用方提供的执行器
     * <pre>
     *     结果集合只包含「处理成功且未被跳过」的分片，顺序与分片索引顺序一致；
     *     失败分片的原因需从返回的future中获取，被中断跳过的分片与返回null的分片均不进入结果集合。
     * </pre>
     *
     * @param executor   异步执行器，本方法不会负责关闭它
     * @param onHandle   单个分片的处理逻辑，其返回值即为该分片的处理结果
     * @param onComplete 收到结果集合的收尾逻辑，无论成败都会被执行一次
     * @param <T>        分片文件处理结果类型
     * @return 携带结果集合的future；存在失败分片时以{@link CompletionException}完成
     * @throws IllegalArgumentException executor/onHandle/onComplete为null，或文件与分片大小非法
     */
    public <T> CompletableFuture<ChunkResults<T>> asyncHandle(Executor executor, Function<FileChunk, T> onHandle, Consumer<ChunkResults<T>> onComplete) {
        return asyncHandle(getFileChunks(), executor, onHandle, onComplete);
    }

    /**
     * 异步分片文件处理，严格控制并发数
     * <pre>
     *     内部按需创建固定大小线程池（大小为{@code min(分片数, maxConcurrency)}且至少为1），
     *     线程为守护线程、命名前缀为{@code file-chunk-}，并在全部分片任务落定后自动关闭。
     *     空文件不创建线程池，直接执行onComplete并返回已完成的future。
     * </pre>
     *
     * @param onHandle       单个分片的处理逻辑
     * @param onComplete     全部分片任务落定后的收尾逻辑，无论成败都会被执行一次
     * @param maxConcurrency 最大并发数，必须大于0
     * @return 全部分片任务完成时完成的future；存在失败分片时以{@link CompletionException}完成
     * @throws IllegalArgumentException onHandle/onComplete为null、maxConcurrency不大于0，或文件与分片大小非法
     */
    public CompletableFuture<Void> asyncHandle(Consumer<FileChunk> onHandle, Runnable onComplete, int maxConcurrency) {

        // 回调与并发数先校验，避免入参非法时白建一个线程池
        Assert.notNull(onHandle, "OnHandle consumer must not be null");
        Assert.notNull(onComplete, "OnComplete runnable must not be null");
        Assert.isTrue(maxConcurrency > 0, "Max concurrency must be greater than 0");

        List<FileChunk> fileChunks = getFileChunks();
        if (fileChunks.isEmpty()) {
            onComplete.run();
            return CompletableFuture.completedFuture(null);
        }

        ExecutorService executorService = createLimitedExecutor(fileChunks.size(), maxConcurrency);
        return asyncHandle(fileChunks, executorService, onHandle, onComplete)
                .whenComplete((unused, throwable) -> executorService.shutdown());
    }

    /**
     * 异步分片文件处理并收集每片结果，严格控制并发数
     * <pre>
     *     线程池的创建与关闭策略同{@link #asyncHandle(Consumer, Runnable, int)}；
     *     空文件不创建线程池，onComplete会收到空结果集合并返回已完成的future。
     * </pre>
     *
     * @param onHandle       单个分片的处理逻辑，其返回值即为该分片的处理结果
     * @param onComplete     收到结果集合的收尾逻辑，无论成败都会被执行一次
     * @param maxConcurrency 最大并发数，必须大于0
     * @param <T>            分片文件处理结果类型
     * @return 携带结果集合的future；存在失败分片时以{@link CompletionException}完成
     * @throws IllegalArgumentException onHandle/onComplete为null、maxConcurrency不大于0，或文件与分片大小非法
     */
    public <T> CompletableFuture<ChunkResults<T>> asyncHandle(Function<FileChunk, T> onHandle, Consumer<ChunkResults<T>> onComplete, int maxConcurrency) {

        Assert.notNull(onHandle, "OnHandle function must not be null");
        Assert.notNull(onComplete, "OnComplete consumer must not be null");
        Assert.isTrue(maxConcurrency > 0, "Max concurrency must be greater than 0");

        List<FileChunk> fileChunks = getFileChunks();
        if (fileChunks.isEmpty()) {
            ChunkResults<T> emptyResults = new ChunkResults<>(Collections.emptyList());
            onComplete.accept(emptyResults);
            return CompletableFuture.completedFuture(emptyResults);
        }

        ExecutorService executorService = createLimitedExecutor(fileChunks.size(), maxConcurrency);
        return asyncHandle(fileChunks, executorService, onHandle, onComplete)
                .whenComplete((unused, throwable) -> executorService.shutdown());
    }

    /**
     * 异步分片文件处理（失败收集模式），使用调用方提供的执行器
     * <pre>
     *     与{@link #asyncHandle(Executor, Function, Consumer)}的区别在于失败分片的去向：
     *     本方法把成功、异常失败、被跳过三种分片全部放进{@link ChunkResults}，
     *     分片处理逻辑抛出的异常已记录在{@link ChunkResult#getError()}中，
     *     因此除执行器拒绝任务等调度异常外，返回的future总是正常完成，调用方不必再注册异常处理。
     *     适合「先全部提交、再根据结果重传」的分片上传场景。
     * </pre>
     *
     * @param executor 异步执行器，本方法不会负责关闭它
     * @param onHandle 单个分片的处理逻辑，其返回值即为该分片的处理结果
     * @param <T>      分片文件处理结果类型
     * @return 携带全量结果的future；只有调度异常（如执行器拒绝任务）才会使其异常完成
     * @throws IllegalArgumentException executor/onHandle为null，或文件与分片大小非法
     */
    public <T> CompletableFuture<ChunkResults<T>> asyncSettleHandle(Executor executor, Function<FileChunk, T> onHandle) {
        return asyncSettleHandle(getFileChunks(), executor, onHandle);
    }

    /**
     * 异步分片文件处理（失败收集模式），处理调用方给定的分片集合
     * <pre>
     *     与实例版本的差别是分片来源：本方法不重新切分文件，直接处理传入的分片，
     *     因此可以把上一轮{@link ChunkResults#getUncompletedChunks()}拿到的待重传分片原样提交重试，
     *     不必为了重传几个分片而切分整个文件。
     *     传入的分片可以来自不同文件，也可以为空列表（空列表返回已完成future，不含任何结果）。
     *     本方法同样不会关闭传入的执行器。{@code settle}收集语义见
     *     {@link #asyncSettleHandle(Executor, Function)}。
     * </pre>
     *
     * @param fileChunks 待处理的分片集合，不可为null
     * @param executor   异步执行器，本方法不负责关闭
     * @param onHandle   单个分片的处理逻辑，其返回值即为该分片的处理结果
     * @param <T>        分片文件处理结果类型
     * @return 携带全量结果的future；只有调度异常（如执行器拒绝任务）才会使其异常完成
     * @throws IllegalArgumentException fileChunks/executor/onHandle为null
     */
    public static <T> CompletableFuture<ChunkResults<T>> asyncSettleHandle(List<FileChunk> fileChunks, Executor executor, Function<FileChunk, T> onHandle) {

        Assert.notNull(fileChunks, "FileChunks must not be null");
        Assert.notNull(executor, "Executor must not be null");
        Assert.notNull(onHandle, "OnHandle function must not be null");

        if (fileChunks.isEmpty()) {
            return CompletableFuture.completedFuture(new ChunkResults<T>(Collections.<ChunkResult<T>>emptyList()));
        }
        return doAsyncHandle(fileChunks, executor, onHandle, null, true);
    }

    /**
     * 异步分片文件处理（失败收集模式），严格控制并发数
     * <pre>
     *     线程池的创建与关闭策略同{@link #asyncHandle(Function, Consumer, int)}：
     *     线程数为{@code min(分片数, maxConcurrency)}、守护线程、命名{@code file-chunk-N}，任务落定后自动shutdown。
     *     空文件不创建线程池，直接返回包含空结果集的已完成future。
     * </pre>
     *
     * @param onHandle       单个分片的处理逻辑，其返回值即为该分片的处理结果
     * @param maxConcurrency 最大并发数，必须大于0
     * @param <T>            分片文件处理结果类型
     * @return 携带全量结果的future；只有调度异常（如执行器拒绝任务）才会使其异常完成
     * @throws IllegalArgumentException onHandle为null、maxConcurrency不大于0，或文件与分片大小非法
     */
    public <T> CompletableFuture<ChunkResults<T>> asyncSettleHandle(Function<FileChunk, T> onHandle, int maxConcurrency) {

        Assert.notNull(onHandle, "OnHandle function must not be null");
        Assert.isTrue(maxConcurrency > 0, "Max concurrency must be greater than 0");

        List<FileChunk> fileChunks = getFileChunks();
        if (fileChunks.isEmpty()) {
            return CompletableFuture.completedFuture(new ChunkResults<>(Collections.<ChunkResult<T>>emptyList()));
        }

        ExecutorService executorService = createLimitedExecutor(fileChunks.size(), maxConcurrency);
        return doAsyncHandle(fileChunks, executorService, onHandle, null, true)
                .whenComplete((unused, throwable) -> executorService.shutdown());
    }

    /**
     * 异步分片文件处理（失败收集模式），使用框架的异步任务执行器并严格控制并发数
     * <pre>
     *     与{@link #asyncSettleHandle(Executor, Function)}的差别在执行器类型与并发控制方式：
     *     1.任务通过{@link AsyncTaskExecutor#supplyAsync(java.util.function.Supplier)}提交，
     *       该执行器（通常是{@code MethodContext#getAsyncTaskExecutor()}返回的共享执行器）
     *       的生命周期归调用方/框架管理，本方法既不创建也不关闭任何线程池；
     *     2.并发数由提交窗口保证：每提交满{@code maxConcurrency}个任务就等这一批落定再提交下一批，
     *       因此不依赖执行器自身的并发配置（{@code AsyncTaskExecutor}默认不限制并发）；
     *     3.结果集覆盖全部分片：处理异常记为{@link ChunkResultStatus#FAILED}、被止损跳过的记为
     *       {@link ChunkResultStatus#SKIPPED}、执行器拒绝任务也记为FAILED，
     *       因此除入参非法外本方法不抛异常，返回的future也总是正常完成。
     * </pre>
     *
     * @param executor       异步任务执行器，本方法不会关闭它
     * @param onHandle       单个分片的处理逻辑，其返回值即为该分片的处理结果
     * @param maxConcurrency 最大并发数，必须大于0
     * @param <T>            分片文件处理结果类型
     * @return 携带全量结果的future，全部分片落定时完成
     * @throws IllegalArgumentException executor/onHandle为null、maxConcurrency不大于0，或文件与分片大小非法
     */
    public <T> CompletableFuture<ChunkResults<T>> asyncSettleHandle(AsyncTaskExecutor executor, Function<FileChunk, T> onHandle, int maxConcurrency) {
        return asyncSettleHandle(getFileChunks(), executor, onHandle, maxConcurrency);
    }

    /**
     * 异步分片文件处理（失败收集模式），处理调用方给定的分片集合，使用框架的异步任务执行器并严格控制并发数
     * <pre>
     *     本方法不重新切分文件，直接处理传入的分片，因此可以把上一轮
     *     {@link ChunkResults#getUncompletedChunks()}拿到的待重传分片原样提交重试。
     *     传入的分片可以来自不同文件，也可以为空集合（空集合直接返回已完成future，不含任何结果）。
     *     提交窗口、执行器生命周期与“全部分片一定落进结果集”的语义见
     *     {@link #asyncSettleHandle(AsyncTaskExecutor, Function, int)}。
     * </pre>
     *
     * @param fileChunks     待处理的分片集合，不可为null
     * @param executor       异步任务执行器，本方法不会关闭它
     * @param onHandle       单个分片的处理逻辑，其返回值即为该分片的处理结果
     * @param maxConcurrency 最大并发数，必须大于0
     * @param <T>            分片文件处理结果类型
     * @return 携带全量结果的future，全部分片落定时完成
     * @throws IllegalArgumentException fileChunks/executor/onHandle为null或maxConcurrency不大于0
     */
    public static <T> CompletableFuture<ChunkResults<T>> asyncSettleHandle(List<FileChunk> fileChunks, AsyncTaskExecutor executor, Function<FileChunk, T> onHandle, int maxConcurrency) {

        Assert.notNull(fileChunks, "FileChunks must not be null");
        Assert.notNull(executor, "AsyncTaskExecutor must not be null");
        Assert.notNull(onHandle, "OnHandle function must not be null");
        Assert.isTrue(maxConcurrency > 0, "Max concurrency must be greater than 0");

        if (fileChunks.isEmpty()) {
            return CompletableFuture.completedFuture(new ChunkResults<>(Collections.<ChunkResult<T>>emptyList()));
        }

        // 中断标志：任一分片失败后置为true，让尚未提交的分片直接记为被跳过
        AtomicBoolean aborted = new AtomicBoolean(false);
        List<CompletableFuture<ChunkResult<T>>> futures = new ArrayList<>(fileChunks.size());
        for (FileChunk fileChunk : fileChunks) {
            // 失败止损：已判定中断后的分片不再提交，但仍补一个跳过结果，保证结果集与分片一一对应
            if (aborted.get()) {
                futures.add(CompletableFuture.completedFuture(ChunkResult.<T>skipped(fileChunk)));
                continue;
            }
            futures.add(submitSettleTask(fileChunk, executor, onHandle, aborted));
            // 提交窗口：满一批就等这批落定，避免一次性把全部分片压进共享线程池
            awaitWindowCompletion(futures, fileChunks, maxConcurrency);
        }

        // 每个future都已落定（异常也已换成FAILED结果），allOf不会异常完成，结果集直接收集即可
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .handle((unused, throwable) -> collectResults(futures));
    }

    /**
     * 把一个分片的失败收集任务提交给异步任务执行器
     * <pre>
     *     分片逻辑的异常在任务内部就被转换成{@link ChunkResultStatus#FAILED}，
     *     执行器拒绝等同步抛出的异常也在本方法内转换成失败结果，
     *     目的是让调用方总能拿到齐全的结果集，而不必处理future异常。
     * </pre>
     *
     * @param fileChunk 待处理的分片
     * @param executor  异步任务执行器
     * @param onHandle  单个分片的处理逻辑
     * @param aborted   失败止损标志
     * @param <T>       分片文件处理结果类型
     * @return 该分片的结果future，异常情况下为已完成的失败结果
     */
    private static <T> CompletableFuture<ChunkResult<T>> submitSettleTask(FileChunk fileChunk, AsyncTaskExecutor executor, Function<FileChunk, T> onHandle, AtomicBoolean aborted) {
        try {
            return executor.supplyAsync(() -> {
                // 失败止损：已进入队列但未开始的分片不再请求服务端，记为被跳过
                if (aborted.get()) {
                    return ChunkResult.<T>skipped(fileChunk);
                }
                try {
                    return ChunkResult.of(fileChunk, onHandle.apply(fileChunk));
                } catch (RuntimeException | Error e) {
                    aborted.set(true);
                    return ChunkResult.<T>failed(fileChunk, e);
                }
            });
        } catch (RuntimeException | Error e) {
            // 执行器拒绝任务：本分片记为失败并停止后续提交，保证异常不沿结果收集链路外泄
            aborted.set(true);
            return CompletableFuture.completedFuture(ChunkResult.<T>failed(fileChunk, e));
        }
    }

    /**
     * 提交满一批时等待这批任务落定，用于限制同时在途的异步任务数量
     * <pre>
     *     失败收集模式下分片逻辑的异常已落到结果里，这里的{@code join()}只会因
     *     future自身异常（如被取消）而抛出，因而不向调用方传播，而是就地换成
     *     {@link ChunkResultStatus#FAILED}结果补位，避免{@link #collectResults}丢分片。
     * </pre>
     *
     * @param futures        已提交任务的结果列表，与分片列表下标一一对应
     * @param fileChunks     分片列表
     * @param maxConcurrency 最大并发数，必须大于0
     * @param <T>            分片文件处理结果类型
     */
    private static <T> void awaitWindowCompletion(List<CompletableFuture<ChunkResult<T>>> futures, List<FileChunk> fileChunks, int maxConcurrency) {
        int size = futures.size();
        if (size == 0 || size % maxConcurrency != 0) {
            return;
        }
        for (int i = size - maxConcurrency; i < size; i++) {
            try {
                futures.get(i).join();
            } catch (CompletionException | CancellationException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                futures.set(i, CompletableFuture.completedFuture(ChunkResult.<T>failed(fileChunks.get(i), cause)));
            }
        }
    }

    /**
     * 异步分片文件处理的实现体（无结果收集版本）
     *
     * @param fileChunks 待处理的分片集合
     * @param executor   异步执行器
     * @param onHandle   单个分片的处理逻辑
     * @param onComplete 收尾逻辑
     * @return 全部分片任务完成时完成的future；存在失败分片时以{@link CompletionException}完成
     */
    private CompletableFuture<Void> asyncHandle(List<FileChunk> fileChunks, Executor executor, Consumer<FileChunk> onHandle, Runnable onComplete) {

        Assert.notNull(executor, "Executor must not be null");
        Assert.notNull(onHandle, "OnHandle consumer must not be null");
        Assert.notNull(onComplete, "OnComplete runnable must not be null");

        // 中断标志：任一分片失败后置为true，让尚未开始的分片放弃执行
        AtomicBoolean aborted = new AtomicBoolean(false);
        CompletableFuture<?>[] futureArray = new CompletableFuture[fileChunks.size()];
        for (int i = 0; i < fileChunks.size(); i++) {
            final FileChunk fileChunk = fileChunks.get(i);
            futureArray[i] = CompletableFuture.runAsync(() -> {
                if (aborted.get()) {
                    return;
                }
                try {
                    onHandle.accept(fileChunk);
                } catch (RuntimeException | Error e) {
                    aborted.set(true);
                    throw e;
                }
            }, executor);
        }

        // allOf在全部任务落定（成功或异常）后才完成，因此whenComplete中的onComplete总能被执行一次；
        // whenComplete不会吞掉异常，失败原因继续沿返回的future向调用方传播
        return CompletableFuture.allOf(futureArray)
                .whenComplete((unused, throwable) -> onComplete.run());
    }

    /**
     * 异步分片文件处理的实现体（带结果收集版本，失败分片不进结果集）
     *
     * @param fileChunks 待处理的分片集合
     * @param executor   异步执行器
     * @param onHandle   单个分片的处理逻辑
     * @param onComplete 收到成功分片结果集合的收尾逻辑
     * @param <T>        分片文件处理结果类型
     * @return 携带结果集合的future；存在失败分片时以{@link CompletionException}完成
     */
    private <T> CompletableFuture<ChunkResults<T>> asyncHandle(List<FileChunk> fileChunks, Executor executor, Function<FileChunk, T> onHandle, Consumer<ChunkResults<T>> onComplete) {

        Assert.notNull(onComplete, "OnComplete consumer must not be null");
        return doAsyncHandle(fileChunks, executor, onHandle, onComplete, false);
    }

    /**
     * 异步分片处理（收集返回值版本）的公共实现
     * <pre>
     *     settle参数决定两种收集模式：
     *     1.settle为false（{@code asyncHandle}）：异常向上抛出使分片future异常完成，
     *       被跳过的分片返回null，两者均不进入结果集，失败原因通过总future传播；
     *     2.settle为true（{@code asyncSettleHandle}）：异常包装为{@link ChunkResultStatus#FAILED}结果、
     *       被跳过的分片包装为{@link ChunkResultStatus#SKIPPED}结果，全部进入结果集，
     *       总future只在调度层面出错时才异常完成。
     * </pre>
     *
     * @param fileChunks 待处理的分片集合
     * @param executor   异步执行器，本方法不负责关闭
     * @param onHandle   单个分片的处理逻辑
     * @param onComplete 收到结果集合的收尾逻辑，为null时不回调（失败收集模式无需onComplete）
     * @param settle     是否将失败与被跳过的分片一并收集
     * @param <T>        分片文件处理结果类型
     * @return 携带结果集合的future
     */
    private static <T> CompletableFuture<ChunkResults<T>> doAsyncHandle(List<FileChunk> fileChunks, Executor executor, Function<FileChunk, T> onHandle, Consumer<ChunkResults<T>> onComplete, boolean settle) {

        Assert.notNull(executor, "Executor must not be null");
        Assert.notNull(onHandle, "OnHandle function must not be null");

        AtomicBoolean aborted = new AtomicBoolean(false);
        List<CompletableFuture<ChunkResult<T>>> futures = new ArrayList<>(fileChunks.size());
        for (FileChunk fileChunk : fileChunks) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                // 失败止损：非settle模式用null表示该分片无结果，settle模式则记录为被跳过
                if (aborted.get()) {
                    return settle ? ChunkResult.<T>skipped(fileChunk) : null;
                }
                try {
                    return ChunkResult.of(fileChunk, onHandle.apply(fileChunk));
                } catch (RuntimeException | Error e) {
                    aborted.set(true);
                    if (settle) {
                        // 失败收集模式：异常已经落到结果集里，不再向future传播
                        return ChunkResult.<T>failed(fileChunk, e);
                    }
                    throw e;
                }
            }, executor));
        }

        CompletableFuture<?>[] futureArray = futures.toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(futureArray)
                .handle((unused, throwable) -> {
                    ChunkResults<T> chunkResults = collectResults(futures);
                    if (onComplete != null) {
                        onComplete.accept(chunkResults);
                    }
                    if (throwable != null) {
                        // onComplete已拿到结果集，失败原因仍通过future传播给调用方；
                        // allOf的异常已是CompletionException包装，此处不再重复包装，保证各条异步路径的异常层级一致
                        throw throwable instanceof CompletionException
                                ? (CompletionException) throwable
                                : new CompletionException(throwable);
                    }
                    return chunkResults;
                });
    }

    /**
     * 收集各分片 future 的结果
     * <pre>
     *     只在全部分片任务落定后调用：
     *     1.非settle模式下失败分片与被中断跳过的分片（结果为null）不进入结果集合；
     *     2.settle模式下三种结果均非null，会全部保留。
     * </pre>
     *
     * @param futures 每个分片对应的结果future
     * @param <T>     分片文件处理结果类型
     * @return 结果集合，顺序与分片顺序一致
     */
    private static <T> ChunkResults<T> collectResults(List<CompletableFuture<ChunkResult<T>>> futures) {
        List<ChunkResult<T>> results = new ArrayList<>(futures.size());
        for (CompletableFuture<ChunkResult<T>> future : futures) {
            // 将异常完成转换为结果为null的正常完成，避免getNow在失败分片上抛异常
            ChunkResult<T> result = future.handle((chunkResult, throwable) -> chunkResult).join();
            if (result != null) {
                results.add(result);
            }
        }
        return new ChunkResults<>(results);
    }

    /**
     * 创建严格限制并发数的线程池
     * <pre>
     *     线程数为{@code max(1, min(taskCount, maxConcurrency))}，
     *     线程为守护线程且命名为{@code file-chunk-N}，便于问题定位；
     *     使用默认无界队列，任务数由分片数天然限定，不会无限增长。
     * </pre>
     *
     * @param taskCount      待处理的分片数
     * @param maxConcurrency 最大并发数，必须大于0
     * @return 固定大小线程池
     */
    private ExecutorService createLimitedExecutor(int taskCount, int maxConcurrency) {
        int poolSize = Math.max(1, Math.min(taskCount, maxConcurrency));
        AtomicInteger threadIndex = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME_PREFIX + threadIndex.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(poolSize, threadFactory);
    }

    /**
     * 获取所有分片文件集合
     * <pre>
     *     每次调用都会按当前文件长度重新切分，因此分片处理期间文件被改动时，
     *     读取分片内容可能抛出{@link com.luckyframework.exception.LuckyIOException}。
     * </pre>
     *
     * @return 所有分片文件集合，空文件返回空集合
     * @throws IllegalArgumentException 文件不存在、不是常规文件，或分片大小非法
     */
    public List<FileChunk> getFileChunks() {
        return ChunkUtils.shard(file, chunkSize);
    }

    /**
     * 获取原始文件对象
     *
     * @return 原始文件对象
     */
    public File getFile() {
        return file;
    }

    /**
     * 获取分片大小
     *
     * @return 分片大小
     */
    public long getChunkSize() {
        return chunkSize;
    }
}
