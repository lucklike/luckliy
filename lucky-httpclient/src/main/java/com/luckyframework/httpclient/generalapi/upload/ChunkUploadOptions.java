package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.ChunkConstants;
import org.springframework.util.Assert;

/**
 * 分片上传的行为配置。
 * <pre>
 *     描述「怎么传」而非「传给谁」，与{@link ChunkUploadApi}（服务端协议）正交。
 *     既可以直接new出来逐个set，也可以在{@link ChunkUpload}注解上以属性形式声明。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/10/21 10:50
 * @see ChunkUploadExecutor
 * @see ChunkUpload
 */
public class ChunkUploadOptions {

    /**
     * 默认分片大小（5M），取自分片包的单一事实源{@link ChunkConstants#DEFAULT_CHUNK_SIZE}
     */
    public static final long DEFAULT_CHUNK_SIZE = ChunkConstants.DEFAULT_CHUNK_SIZE;

    /**
     * 默认同时上传的分片数量，与分片下载侧的默认并发数保持一致
     */
    public static final int DEFAULT_MAX_CONCURRENCY = 8;

    /**
     * 默认重试次数，表示首轮失败后最多再重传3轮
     */
    public static final int DEFAULT_MAX_RETRY_COUNT = 3;

    /**
     * 默认重试前的等待时间（毫秒）
     */
    public static final long DEFAULT_RETRY_BACKOFF_MILLIS = 500L;

    /**
     * 单个分片的字节数
     */
    private long chunkSize = DEFAULT_CHUNK_SIZE;

    /**
     * 同时上传的分片数量上限
     */
    private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;

    /**
     * 最大重试次数，小于0表示无限重试直到全部分片上传成功
     */
    private int maxRetryCount = DEFAULT_MAX_RETRY_COUNT;

    /**
     * 每轮重试前的等待时间（毫秒）
     */
    private long retryBackoffMillis = DEFAULT_RETRY_BACKOFF_MILLIS;

    /**
     * 是否在上传前查询并跳过服务端已有的分片
     */
    private boolean skipUploaded = true;

    /**
     * 是否在所有分片上传成功后自动调用{@link ChunkUploadApi#mergeChunks}
     */
    private boolean mergeAfterSuccess = true;

    /**
     * 是否在切片前先调用{@link ChunkUploadApi#checkInstantUpload}探测秒传
     */
    private boolean instantUpload = false;

    /**
     * 是否在合并失败后调用{@link ChunkUploadApi#deleteChunks}删除服务端已上传的分片
     */
    private boolean deleteOnMergeFailure = false;

    /**
     * 校验配置是否合法，非法时抛出异常
     * <pre>
     *     由{@link ChunkUploadExecutor}在开始上传前调用，避免传到一半才因配置失败白白消耗请求；
     *     使用方一般无需直接调用。
     * </pre>
     *
     * @throws IllegalArgumentException 分片大小、并发数、等待时间不合法
     */
    public void validate() {
        Assert.isTrue(chunkSize > 0L, "Chunk size must be greater than 0");
        Assert.isTrue(chunkSize <= ChunkConstants.MAX_CHUNK_SIZE,
                "Chunk size must not be greater than " + ChunkConstants.MAX_CHUNK_SIZE);
        Assert.isTrue(maxConcurrency > 0, "Max concurrency must be greater than 0");
        Assert.isTrue(maxRetryCount >= -1, "Max retry count must be greater than or equal to -1");
        Assert.isTrue(retryBackoffMillis >= 0L, "Retry backoff millis must not be negative");
    }

    /**
     * 获取单个分片的字节数
     *
     * @return 单个分片的字节数
     */
    public long getChunkSize() {
        return chunkSize;
    }

    /**
     * 设置单个分片的字节数
     *
     * @param chunkSize 单个分片的字节数，需大于0且不超过{@link ChunkConstants#MAX_CHUNK_SIZE}
     * @return 当前配置对象，便于链式设置
     */
    public ChunkUploadOptions setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * 获取同时上传的分片数量上限
     *
     * @return 同时上传的分片数量上限
     */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    /**
     * 设置同时上传的分片数量上限
     *
     * @param maxConcurrency 分片并发数，需大于0
     * @return 当前配置对象，便于链式设置
     */
    public ChunkUploadOptions setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
        return this;
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数，小于0表示无限重试
     */
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    /**
     * 设置最大重试次数
     *
     * @param maxRetryCount 重试次数，表示首轮之外最多再重传几轮；小于0表示无限重试直到全部成功
     * @return 当前配置对象，便于链式设置
     */
    public ChunkUploadOptions setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    /**
     * 获取每轮重试前的等待时间
     *
     * @return 等待时间（毫秒）
     */
    public long getRetryBackoffMillis() {
        return retryBackoffMillis;
    }

    /**
     * 设置每轮重试前的等待时间
     *
     * @param retryBackoffMillis 等待时间（毫秒），不可为负数
     * @return 当前配置对象，便于链式设置
     */
    public ChunkUploadOptions setRetryBackoffMillis(long retryBackoffMillis) {
        this.retryBackoffMillis = retryBackoffMillis;
        return this;
    }

    /**
     * 是否会在上传前查询并跳过服务端已有的分片
     *
     * @return 跳过返回true；全量重传返回false
     */
    public boolean isSkipUploaded() {
        return skipUploaded;
    }

    /**
     * 设置是否在上传前查询并跳过服务端已有的分片
     * <pre>
     *     开启时会逐个分片调用{@link ChunkUploadApi#isChunkUploaded}，
     *     由实现方判断哪些分片服务端已存在，已有分片不再重复上传（断点续传）；
     *     服务端没有该查询能力时应关闭，否则会退化成每次全量上传。
     * </pre>
     *
     * @param skipUploaded 是否跳过已有分片
     * @return 当前配置对象，便于链式设置
     */
    public ChunkUploadOptions setSkipUploaded(boolean skipUploaded) {
        this.skipUploaded = skipUploaded;
        return this;
    }

    /**
     * 是否会在所有分片上传成功后自动通知服务端合并
     *
     * @return 自动合并返回true；由调用方自行决定何时合并返回false
     */
    public boolean isMergeAfterSuccess() {
        return mergeAfterSuccess;
    }

    /**
     * 设置是否在所有分片上传成功后自动通知服务端合并
     *
     * @param mergeAfterSuccess 是否自动合并
     * @return 当前配置对象，便于链式设置
     */
    public ChunkUploadOptions setMergeAfterSuccess(boolean mergeAfterSuccess) {
        this.mergeAfterSuccess = mergeAfterSuccess;
        return this;
    }

    /**
     * 是否在切片前先探测秒传
     *
     * @return 探测秒传返回true；直接走分片流程返回false
     */
    public boolean isInstantUpload() {
        return instantUpload;
    }

    /**
     * 设置是否在切片前先探测秒传
     * <pre>
     *     开启时{@link ChunkUploadExecutor}在切分文件之前先调用
     *     {@link ChunkUploadApi#checkInstantUpload}询问服务端该文件是否已存在，
     *     命中则跳过全部分片的对账、上传与合并，服务端返回的内部文件ID等信息
     *     可通过{@link ChunkUploadResult#getServerData()}取用；
     *     未命中或探测失败都只记warn，继续走正常分片流程；
     *     {@code checkInstantUpload}没有可兜底的默认协议（未覆写时抛
     *     {@link ChunkUploadNotImplementedException}并被降级为未命中），
     *     服务端没有秒传接口时保持关闭。
     * </pre>
     *
     * @param instantUpload 是否启用秒传探测
     * @return 当前配置对象，便于链式设置
     */
    public ChunkUploadOptions setInstantUpload(boolean instantUpload) {
        this.instantUpload = instantUpload;
        return this;
    }

    /**
     * 是否在合并失败后删除服务端已上传的分片
     * <pre>
     *     只在合并请求<b>实际发起</b>且失败（返回false或抛异常）后才会调用
     *     {@link ChunkUploadApi#deleteChunks}回收孤儿分片；
     *     分片未全部成功而未发起合并时不会删除，那些分片下次续传还要用；
     *     {@code deleteChunks}没有可兜底的默认协议（未覆写时抛{@link ChunkUploadNotImplementedException}，
     *     只会记入结果与日志、不向外抛），服务端没有删除接口时保持关闭。
     * </pre>
     *
     * @return 合并失败后删除分片返回true；保留分片返回false
     */
    public boolean isDeleteOnMergeFailure() {
        return deleteOnMergeFailure;
    }

    /**
     * 设置是否在合并失败后删除服务端已上传的分片
     *
     * @param deleteOnMergeFailure 合并失败后是否调用{@link ChunkUploadApi#deleteChunks}
     * @return 当前配置对象，便于链式设置
     */
    public ChunkUploadOptions setDeleteOnMergeFailure(boolean deleteOnMergeFailure) {
        this.deleteOnMergeFailure = deleteOnMergeFailure;
        return this;
    }
}
