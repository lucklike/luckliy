package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.ChunkResult;
import com.luckyframework.httpclient.generalapi.chunk.ChunkResultClassify;
import com.luckyframework.httpclient.generalapi.chunk.ChunkResults;
import com.luckyframework.httpclient.generalapi.chunk.ChunkUtils;
import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.generalapi.chunk.FileChunkHandle;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import com.luckyframework.retry.FixedWaitBeforeRetry;
import com.luckyframework.retry.RetryFailureException;
import com.luckyframework.retry.RetryUtils;
import com.luckyframework.retry.RetryWaiteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 分片上传执行内核。
 * <pre>
 *     本类把「一个大文件怎么可靠地传完」这件事固定下来，与「服务端接口长什么样」无关：
 *     1.按{@link ChunkUploadOptions#getChunkSize()}切分文件；
 *     2.开启断点续传时逐片询问{@link ChunkUploadApi#isChunkUploaded}该片服务端是否已有，
 *       命中的分片跳过；判据本身失败时降级为全量上传；
 *     3.把分片任务提交给外部注入的{@link AsyncTaskExecutor}，并用提交窗口把在途任务数
 *       限制在{@link ChunkUploadOptions#getMaxConcurrency()}以内；
 *     4.每轮结束后把「执行异常」「被止损跳过」「接口返回false」三类分片合并成重传集合，
 *       等待{@link ChunkUploadOptions#getRetryBackoffMillis()}后只重传这部分分片，
 *       最多重传{@link ChunkUploadOptions#getMaxRetryCount()}轮（小于0表示无限重传直到全部成功），
 *       重试轮次与退避统一交由{@link RetryUtils}驱动；
 *     5.全部分片上传成功后才按需通知服务端合并；
 *     6.合并实际发起且仍失败时，按需调用{@link ChunkUploadApi#deleteChunks}回收服务端的孤儿分片
 *       （{@link ChunkUploadOptions#isDeleteOnMergeFailure()}，默认关闭）。
 *
 *     失败处理约定（重要）：
 *     1.分片失败、重试用尽、合并失败<b>都不会抛异常</b>，一律通过
 *       {@link ChunkUploadResult#isSuccess()}与{@link ChunkUploadResult#getFailedChunks()}表达，
 *       因此调用方必须主动判断结果，不能靠捕获异常来区分成败；
 *     2.只有参数非法（文件不存在、{@link ChunkUploadOptions#validate()}不通过、
 *       执行器拒绝任务等）才会抛出{@link IllegalArgumentException}，
 *       这类问题在发起任何请求之前就会被发现；
 *     3.查询分片是否已上传的判据失败不会中断本次上传，会从失败的那一片起降级为全量上传并记录warn日志；
 *     4.合并失败后的分片删除同样不抛异常，结果见{@link ChunkUploadResult#isChunksDeleted()}与
 *       {@link ChunkUploadResult#getDeleteError()}。
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
     * 分片上传一个文件，服务端保存时使用原文件名
     *
     * @param file   待上传的文件，不可为null
     * @param fileId 文件唯一标识，用于查询已上传分片与合并，不可为空串
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
     *     空文件没有任何分片，会直接进入合并阶段并以{@code totalChunks=0}通知服务端，
     *     由服务端决定如何创建空文件。
     * </pre>
     *
     * @param file     待上传的文件，不可为null
     * @param fileId   文件唯一标识，用于查询已上传分片与合并，不可为空串
     * @param fileName 服务端保存时使用的文件名，不可为空串
     * @return 上传结果；失败信息记录在结果里，不会抛异常
     * @throws IllegalArgumentException 文件不存在/不是常规文件、fileId或fileName为空，或配置不合法
     */
    public ChunkUploadResult upload(File file, String fileId, String fileName) {

        Assert.notNull(file, "File must not be null");
        Assert.hasText(fileId, "FileId must not be empty");
        Assert.hasText(fileName, "FileName must not be empty");
        // 再次校验：配置对象可能被使用者在构造内核之后修改，避免带着非法参数发起请求
        options.validate();

        ChunkUploadResult.Builder builder = new ChunkUploadResult.Builder();
        List<FileChunk> allChunks = ChunkUtils.shard(file, options.getChunkSize());
        int totalChunks = allChunks.size();
        builder.identity(fileId, fileName).metadata(file.length(), options.getChunkSize(), totalChunks);

        // 断点续传：过滤掉服务端已经收到的分片
        List<FileChunk> pendingChunks = resolvePendingChunks(allChunks, fileId, builder);
        // 并发上传+失败重传，返回重试用尽后仍未传成功的分片
        List<ChunkResult<Boolean>> failedChunks = uploadWithRetry(pendingChunks, fileId, builder);

        builder.replaceFailed(failedChunks);
        mergeIfNeeded(file, fileId, totalChunks, failedChunks.isEmpty(), builder);
        return builder.build();
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
     * @param fileId    文件唯一标识
     * @param builder   结果构建器，用于记录被跳过的分片索引
     * @return 待上传的分片集合
     */
    private List<FileChunk> resolvePendingChunks(List<FileChunk> allChunks, String fileId, ChunkUploadResult.Builder builder) {

        if (!options.isSkipUploaded() || allChunks.isEmpty()) {
            return allChunks;
        }
        List<FileChunk> pendingChunks = new ArrayList<>(allChunks.size());
        List<Long> skippedIndexes = new ArrayList<>();
        for (int i = 0; i < allChunks.size(); i++) {
            FileChunk fileChunk = allChunks.get(i);
            boolean uploaded;
            try {
                uploaded = uploadApi.isChunkUploaded(fileId, fileChunk);
            } catch (RuntimeException | Error e) {
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
     * @param fileId        文件唯一标识
     * @param builder       结果构建器，用于累加成功分片与重传轮数
     * @return 重试用尽后仍未上传成功的分片结果，全部成功时为空集合
     */
    @SuppressWarnings("unchecked")
    private List<ChunkResult<Boolean>> uploadWithRetry(List<FileChunk> pendingChunks, String fileId, ChunkUploadResult.Builder builder) {

        if (pendingChunks.isEmpty()) {
            builder.retryRounds(0);
            return Collections.emptyList();
        }

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
                                fileChunk -> uploadApi.uploadChunk(fileId, fileChunk), options.getMaxConcurrency())
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
     *     合并接口抛出的异常被记录进结果而不向上传播，与分片上传保持一致的「不抛异常」约定。
     *     合并实际发起且仍失败时，若开启了{@link ChunkUploadOptions#isDeleteOnMergeFailure()}
     *     再走{@link #deleteChunksAfterMergeFailure}清理服务端分片；分片未传完而未发起合并时不清理，
     *     那些分片留着下次续传还有用。
     * </pre>
     *
     * @param file             待上传的原始文件，交给协议实现按需要上报文件名等信息
     * @param fileId           文件唯一标识
     * @param totalChunks      分片总数，空文件为0
     * @param allChunksUploaded 全部分片是否已上传成功
     * @param builder          结果构建器
     */
    private void mergeIfNeeded(File file, String fileId, int totalChunks, boolean allChunksUploaded, ChunkUploadResult.Builder builder) {

        boolean mergeRequired = options.isMergeAfterSuccess();
        if (!mergeRequired) {
            builder.merge(false, false, null);
            return;
        }
        if (!allChunksUploaded) {
            log.warn("File[{}] has chunks not uploaded successfully, skip merging", fileId);
            builder.merge(true, false, null);
            return;
        }

        boolean merged = false;
        try {
            merged = uploadApi.mergeChunks(fileId, file, totalChunks);
            if (!merged) {
                log.warn("The merge request of file[{}] returned failure", fileId);
            }
            builder.merge(true, merged, null);
        } catch (RuntimeException | Error e) {
            log.error("Failed to merge the chunks of file[{}]", fileId, e);
            builder.merge(true, false, e);
        }
        if (!merged) {
            deleteChunksAfterMergeFailure(fileId, builder);
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
     * @param fileId  文件唯一标识
     * @param builder 结果构建器
     */
    private void deleteChunksAfterMergeFailure(String fileId, ChunkUploadResult.Builder builder) {

        if (!options.isDeleteOnMergeFailure()) {
            return;
        }
        try {
            boolean deleted = uploadApi.deleteChunks(fileId);
            if (deleted) {
                log.info("The uploaded chunks of file[{}] are deleted from the server after the merge failed", fileId);
            } else {
                log.warn("The chunk delete request of file[{}] returned failure, chunks may still remain on the server", fileId);
            }
            builder.delete(true, deleted, null);
        } catch (RuntimeException | Error e) {
            log.error("Failed to delete the chunks of file[{}] after the merge failed", fileId, e);
            builder.delete(true, false, e);
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
}
