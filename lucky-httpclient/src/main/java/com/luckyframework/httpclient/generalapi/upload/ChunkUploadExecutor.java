package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.ChunkResult;
import com.luckyframework.httpclient.generalapi.chunk.ChunkResultClassify;
import com.luckyframework.httpclient.generalapi.chunk.ChunkResults;
import com.luckyframework.httpclient.generalapi.chunk.ChunkUtils;
import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.generalapi.chunk.FileChunkHandle;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.function.DigestFunctions;
import com.luckyframework.retry.FixedWaitBeforeRetry;
import com.luckyframework.retry.RetryFailureException;
import com.luckyframework.retry.RetryUtils;
import com.luckyframework.retry.RetryWaiteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 分片上传执行内核。
 * <pre>
 *     本类把「一个大文件怎么可靠地传完」这件事固定下来，与「服务端接口长什么样」无关：
 *     1.解析文件标识：调用方给了fileId（方法参数或注解{@code fileId}属性）就直接使用，
 *       否则调用{@link ChunkUploadApi#initUpload}请服务端生成，服务端不生成时退回文件内容MD5；
 *       服务端生成失败则本次上传直接失败结束；
 *     2.开启秒传（{@link ChunkUploadOptions#isInstantUpload()}）时先调用
 *       {@link ChunkUploadApi#checkInstantUpload}探测，命中则跳过切片与全部上传动作直接返回；
 *     3.按{@link ChunkUploadOptions#getChunkSize()}切分文件；
 *     4.开启断点续传时逐片询问{@link ChunkUploadApi#isChunkUploaded}该片服务端是否已有，
 *       命中的分片跳过；判据本身失败时降级为全量上传；
 *     5.把分片任务提交给外部注入的{@link AsyncTaskExecutor}，并用提交窗口把在途任务数
 *       限制在{@link ChunkUploadOptions#getMaxConcurrency()}以内；
 *     6.每轮结束后把「执行异常」「被止损跳过」「接口返回false」三类分片合并成重传集合，
 *       等待{@link ChunkUploadOptions#getRetryBackoffMillis()}后只重传这部分分片，
 *       最多重传{@link ChunkUploadOptions#getMaxRetryCount()}轮（小于0表示无限重传直到全部成功），
 *       重试轮次与退避统一交由{@link RetryUtils}驱动；
 *     7.全部分片上传成功后才按需通知服务端合并（{@link ChunkMergeContext}随文件级元信息一并
 *       交给协议），服务端返回的内部文件ID等信息随{@link ChunkUploadResponse#getData()}透传给调用方；
 *     8.合并实际发起且仍失败时，按需调用{@link ChunkUploadApi#deleteChunks}回收服务端的孤儿分片
 *       （{@link ChunkUploadOptions#isDeleteOnMergeFailure()}，默认关闭）；
 *     9.无论本次上传成功、失败还是中途抛出异常，最后都会调用一次
 *       {@link ChunkUploadApi#releaseResources}回收资源。
 *
 *     失败处理约定（重要）：
 *     1.分片失败、重试用尽、合并失败<b>都不会抛异常</b>，一律通过
 *       {@link ChunkUploadResult#isSuccess()}与{@link ChunkUploadResult#getFailedChunks()}表达，
 *       因此调用方必须主动判断结果，不能靠捕获异常来区分成败；
 *     2.只有参数非法（文件不存在、{@link ChunkUploadOptions#validate()}不通过、
 *       执行器拒绝任务等）才会抛出{@link IllegalArgumentException}，
 *       这类问题在发起任何请求之前就会被发现；
 *     3.查询分片是否已上传的判据失败不会中断本次上传，会从失败的那一片起降级为全量上传并记录warn日志；
 *     4.秒传探测失败同样不会中断本次上传，只记warn后继续走正常分片流程，
 *       命中与否见{@link ChunkUploadResult#isInstantUploaded()}；
 *     5.合并失败后的分片删除同样不抛异常，结果见{@link ChunkUploadResult#isChunksDeleted()}与
 *       {@link ChunkUploadResult#getDeleteError()}；
 *     6.申请文件标识失败（{@link ChunkUploadApi#initUpload}抛异常）同样不抛异常，
 *       但也不上传任何分片、不发起合并，整体直接失败，原因见{@link ChunkUploadResult#getInitError()}；
 *     7.释放资源（{@link ChunkUploadApi#releaseResources}）抛出的异常也不向外抛，只记error日志：
 *       它是收尾动作，既不能覆盖本次上传结果，也不能替换原本要向上传播的异常。
 *
 *     执行器使用约定：
 *     1.本方法为阻塞调用，返回时全部分片已落定（内部对每轮{@code join()}）；
 *     2.分片任务全部提交给构造时传入的{@link AsyncTaskExecutor}，本类<b>既不创建也不关闭任何线程池</b>，
 *       执行器的生命周期归调用方（注解方式下用{@code MethodContext#getAsyncTaskExecutor()}，
 *       与{@code @RangeDownload}用的是同一个共享执行器）；
 *     3.并发数由提交窗口限定，不依赖执行器自身的并发配置（{@link AsyncTaskExecutor}默认不限并发）；
 *     4.结果列表（成功/失败/跳过）均按分片索引升序，见{@link ChunkUploadResult}。
 *
 *     用法示例：
 *     {@code
 *     ChunkUploadApi api = ...;                           // 由使用者实现的服务端协议
 *     AsyncTaskExecutor taskExecutor = ...;           // 框架共享执行器或调用方自己的执行器
 *     ChunkUploadExecutor executor = new ChunkUploadExecutor(api,
 *             new ChunkUploadOptions().setChunkSize(1024 * 1024).setMaxRetryCount(5), taskExecutor);
 *     ChunkUploadResult result = executor.upload(new File("D:/big.zip"), "fileId-001");
 *     if (!result.isSuccess()) {
 *         log.warn("Chunks not uploaded completely: {}", result.getFailedChunks());   // 可据此单独重传
 *     }
 *     }
 *
 *     需要以注解方式声明式使用时见{@link ChunkUpload}。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-21 11:10
 * @see ChunkUploadApi
 * @see ChunkUploadOptions
 * @see ChunkUploadResult
 * @see FileChunkHandle
 */
public class ChunkUploadExecutor {

    private static final Logger log = LoggerFactory.getLogger(ChunkUploadExecutor.class);

    /**
     * 还没有切片时传给释放动作的空分片清单
     */
    private static final ChunkMergeContext EMPTY_MERGE_CONTEXT = ChunkMergeContext.ofAllChunks(Collections.emptyList());

    /**
     * 服务端协议实现，上传、合并、删除动作都由它发出真实请求（删除是可选动作）
     */
    private final ChunkUploadApi uploadApi;

    /**
     * 上传行为配置，构造时校验一次，避免每次上传重复校验
     */
    private final ChunkUploadOptions options;

    /**
     * 分片任务的提交执行器，由外部注入，本类不创建也不关闭它
     */
    private final AsyncTaskExecutor asyncTaskExecutor;

    /**
     * 使用默认配置创建上传内核
     *
     * @param uploadApi         服务端协议实现，不可为null
     * @param asyncTaskExecutor 分片任务的异步执行器，不可为null，本类不会关闭它
     * @throws IllegalArgumentException uploadApi/asyncTaskExecutor为null，或默认配置不合法
     */
    public ChunkUploadExecutor(ChunkUploadApi uploadApi, AsyncTaskExecutor asyncTaskExecutor) {
        this(uploadApi, new ChunkUploadOptions(), asyncTaskExecutor);
    }

    /**
     * 创建上传内核
     *
     * @param uploadApi         服务端协议实现，不可为null
     * @param options           上传行为配置，不可为null
     * @param asyncTaskExecutor 分片任务的异步执行器，不可为null，本类不会关闭它
     * @throws IllegalArgumentException 任一参数为null，或options不合法
     */
    public ChunkUploadExecutor(ChunkUploadApi uploadApi, ChunkUploadOptions options, AsyncTaskExecutor asyncTaskExecutor) {
        Assert.notNull(uploadApi, "UploadApi must not be null");
        Assert.notNull(options, "UploadOptions must not be null");
        Assert.notNull(asyncTaskExecutor, "AsyncTaskExecutor must not be null");
        options.validate();
        this.uploadApi = uploadApi;
        this.options = options;
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    /**
     * 分片上传一个文件，文件名与文件标识都由内核对文件取值
     * <pre>
     *     文件名取原始文件名；文件标识按「服务端生成 → 文件内容MD5」的顺序解析，
     *     需要服务端生成时在协议接口里覆写{@link ChunkUploadApi#initUpload}。
     * </pre>
     *
     * @param file 待上传的文件，不可为null
     * @return 上传结果；失败信息记录在结果里，不会抛异常
     * @throws IllegalArgumentException 文件不存在/不是常规文件，或配置不合法
     */
    public ChunkUploadResult upload(File file) {
        Assert.notNull(file, "File must not be null");
        return upload(file, null, file.getName());
    }

    /**
     * 分片上传一个文件，服务端保存时使用原文件名
     *
     * @param file   待上传的文件，不可为null
     * @param fileId 文件唯一标识，可为null或空串——此时由内核对服务端生成与MD5兜底
     * @return 上传结果；失败信息记录在结果里，不会抛异常
     * @throws IllegalArgumentException 文件不存在/不是常规文件，或配置不合法
     */
    public ChunkUploadResult upload(File file, String fileId) {
        Assert.notNull(file, "File must not be null");
        return upload(file, fileId, file.getName());
    }

    /**
     * 分片上传一个文件
     * <pre>
     *     本方法阻塞直到全部分片落定（含重试用尽）。
     *     文件标识的解析顺序为：显式给出的{@code fileId} → {@link ChunkUploadApi#initUpload}
     *     由服务端生成 → 文件内容MD5；服务端生成失败时本次上传直接结束，不发起任何分片请求。
     *     开启秒传时先探测服务端，命中则不产生任何分片直接成功返回。
     *     空文件没有任何分片，会直接进入合并阶段并以{@code totalChunks=0}通知服务端，
     *     由服务端决定如何创建空文件。
     * </pre>
     *
     * @param file     待上传的文件，不可为null
     * @param fileId   文件唯一标识，可为null或空串——此时由内核对服务端生成与MD5兜底
     * @param fileName 服务端保存时使用的文件名，不可为空串
     * @return 上传结果；失败信息记录在结果里，不会抛异常
     * @throws IllegalArgumentException 文件不存在/不是常规文件、fileName为空，或配置不合法
     */
    public ChunkUploadResult upload(File file, String fileId, String fileName) {

        Assert.notNull(file, "File must not be null");
        Assert.hasText(fileName, "FileName must not be empty");
        // 再次校验：配置对象可能被使用者在构造内核之后修改，避免带着非法参数发起请求
        options.validate();
        // 申请文件标识会发起请求，文件本身的校验必须提前到所有网络动作之前
        Assert.isTrue(file.exists(), "File does not exist");
        Assert.isTrue(file.isFile(), "File is not a file");

        ChunkUploadResult.Builder builder = new ChunkUploadResult.Builder();
        long totalSize = file.length();
        long chunkSize = options.getChunkSize();
        int totalChunks = chunkCount(totalSize, chunkSize);
        ChunkUploadMeta baseMeta = ChunkUploadMeta.of(file, null, fileName, totalSize, chunkSize, totalChunks);

        // 释放动作必须必然执行：用try/finally兜住「正常返回」与「中途抛异常」两条出口，
        // 两个变量始终指向此刻已知的最新一份信息，供收尾时回调协议
        ChunkUploadMeta meta = baseMeta;
        ChunkMergeContext chunkContext = EMPTY_MERGE_CONTEXT;
        try {
            ChunkUploadMeta resolvedMeta = resolveMeta(baseMeta, fileId, builder);
            if (resolvedMeta == null) {
                // 服务端生成标识失败：必要准备未完成，不再发起任何分片请求
                return builder.build();
            }
            meta = resolvedMeta;

            // 秒传：切片之前先探测，命中则全部分片与合并动作都不再执行；未命中才不会产生切片开销
            if (tryInstantUpload(meta, builder)) {
                // 命中表示本次没有切片，结果里的分片总数要记0
                meta = meta.withTotalChunks(0);
                builder.meta(meta);
                return builder.build();
            }

            List<FileChunk> allChunks = ChunkUtils.shard(file, chunkSize);
            meta = meta.withTotalChunks(allChunks.size());
            builder.meta(meta);

            // 断点续传：过滤掉服务端已经收到的分片
            List<FileChunk> pendingChunks = resolvePendingChunks(allChunks, meta, builder);
            // 并发上传+失败重传，返回重试用尽后仍未传成功的分片
            List<ChunkResult<Boolean>> failedChunks = uploadWithRetry(pendingChunks, meta, builder);

            builder.replaceFailed(failedChunks);
            // 合并动作除了文件级元信息，还要知道「本次切了哪些片、其中哪些片传成功了」
            chunkContext = ChunkMergeContext.of(allChunks, uploadedChunksOf(pendingChunks, failedChunks));
            mergeIfNeeded(meta, chunkContext, failedChunks.isEmpty(), builder);
            return builder.build();
        } finally {
            releaseResources(meta, chunkContext);
        }
    }

    /**
     * 解析本次上传的文件级元信息，同时确定文件标识
     * <pre>
     *     fileId的解析顺序：
     *     1.调用方显式给出（方法参数或注解{@code fileId}属性）→ 直接使用，不发起申请请求；
     *     2.否则调用{@link ChunkUploadApi#initUpload}由服务端生成；
     *     3.服务端返回null或空串时，用文件内容MD5兜底。
     *     服务端生成属于必要准备，与秒传探测的「失败降级」相反：缺少标识时后续请求注定无效，
     *     因此抛异常就记录进结果并不再继续，避免带着空标识打一堆必败的请求。
     * </pre>
     *
     * @param baseMeta 文件级元信息，fileId尚为null
     * @param fileId   显式指定的文件标识，可为null或空串
     * @param builder  结果构建器，用于记录申请动作与异常
     * @return 完整的文件级元信息；服务端生成标识失败时返回null
     */
    private ChunkUploadMeta resolveMeta(ChunkUploadMeta baseMeta, String fileId, ChunkUploadResult.Builder builder) {

        if (StringUtils.hasText(fileId)) {
            return baseMeta.withFileId(fileId);
        }

        String serverFileId;
        try {
            serverFileId = uploadApi.initUpload(baseMeta);
        } catch (Exception | Error e) {
            log.error("Failed to init the fileId of file[{}], the upload will not start",
                    baseMeta.getFile().getAbsolutePath(), e);
            builder.init(true, e).meta(baseMeta);
            return null;
        }
        builder.init(true, null);
        if (StringUtils.hasText(serverFileId)) {
            log.info("The server generated the fileId[{}] for file[{}]",
                    serverFileId, baseMeta.getFile().getAbsolutePath());
            return baseMeta.withFileId(serverFileId);
        }
        return baseMeta.withFileId(md5Hex(baseMeta.getFile()));
    }

    /**
     * 计算分片总数
     * <pre>
     *     与{@link ChunkUtils#shard(long, long)}同一算法（向上取整、整除时不多切空片），
     *     因为申请文件标识与秒传探测都发生在切片之前，只能用这里算出的片数上报。
     * </pre>
     *
     * @param totalSize 文件总大小
     * @param chunkSize 单片大小
     * @return 分片总数，空文件为0
     */
    private static int chunkCount(long totalSize, long chunkSize) {
        if (totalSize <= 0L) {
            return 0;
        }
        return (int) ((totalSize - 1) / chunkSize + 1);
    }

    /**
     * 计算文件内容的MD5作为文件标识，即服务端不生成标识时的兜底方案
     *
     * @param file 待上传的文件
     * @return 文件内容的MD5十六进制字符串
     * @throws ChunkUploadException 读取文件失败
     */
    private static String md5Hex(File file) {
        try {
            return DigestFunctions.md5Hex(file);
        } catch (Exception e) {
            throw new ChunkUploadException(e, "Failed to compute the MD5 of file '{}' as fileId.", file.getAbsolutePath());
        }
    }

    /**
     * 秒传探测：切片前询问服务端是否已有该文件
     * <pre>
     *     只在开启{@link ChunkUploadOptions#isInstantUpload()}时调用；
     *     命中（{@code isSuccess()}为true）则结果标记为秒传成功且无需合并，
     *     服务端返回的内部文件ID等信息记入{@code getServerData()}；
     *     未命中、返回null、抛异常（含未覆写默认协议抛出的
     *     {@link ChunkUploadNotImplementedException}）都只记warn并继续走正常分片流程，
     *     秒传是优化手段，不应让探测失败拖垮本次上传。
     * </pre>
     *
     * @param meta    文件级元信息，fileId此时一定已确定，其余探测参数由协议实现按需取用
     * @param builder 结果构建器
     * @return 命中秒传返回true，本次上传可以直接结束
     */
    private boolean tryInstantUpload(ChunkUploadMeta meta, ChunkUploadResult.Builder builder) {

        boolean required = options.isInstantUpload();
        builder.instantUpload(required, false, null);
        if (!required) {
            return false;
        }
        String fileId = meta.getFileId();
        ChunkUploadResponse response;
        try {
            response = uploadApi.checkInstantUpload(meta);
        } catch (Exception | Error e) {
            log.warn("Failed to check the instant upload of file[{}], continue with the normal chunk upload: {}", fileId, e.getMessage());
            return false;
        }
        if (response == null || !response.isSuccess()) {
            log.info("The server does not have file[{}] yet (or the instant upload check returned failure), start the chunk upload", fileId);
            return false;
        }
        log.info("File[{}] hit the instant upload, skip all chunk uploading", fileId);
        builder.instantUpload(true, true, response.getData());
        return true;
    }

    /**
     * 计算本次需要上传的分片
     * <pre>
     *     开启{@link ChunkUploadOptions#isSkipUploaded()}时对每个分片调用一次
     *     {@link ChunkUploadApi#isChunkUploaded}，命中的分片记入
     *     {@link ChunkUploadResult#getAlreadyUploadedChunks()}并跳过；
     *     判据本身抛异常时只记录warn，从当前分片起剩余全部按未上传处理——
     *     断点续传是优化手段，不应让查询失败拖垮本次上传，也不再对剩余分片重复发起注定失败的查询。
     * </pre>
     *
     * @param allChunks 全部切片结果
     * @param meta      文件级元信息，与传给协议接口的是同一个对象
     * @param builder   结果构建器，用于记录被跳过的分片索引
     * @return 待上传的分片集合
     */
    private List<FileChunk> resolvePendingChunks(List<FileChunk> allChunks, ChunkUploadMeta meta, ChunkUploadResult.Builder builder) {

        if (!options.isSkipUploaded() || allChunks.isEmpty()) {
            return allChunks;
        }
        String fileId = meta.getFileId();
        List<FileChunk> pendingChunks = new ArrayList<>(allChunks.size());
        List<Long> skippedIndexes = new ArrayList<>();
        for (int i = 0; i < allChunks.size(); i++) {
            FileChunk fileChunk = allChunks.get(i);
            boolean uploaded;
            try {
                uploaded = uploadApi.isChunkUploaded(meta, fileChunk);
            } catch (Exception | Error e) {
                log.warn("Failed to query the uploaded chunks of file[{}], upload all remaining chunks this time: {}", fileId, e.getMessage());
                pendingChunks.addAll(allChunks.subList(i, allChunks.size()));
                break;
            }
            if (uploaded) {
                skippedIndexes.add(fileChunk.getIndex());
            } else {
                pendingChunks.add(fileChunk);
            }
        }
        builder.alreadyUploaded(skippedIndexes);
        if (!skippedIndexes.isEmpty()) {
            log.info("File[{}] already has {} chunks on server, skip them this time", fileId, skippedIndexes.size());
        }
        return pendingChunks;
    }

    /**
     * 并发上传分片，并对未传成功的分片逐轮重传
     * <pre>
     *     一轮上传被包装成{@link Callable}交给{@link RetryUtils#callReturn(Callable, int, com.luckyframework.retry.RunBeforeRetry, com.luckyframework.retry.RetryDecider)}：
     *     任务返回本轮未传成功的分片列表，决策器从{@link com.luckyframework.retry.TaskResult}里取这个列表，
     *     用「列表非空」判定是否需要重试，退避等待由{@link FixedWaitBeforeRetry}完成，
     *     重试次数用尽时框架抛出的{@link RetryFailureException}会在这里被消化掉——
     *     重试失败不向调用方报异常，未传完的信息一惯通过{@link ChunkUploadResult}表达。
     *     待重传的分片由{@link ChunkResultClassify#getFailure()}决出，它同时涵盖
     *     「业务返回false」与「执行失败」两类，二者都需要再传一次；
     *     首轮不计入重传轮数，退避等待被中断时停止重传并还原中断标志。
     * </pre>
     *
     * @param pendingChunks 待上传的分片集合
     * @param meta          文件级元信息，与传给协议接口的是同一个对象
     * @param builder       结果构建器，用于累加成功分片与重传轮数
     * @return 重试用尽后仍未上传成功的分片结果，全部成功时为空集合
     */
    @SuppressWarnings("unchecked")
    private List<ChunkResult<Boolean>> uploadWithRetry(List<FileChunk> pendingChunks, ChunkUploadMeta meta, ChunkUploadResult.Builder builder) {

        if (pendingChunks.isEmpty()) {
            builder.retryRounds(0);
            return Collections.emptyList();
        }

        String fileId = meta.getFileId();
        int maxRetryCount = options.getMaxRetryCount();
        // 小于0表示无限重传直到全部成功，交给RetryUtils时用整型最大值表达
        int retryCount = maxRetryCount < 0 ? Integer.MAX_VALUE : maxRetryCount;
        // 下一轮要传的分片：首轮是全量，之后只传上一轮未成功的部分，不重新切分文件
        AtomicReference<List<FileChunk>> pendingRef = new AtomicReference<>(pendingChunks);
        AtomicReference<List<ChunkResult<Boolean>>> uncompleted = new AtomicReference<>(Collections.<ChunkResult<Boolean>>emptyList());
        // 任务执行次数减1即为实际重传轮数
        AtomicInteger taskInvocations = new AtomicInteger();
        try {
            RetryUtils.callReturn(() -> {
                int invocation = taskInvocations.incrementAndGet();
                ChunkResults<Boolean> results = FileChunkHandle
                        .asyncSettleHandle(pendingRef.get(), asyncTaskExecutor,
                                fileChunk -> uploadOneChunk(meta, fileChunk), options.getMaxConcurrency())
                        .join();
                ChunkResultClassify<Boolean> classify = results.classify(Boolean.TRUE::equals);
                classify.getSuccess().forEach(builder::addSuccess);
                List<ChunkResult<Boolean>> failedChunks = classify.getFailure().getResults();
                uncompleted.set(failedChunks);
                pendingRef.set(chunksOf(failedChunks));
                if (invocation > 1) {
                    log.info("File[{}] starts the {} retry of {} chunks", fileId, invocation - 1, failedChunks.size());
                }
                return failedChunks;
            }, retryCount, new FixedWaitBeforeRetry(options.getRetryBackoffMillis()),
                    taskResult -> {
                        // 决策器的入参是本轮的任务结果，只要还有未传成功的分片就继续重传
                        List<ChunkResult<Boolean>> failedChunks = taskResult.getResult();
                        return failedChunks != null && !failedChunks.isEmpty();
                    });
        } catch (RetryFailureException e) {
            // 重试用尽：框架抛出的异常在此停下，未传完的分片已经落在uncompleted里
            log.warn("File[{}] still has {} chunks not uploaded after reaching the max retry count {}, stop retrying",
                    fileId, uncompleted.get().size(), maxRetryCount);
        } catch (RetryWaiteException e) {
            // 退避等待时线程被中断：中断标志已由框架还原，此处只停止重传
            log.warn("File[{}] was interrupted while waiting for the chunk retry, {} chunks are still not uploaded",
                    fileId, uncompleted.get().size());
        } catch (Throwable e) {
            // 分片逻辑的异常已被收集进结果集，走到这里说明出现了预期外的错误，同样以结果形式上报
            log.error("File[{}] chunk upload stopped unexpectedly: {}", fileId, e.toString(), e);
            if (uncompleted.get().isEmpty()) {
                uncompleted.set(markAllFailed(pendingRef.get(), e));
            }
        } finally {
            builder.retryRounds(Math.max(0, taskInvocations.get() - 1));
        }
        return uncompleted.get();
    }

    /**
     * 调用协议的上传动作，把检查异常收敛成运行时异常
     * <pre>
     *     {@link ChunkUploadApi#uploadChunk}声明了{@code throws Exception}，而分片任务交给
     *     {@link FileChunkHandle#asyncSettleHandle}并发执行（其函数式参数是
     *     {@link java.util.function.Function}，不允许抛检查异常）；
     *     这里统一包一层，语义不变：异常照样落进该分片的{@link ChunkResult}，纳入下一轮重试。
     * </pre>
     *
     * @param meta      文件级元信息，与其它协议动作收到的是同一个对象
     * @param fileChunk 待上传的分片
     * @return 该分片是否上传成功
     */
    private boolean uploadOneChunk(ChunkUploadMeta meta, FileChunk fileChunk) {
        try {
            return uploadApi.uploadChunk(meta, fileChunk);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new ChunkUploadException(e, "Failed to upload the chunk {}", fileChunk.getIndex());
        }
    }

    /**
     * 把分片集合全部包装成失败结果
     * <pre>
     *     只在预期外的错误导致本轮上传无法产出结果时使用，
     *     保证{@link ChunkUploadResult#getFailedChunks()}仍能列出需要重传的分片。
     * </pre>
     *
     * @param fileChunks 未能产出生效结果的分片集合
     * @param error      导致上传中断的异常
     * @return 全部标记为失败的单片结果
     */
    private static List<ChunkResult<Boolean>> markAllFailed(List<FileChunk> fileChunks, Throwable error) {
        List<ChunkResult<Boolean>> results = new ArrayList<>(fileChunks.size());
        for (FileChunk fileChunk : fileChunks) {
            results.add(ChunkResult.<Boolean>failed(fileChunk, error));
        }
        return results;
    }

    /**
     * 按需通知服务端合并分片
     * <pre>
     *     只有在分片全部上传成功后才发起合并，避免服务端收到残缺文件；
     *     分片未全部成功时不合并，此时{@link ChunkUploadResult#isMerged()}与{@link ChunkUploadResult#isSuccess()}
     *     都为false，原因可从{@link ChunkUploadResult#getFailedChunks()}查到。
     *     合并时除文件级元信息外，还会把「本次切了哪些片、其中哪些片传成功了」一并交给协议
     *     （{@link ChunkMergeContext}），供需要上报片号列表、逐片MD5的合并接口取用。
     *     合并接口返回的{@link ChunkUploadResponse}中，{@code isSuccess()}决定合并成败，
     *     {@code getData()}（内部文件ID、下载URL等）透传进结果；接口返回null视为失败。
     *     合并接口抛出的异常被记录进结果而不向上传播，与分片上传保持一致的「不抛异常」约定。
     *     合并实际发起且仍失败时，若开启了{@link ChunkUploadOptions#isDeleteOnMergeFailure()}
     *     再走{@link #deleteChunksAfterMergeFailure}清理服务端分片；分片未传完而未发起合并时不清理，
     *     那些分片留着下次续传还有用。
     * </pre>
     *
     * @param meta              文件级元信息，合并所需参数全部从这里取用
     * @param chunkContext      分片清单，含全部分片与本次上传成功的分片
     * @param allChunksUploaded 全部分片是否已上传成功
     * @param builder           结果构建器
     */
    private void mergeIfNeeded(ChunkUploadMeta meta, ChunkMergeContext chunkContext, boolean allChunksUploaded, ChunkUploadResult.Builder builder) {

        boolean mergeRequired = options.isMergeAfterSuccess();
        if (!mergeRequired) {
            builder.merge(false, false, null);
            return;
        }
        String fileId = meta.getFileId();
        if (!allChunksUploaded) {
            log.warn("File[{}] has chunks not uploaded successfully, skip merging", fileId);
            builder.merge(true, false, null);
            return;
        }

        boolean merged = false;
        try {
            ChunkUploadResponse response = uploadApi.mergeChunks(meta, chunkContext);
            merged = response != null && response.isSuccess();
            if (merged) {
                builder.serverData(response.getData());
            } else {
                log.warn("The merge request of file[{}] returned failure", fileId);
            }
            builder.merge(true, merged, null);
        } catch (Exception | Error e) {
            log.error("Failed to merge the chunks of file[{}]", fileId, e);
            builder.merge(true, false, e);
        }
        if (!merged) {
            deleteChunksAfterMergeFailure(meta, builder);
        }
    }

    /**
     * 合并失败后按需删除服务端已上传的分片
     * <pre>
     *     只在{@link ChunkUploadOptions#isDeleteOnMergeFailure()}开启时发起；
     *     删除同样遵守「不抛异常、只记录结果」的约定：接口返回false或抛异常都只记入
     *     {@link ChunkUploadResult}与日志，整体成败在合并失败时已经是false，删除不另行扩大影响面；
     *     开启开关但未覆写{@link ChunkUploadApi#deleteChunks}时，默认实现抛出的
     *     {@link ChunkUploadNotImplementedException}也走异常分支被记录进{@code getDeleteError()}。
     * </pre>
     *
     * @param meta    文件级元信息，与传给协议接口的是同一个对象
     * @param builder 结果构建器
     */
    private void deleteChunksAfterMergeFailure(ChunkUploadMeta meta, ChunkUploadResult.Builder builder) {

        if (!options.isDeleteOnMergeFailure()) {
            return;
        }
        String fileId = meta.getFileId();
        try {
            boolean deleted = uploadApi.deleteChunks(meta);
            if (deleted) {
                log.info("The uploaded chunks of file[{}] are deleted from the server after the merge failed", fileId);
            } else {
                log.warn("The chunk delete request of file[{}] returned failure, chunks may still remain on the server", fileId);
            }
            builder.delete(true, deleted, null);
        } catch (Exception | Error e) {
            log.error("Failed to delete the chunks of file[{}] after the merge failed", fileId, e);
            builder.delete(true, false, e);
        }
    }

    /**
     * 回调协议的释放动作，把异常收敛掉
     * <pre>
     *     释放资源是收尾动作：它在其它流程全部结束后、结果返回或异常向外传播的最后一刻执行，
     *     自身失败既不能改写本次上传结果，也不能替换原本要向上传播的异常，
     *     因此这里吞掉异常只记error日志，保证调用方拿到的始终是上传本身的成败。
     *     协议未覆写时默认实现什么也不做，不影响任何行为；
     *     参数非法（文件不存在、fileName为空、配置不合法）在构造文件级元信息之前就被拦截，
     *     此时一个请求都没发出、也没有资源被占用，不会回调本方法。
     * </pre>
     *
     * @param meta    此刻已知的最新文件级元信息，申请标识失败时为{@code fileId}为null的那一份
     * @param context 此刻已知的分片清单，未切片时为空清单
     */
    private void releaseResources(ChunkUploadMeta meta, ChunkMergeContext context) {
        try {
            uploadApi.releaseResources(meta, context);
        } catch (Exception | Error e) {
            log.error("Failed to release the resources of file[{}]", meta.getFileId(), e);
        }
    }

    /**
     * 从重传结果中取出对应的分片
     *
     * @param chunkResults 未上传成功的分片结果集合
     * @return 待重传的分片集合，顺序与结果集合一致
     */
    private static List<FileChunk> chunksOf(List<ChunkResult<Boolean>> chunkResults) {
        List<FileChunk> chunks = new ArrayList<>(chunkResults.size());
        for (ChunkResult<Boolean> chunkResult : chunkResults) {
            chunks.add(chunkResult.getFileChunk());
        }
        return chunks;
    }

    /**
     * 算出本次实际上传成功的分片，即「本次提交上传的分片」减去「重试用尽后仍失败的分片」
     * <pre>
     *     断点续传被跳过的分片不在{@code pendingChunks}里，因此天然不会出现在结果中，
     *     与{@link ChunkMergeContext#getUploadedChunks()}的语义保持一致；
     *     两者都按片号升序，逐个过滤即可保持顺序。
     * </pre>
     *
     * @param pendingChunks 本次提交上传的分片集合
     * @param failedChunks  重试用尽后仍未上传成功的分片结果
     * @return 本次上传成功的分片集合，按片号升序
     */
    private static List<FileChunk> uploadedChunksOf(List<FileChunk> pendingChunks, List<ChunkResult<Boolean>> failedChunks) {
        if (failedChunks.isEmpty()) {
            return pendingChunks;
        }
        Set<Long> failedIndexes = new HashSet<>(failedChunks.size() * 2);
        for (ChunkResult<Boolean> failedChunk : failedChunks) {
            failedIndexes.add(failedChunk.getFileChunk().getIndex());
        }
        List<FileChunk> uploadedChunks = new ArrayList<>(pendingChunks.size() - failedIndexes.size());
        for (FileChunk pendingChunk : pendingChunks) {
            if (!failedIndexes.contains(pendingChunk.getIndex())) {
                uploadedChunks.add(pendingChunk);
            }
        }
        return uploadedChunks;
    }
}
