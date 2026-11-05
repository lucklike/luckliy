package com.luckyframework.httpclient.generalapi.chunk;

import java.util.function.Consumer;

/**
 * 分片结果分类。
 * <pre>
 *     {@link ChunkResults#classify(java.util.function.Function)}的产物，把一次分片处理的结果拆成两组：
 *     1.success：被判定为成功的分片结果，可用于统计已上传的片数；
 *     2.failure：被判定为失败的分片结果，可通过{@link ChunkResult#getFileChunk()}定位区间后重新提交。
 *
 *     两组均保持原有的分片顺序，且互斥、并集等于分类前的结果集。
 * </pre>
 *
 * @param <R> 结果类型
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 05:05
 * @see ChunkResults
 * @see ChunkResult
 */
public class ChunkResultClassify<R> {

    /**
     * 成功结果集
     */
    private final ChunkResults<R> success;

    /**
     * 失败结果集
     */
    private final ChunkResults<R> failure;

    /**
     * 构造分类结果，实例统一由{@link ChunkResults#classify(java.util.function.Function)}创建
     *
     * @param success 成功结果集
     * @param failure 失败结果集
     */
    ChunkResultClassify(ChunkResults<R> success, ChunkResults<R> failure) {
        this.success = success;
        this.failure = failure;
    }

    /**
     * 遍历成功结果集
     *
     * @param consumer 元素处理逻辑
     */
    public void successForEach(Consumer<ChunkResult<R>> consumer) {
        getSuccess().forEach(consumer);
    }

    /**
     * 遍历失败结果集
     *
     * @param consumer 元素处理逻辑
     */
    public void failureForEach(Consumer<ChunkResult<R>> consumer) {
        getFailure().forEach(consumer);
    }

    /**
     * 是否全部成功
     * <pre>
     *     语义与 {@link ChunkResults#isAllSuccess(java.util.function.Function)} 保持一致：
     *     不存在失败分片即视为全部成功，因此空结果集也返回true。
     * </pre>
     *
     * @return 是否全部成功
     */
    public boolean isAllSuccess() {
        return failure.isEmpty();
    }

    /**
     * 获取成功分片的数量
     *
     * @return 成功分片的数量
     */
    public int successSize() {
        return success.size();
    }

    /**
     * 获取失败分片的数量
     *
     * @return 失败分片的数量
     */
    public int failureSize() {
        return failure.size();
    }

    /**
     * 获取成功结果集
     *
     * @return 成功结果集
     */
    public ChunkResults<R> getSuccess() {
        return success;
    }

    /**
     * 获取失败结果集
     *
     * @return 失败结果集
     */
    public ChunkResults<R> getFailure() {
        return failure;
    }
}
