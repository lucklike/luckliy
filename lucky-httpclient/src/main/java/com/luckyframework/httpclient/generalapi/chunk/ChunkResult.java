package com.luckyframework.httpclient.generalapi.chunk;

import org.springframework.util.Assert;

/**
 * 分片处理结果。
 * <pre>
 *     把「哪个分片」与「该分片的处理结果」绑定在一起，使上层在并发处理后可以回溯每片的结果：
 *     1.fileChunk：结果所属的分片信息，可通过它拿到索引、区间与源文件；
 *     2.result：分片处理逻辑的返回值，类型由{@code R}指定，允许为null。
 *
 *     一共有三种形态，由{@link ChunkResultStatus}标识：
 *     1.SUCCESS：分片任务执行完毕，{@link #getResult()}为处理逻辑的返回值（允许为null），由{@link #of(FileChunk, Object)}创建；
 *     2.FAILED：分片任务执行中抛出异常，异常记录在{@link #getError()}，由{@link #failed(FileChunk, Throwable)}创建；
 *     3.SKIPPED：因其他分片失败触发止损，本分片未开始执行，由{@link #skipped(FileChunk)}创建。
 *
 *     两种结果集合的取法：
 *     1.{@link FileChunkHandle#asyncHandle(java.util.concurrent.Executor, java.util.function.Function, java.util.function.Consumer)}
 *       只把SUCCESS形态的结果放进{@link ChunkResults}，失败原因需从返回的future中获取；
 *     2.{@link FileChunkHandle#asyncSettleHandle(java.util.concurrent.Executor, java.util.function.Function)}
 *       三种形态全部放进{@link ChunkResults}，便于取出未完成分片做重试（如分片上传的断点续传）。
 *
 *     注意FAILED/SKIPPED表达的是「任务没跑完」，与处理逻辑返回值在业务上成功与否无关，
 *     业务维度的判断请使用{@link ChunkResults#classify(java.util.function.Function)}。
 * </pre>
 *
 * @param <R> 结果类型
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 05:05
 * @see ChunkResults
 * @see ChunkResultStatus
 * @see FileChunk
 */
public class ChunkResult<R> {

    /**
     * 分片文件信息
     */
    private final FileChunk fileChunk;

    /**
     * 返回结果，仅SUCCESS形态有意义，FAILED与SKIPPED形态为null
     */
    private final R result;

    /**
     * 执行状态
     */
    private final ChunkResultStatus status;

    /**
     * 分片任务执行中抛出的异常，仅FAILED形态非null
     */
    private final Throwable error;

    /**
     * 构造分片处理结果，实例统一通过{@link #of(FileChunk, Object)}、
     * {@link #failed(FileChunk, Throwable)}、{@link #skipped(FileChunk)}创建
     *
     * @param fileChunk 分片文件信息
     * @param result    该分片的处理结果
     * @param status    执行状态
     * @param error     执行异常，可为null
     */
    private ChunkResult(FileChunk fileChunk, R result, ChunkResultStatus status, Throwable error) {
        this.fileChunk = fileChunk;
        this.result = result;
        this.status = status;
        this.error = error;
    }

    /**
     * 创建一个执行完毕的分片处理结果
     *
     * @param fileChunk 分片文件信息
     * @param result    该分片的处理结果
     * @param <R>       结果类型
     * @return 状态为{@link ChunkResultStatus#SUCCESS}的分片处理结果
     */
    public static <R> ChunkResult<R> of(FileChunk fileChunk, R result) {
        return new ChunkResult<>(fileChunk, result, ChunkResultStatus.SUCCESS, null);
    }

    /**
     * 创建一个执行失败的分片处理结果
     *
     * @param fileChunk 分片文件信息
     * @param error     分片处理逻辑抛出的异常，不可为null
     * @param <R>       结果类型
     * @return 状态为{@link ChunkResultStatus#FAILED}的分片处理结果，其{@link #getResult()}为null
     * @throws IllegalArgumentException error为null时抛出（失败结果必须带上失败原因，否则无法定位问题）
     */
    public static <R> ChunkResult<R> failed(FileChunk fileChunk, Throwable error) {
        Assert.notNull(error, "Error must not be null");
        return new ChunkResult<>(fileChunk, null, ChunkResultStatus.FAILED, error);
    }

    /**
     * 创建一个被跳过的分片处理结果
     * <pre>
     *     因其他分片失败触发止损，本分片尚未开始执行，因此没有返回值也没有异常。
     * </pre>
     *
     * @param fileChunk 分片文件信息
     * @param <R>       结果类型
     * @return 状态为{@link ChunkResultStatus#SKIPPED}的分片处理结果
     */
    public static <R> ChunkResult<R> skipped(FileChunk fileChunk) {
        return new ChunkResult<>(fileChunk, null, ChunkResultStatus.SKIPPED, null);
    }

    /**
     * 获取结果所属的分片文件信息
     *
     * @return 分片文件信息
     */
    public FileChunk getFileChunk() {
        return fileChunk;
    }

    /**
     * 获取该分片的处理结果
     *
     * @return 处理结果，分片处理逻辑返回null时为null；FAILED与SKIPPED形态恒为null
     */
    public R getResult() {
        return result;
    }

    /**
     * 获取该分片的执行状态
     *
     * @return 执行状态
     */
    public ChunkResultStatus getStatus() {
        return status;
    }

    /**
     * 该分片任务是否执行完毕
     *
     * @return 执行完毕返回true；执行失败或被跳过返回false
     */
    public boolean isSuccess() {
        return status == ChunkResultStatus.SUCCESS;
    }

    /**
     * 获取分片任务执行中抛出的异常
     *
     * @return 执行异常，仅{@link ChunkResultStatus#FAILED}形态非null
     */
    public Throwable getError() {
        return error;
    }
}
