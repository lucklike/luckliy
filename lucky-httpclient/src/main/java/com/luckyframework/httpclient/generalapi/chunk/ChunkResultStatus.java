package com.luckyframework.httpclient.generalapi.chunk;

/**
 * 分片处理结果的执行状态。
 * <pre>
 *     描述「分片任务本身有没有跑完」，与分片处理逻辑的返回值是否业务成功无关：
 *     1.{@link #SUCCESS}：分片任务已执行完毕（返回值业务上成功还是失败，由{@link ChunkResults#classify(java.util.function.Function)}另行判断）；
 *     2.{@link #FAILED}：分片任务执行过程中抛出了异常，异常记录在{@link ChunkResult#getError()}；
 *     3.{@link #SKIPPED}：因其他分片失败触发止损，本分片尚未开始执行就被跳过。
 *
 *     FAILED与SKIPPED都意味着该分片需要重新处理，
 *     可通过{@link ChunkResults#getUncompletedChunks()}取到这两类分片。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/10/21 10:20
 * @see ChunkResult
 * @see ChunkResults
 */
public enum ChunkResultStatus {

    /**
     * 分片任务已执行完毕
     */
    SUCCESS,

    /**
     * 分片任务执行中抛出异常
     */
    FAILED,

    /**
     * 因止损机制未开始执行分片任务
     */
    SKIPPED
}
