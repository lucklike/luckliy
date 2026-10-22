package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.ChunkResult;
import com.luckyframework.httpclient.generalapi.chunk.FileChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 分片上传的最终结果。
 * <pre>
 *     由{@link ChunkUploadExecutor}在一次上传流程结束后生成，是不可变对象。
 *     无论上传是否成功，本对象都会正常返回（失败信息记录在{@link #getFailedChunks()}里），
 *     因此调用方必须主动判断{@link #isSuccess()}，不能靠异常与否来区分成败。
 *
 *     三个字段的分工：
 *     1.{@link #getSuccessChunks()}：本次实际上传成功的分片结果，可从中取到服务端返回的原始值；
 *     2.{@link #getAlreadyUploadedChunks()}：上传前服务端已存在、因而被跳过的分片索引；
 *     3.{@link #getFailedChunks()}：重试用尽后仍未上传成功的分片，失败原因见
 *       {@link ChunkResult#getError()}（为null时表示是分片接口返回了失败值）。
 *
 *     合并与清理的结果各自占三个字段，分工与分片列表一样看「是否要求-是否成功-异常」：
 *     合并见{@link #isMerged()}与{@link #getMergeError()}；
 *     合并失败后的分片删除见{@link #isDeleteRequired()}、{@link #isChunksDeleted()}与{@link #getDeleteError()}。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/10/21 10:55
 * @see ChunkUploadExecutor
 * @see ChunkUploadOptions
 */
public class ChunkUploadResult {

    /**
     * 文件唯一标识
     */
    private final String fileId;

    /**
     * 服务端保存时使用的文件名
     */
    private final String fileName;

    /**
     * 原始文件的总大小
     */
    private final long totalSize;

    /**
     * 切分时使用的单片大小
     */
    private final long chunkSize;

    /**
     * 分片总数，空文件为0
     */
    private final int totalChunks;

    /**
     * 本次实际上传成功的分片结果，按分片索引升序
     */
    private final List<ChunkResult<Boolean>> successChunks;

    /**
     * 上传前服务端已存在、本次被跳过的分片索引，按升序
     */
    private final List<Long> alreadyUploadedChunks;

    /**
     * 重试用尽后仍未上传成功的分片结果，按分片索引升序
     */
    private final List<ChunkResult<Boolean>> failedChunks;

    /**
     * 实际执行的重传轮数（不含首轮）
     */
    private final int retryRounds;

    /**
     * 是否需要在全部上传成功后合并文件
     */
    private final boolean mergeRequired;

    /**
     * 合并请求是否已执行且成功
     */
    private final boolean merged;

    /**
     * 合并请求抛出的异常，为null表示合并未发生异常（未执行合并或合并正常返回）
     */
    private final Throwable mergeError;

    /**
     * 合并失败后是否需要执行分片删除（仅在开启deleteOnMergeFailure且合并实际发起仍失败时为true）
     */
    private final boolean deleteRequired;

    /**
     * 服务端已上传的分片是否删除成功
     */
    private final boolean chunksDeleted;

    /**
     * 删除请求抛出的异常，为null表示删除未发生异常（未执行删除或删除正常返回）
     */
    private final Throwable deleteError;

    /**
     * 由{@link Builder}调用，构造最终结果对象
     *
     * @param builder 持有全部结果字段的构造器
     */
    private ChunkUploadResult(Builder builder) {
        this.fileId = builder.fileId;
        this.fileName = builder.fileName;
        this.totalSize = builder.totalSize;
        this.chunkSize = builder.chunkSize;
        this.totalChunks = builder.totalChunks;
        this.successChunks = sortedImmutableCopy(builder.successChunks, ChunkResult::getFileChunk);
        this.alreadyUploadedChunks = sortedImmutableCopy(builder.alreadyUploadedChunks);
        this.failedChunks = sortedImmutableCopy(builder.failedChunks, ChunkResult::getFileChunk);
        this.retryRounds = builder.retryRounds;
        this.mergeRequired = builder.mergeRequired;
        this.merged = builder.merged;
        this.mergeError = builder.mergeError;
        this.deleteRequired = builder.deleteRequired;
        this.chunksDeleted = builder.chunksDeleted;
        this.deleteError = builder.deleteError;
    }

    /**
     * 将入参列表拷贝、按分片索引升序排序后包装为不可变列表
     * <pre>
     *     重传时只会提交未成功的分片，因此累加得到的顺序是「首轮升序 + 重传轮升序」，
     *     这里统一排序，保证返回给调用方的三份列表始终按索引递增；
     *     同时结果对外暴露的是快照，返回后即使构建处的列表继续变化也不会影响已交付的结果。
     * </pre>
     *
     * @param list         待处理的列表，为null时视为空列表
     * @param indexGetter 从元素中取分片索引的方式
     * @param <T>          元素类型
     * @return 按索引升序的不可变列表
     */
    private static <T> List<T> sortedImmutableCopy(List<T> list, Function<T, FileChunk> indexGetter) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparingLong(chunk -> indexGetter.apply(chunk).getIndex()));
        return Collections.unmodifiableList(copy);
    }

    /**
     * 将入参列表拷贝、升序排序后包装为不可变列表
     *
     * @param list 待处理的索引列表，为null时视为空列表
     * @return 升序的不可变列表
     */
    private static List<Long> sortedImmutableCopy(List<Long> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> copy = new ArrayList<>(list);
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }

    /**
     * 是否所有分片都已上传成功
     *
     * @return 不存在{@link #getFailedChunks() 失败分片}时返回true
     */
    public boolean isAllChunksUploaded() {
        return failedChunks.isEmpty();
    }

    /**
     * 整个上传流程是否成功
     * <pre>
     *     全部分片上传成功，并且（不需要合并 或 合并已执行且成功）时返回true。
     *     需要合并但分片未全部成功时不会发起合并请求，本方法同样返回false。
     * </pre>
     *
     * @return 上传（含合并）是否整体成功
     */
    public boolean isSuccess() {
        return isAllChunksUploaded() && (!mergeRequired || merged);
    }

    /**
     * 获取文件唯一标识
     *
     * @return 文件唯一标识
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * 获取服务端保存时使用的文件名
     *
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取原始文件的总大小
     *
     * @return 总大小（字节）
     */
    public long getTotalSize() {
        return totalSize;
    }

    /**
     * 获取切分时使用的单片大小
     *
     * @return 单片大小（字节）
     */
    public long getChunkSize() {
        return chunkSize;
    }

    /**
     * 获取分片总数
     *
     * @return 分片总数，空文件为0
     */
    public int getTotalChunks() {
        return totalChunks;
    }

    /**
     * 获取本次实际上传成功的分片结果
     *
     * @return 成功分片结果列表，按索引升序，不可修改
     */
    public List<ChunkResult<Boolean>> getSuccessChunks() {
        return successChunks;
    }

    /**
     * 获取上传前服务端已存在、本次被跳过的分片索引
     *
     * @return 已有分片索引列表，按升序，不可修改
     */
    public List<Long> getAlreadyUploadedChunks() {
        return alreadyUploadedChunks;
    }

    /**
     * 获取重试用尽后仍未上传成功的分片
     * <pre>
     *     可通过{@link ChunkResult#getFileChunk()}拿到区间与索引直接发起重传；
     *     {@link ChunkResult#getError()}非null表示该分片上传时抛出了异常，
     *     为null则表示分片接口正常返回但返回值为失败（false）。
     * </pre>
     *
     * @return 失败分片结果列表，按索引升序，不可修改
     */
    public List<ChunkResult<Boolean>> getFailedChunks() {
        return failedChunks;
    }

    /**
     * 获取实际执行的重传轮数
     *
     * @return 重传轮数，不含首轮；可用于评估网络质量
     */
    public int getRetryRounds() {
        return retryRounds;
    }

    /**
     * 合并请求是否已成功执行
     *
     * @return 已合并返回true；不需要合并、分片未全部成功或合并失败都返回false
     */
    public boolean isMerged() {
        return merged;
    }

    /**
     * 获取合并请求抛出的异常
     * <pre>
     *     合并按「不抛异常、只记录结果」的约定实现：合并接口抛异常时不会传播给调用方，
     *     而是落在这里，同时{@link #isMerged()}为false、{@link #isSuccess()}为false。
     *     因分片未全部成功而未发起合并时本方法返回null，此时原因见{@link #getFailedChunks()}。
     * </pre>
     *
     * @return 合并异常，未发生异常时返回null
     */
    public Throwable getMergeError() {
        return mergeError;
    }

    /**
     * 合并失败后是否执行了分片删除请求
     * <pre>
     *     只有开启{@link ChunkUploadOptions#isDeleteOnMergeFailure()}且合并实际发起仍失败时才是true；
     *     未开启、合并成功、分片未传完而未发起合并都返回false。
     * </pre>
     *
     * @return 是否发起了删除请求
     */
    public boolean isDeleteRequired() {
        return deleteRequired;
    }

    /**
     * 服务端已上传的分片是否删除成功
     *
     * @return 删除接口返回成功时返回true；未删除、删除返回false或删除抛异常都返回false
     */
    public boolean isChunksDeleted() {
        return chunksDeleted;
    }

    /**
     * 获取删除请求抛出的异常
     * <pre>
     *     删除与合并一样按「不抛异常、只记录结果」的约定实现：删除接口抛异常时不会传播给调用方，
     *     而是落在这里，同时{@link #isChunksDeleted()}为false。
     *     未发起删除或因接口返回false而失败时本方法返回null。
     * </pre>
     *
     * @return 删除异常，未发生异常时返回null
     */
    public Throwable getDeleteError() {
        return deleteError;
    }

    /**
     * 输出便于日志查看的上传结果摘要
     *
     * @return 结果摘要，包含成功/跳过/失败的分片数量
     */
    @Override
    public String toString() {
        return "ChunkUploadResult{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", totalSize=" + totalSize +
                ", totalChunks=" + totalChunks +
                ", success=" + successChunks.size() +
                ", alreadyUploaded=" + alreadyUploadedChunks.size() +
                ", failed=" + failedChunks.size() +
                ", retryRounds=" + retryRounds +
                ", merged=" + merged +
                ", chunksDeleted=" + chunksDeleted +
                '}';
    }

    /**
     * {@link ChunkUploadResult}的构建器，仅供上传内核在同包内组装结果使用
     * <pre>
     *     结果对象本身不可变，因此需要构建器在流程中累加各阶段数据，最后一次性固化。
     * </pre>
     */
    static class Builder {

        private String fileId;
        private String fileName;
        private long totalSize;
        private long chunkSize;
        private int totalChunks;
        private int retryRounds;
        private boolean mergeRequired;
        private boolean merged;
        private Throwable mergeError;
        private boolean deleteRequired;
        private boolean chunksDeleted;
        private Throwable deleteError;
        private final List<ChunkResult<Boolean>> successChunks = new ArrayList<>();
        private final List<Long> alreadyUploadedChunks = new ArrayList<>();
        private final List<ChunkResult<Boolean>> failedChunks = new ArrayList<>();

        /**
         * 设置文件唯一标识与文件名
         *
         * @param fileId   文件唯一标识
         * @param fileName 文件名
         * @return 当前构建器
         */
        Builder identity(String fileId, String fileName) {
            this.fileId = fileId;
            this.fileName = fileName;
            return this;
        }

        /**
         * 设置切分元数据
         *
         * @param totalSize   文件总大小
         * @param chunkSize   单片大小
         * @param totalChunks 分片总数
         * @return 当前构建器
         */
        Builder metadata(long totalSize, long chunkSize, int totalChunks) {
            this.totalSize = totalSize;
            this.chunkSize = chunkSize;
            this.totalChunks = totalChunks;
            return this;
        }

        /**
         * 记录上传前服务端已存在的分片索引
         *
         * @param indexes 已被跳过的分片索引
         * @return 当前构建器
         */
        Builder alreadyUploaded(List<Long> indexes) {
            if (indexes != null) {
                this.alreadyUploadedChunks.addAll(indexes);
            }
            return this;
        }

        /**
         * 记录一个上传成功的分片
         *
         * @param chunkResult 该分片的上传结果
         * @return 当前构建器
         */
        Builder addSuccess(ChunkResult<Boolean> chunkResult) {
            this.successChunks.add(chunkResult);
            return this;
        }

        /**
         * 覆盖记录最终仍未上传成功的分片
         * <pre>
         *     每轮重传结束后都会重新计算失败集合，因此这里用替换而非累加。
         * </pre>
         *
         * @param chunkResults 本轮结束后仍未成功的分片结果
         * @return 当前构建器
         */
        Builder replaceFailed(List<ChunkResult<Boolean>> chunkResults) {
            this.failedChunks.clear();
            if (chunkResults != null) {
                this.failedChunks.addAll(chunkResults);
            }
            return this;
        }

        /**
         * 设置实际执行的重传轮数
         *
         * @param retryRounds 重传轮数，不含首轮
         * @return 当前构建器
         */
        Builder retryRounds(int retryRounds) {
            this.retryRounds = retryRounds;
            return this;
        }

        /**
         * 设置合并要求与合并结果
         *
         * @param mergeRequired 本次流程是否需要在全部成功后合并
         * @param merged        合并请求是否已执行且成功
         * @param mergeError    合并请求抛出的异常，无异常时传null
         * @return 当前构建器
         */
        Builder merge(boolean mergeRequired, boolean merged, Throwable mergeError) {
            this.mergeRequired = mergeRequired;
            this.merged = merged;
            this.mergeError = mergeError;
            return this;
        }

        /**
         * 设置合并失败后的分片删除要求与删除结果
         *
         * @param deleteRequired 本次流程是否实际发起了删除请求
         * @param chunksDeleted  删除请求是否已执行且成功
         * @param deleteError    删除请求抛出的异常，无异常时传null
         * @return 当前构建器
         */
        Builder delete(boolean deleteRequired, boolean chunksDeleted, Throwable deleteError) {
            this.deleteRequired = deleteRequired;
            this.chunksDeleted = chunksDeleted;
            this.deleteError = deleteError;
            return this;
        }

        /**
         * 固化为不可变的上传结果
         *
         * @return 分片上传结果
         */
        ChunkUploadResult build() {
            return new ChunkUploadResult(this);
        }
    }
}
