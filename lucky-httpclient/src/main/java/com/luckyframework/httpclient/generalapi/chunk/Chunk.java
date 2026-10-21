package com.luckyframework.httpclient.generalapi.chunk;

/**
 * 分片信息。
 * <pre>
 *     描述一段字节区间[start,end)以及它的序号，是纯数据对象，不含任何IO行为：
 *     1.index：分片索引，从1开始，由{@link ChunkUtils#shard(long, long)}按切分顺序递增生成；
 *     2.start：分片在原始数据中的起始位置（含），从0开始；
 *     3.end：分片在原始数据中的结束位置（不含），最后一片的end等于总大小。
 *
 *     因此{@link #getSize()}恒等于本片真实字节数，且各分片大小之和等于总大小，不存在空分片。
 *     注意与下载侧{@code Range.Index}的区间语义不同：后者使用[start,end]闭区间。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 03:34
 * @see Chunks
 * @see FileChunk
 * @see ChunkUtils
 */
public class Chunk {

    /**
     * 分片索引，从1开始
     */
    private long index;

    /**
     * 读取开始位置（含）
     */
    private long start;

    /**
     * 读取结束位置（不含）
     */
    private long end;

    /**
     * 获取分片大小
     * <pre>
     *     即本分片包含的字节数，等于end-start；
     *     由{@link ChunkUtils}生成的分片不存在end小于start的情况，因此返回值不会为负。
     * </pre>
     *
     * @return 分片大小
     */
    public long getSize() {
        return end - start;
    }

    /**
     * 获取分片索引
     *
     * @return 分片索引，从1开始
     */
    public long getIndex() {
        return index;
    }

    /**
     * 设置分片索引
     *
     * @param index 分片索引
     */
    public void setIndex(long index) {
        this.index = index;
    }

    /**
     * 获取分片开始位置（含）
     *
     * @return 分片开始位置
     */
    public long getStart() {
        return start;
    }

    /**
     * 设置分片开始位置（含）
     *
     * @param start 分片开始位置
     */
    public void setStart(long start) {
        this.start = start;
    }

    /**
     * 获取分片结束位置（不含）
     *
     * @return 分片结束位置
     */
    public long getEnd() {
        return end;
    }

    /**
     * 设置分片结束位置（不含）
     *
     * @param end 分片结束位置
     */
    public void setEnd(long end) {
        this.end = end;
    }

    /**
     * 以{@code Chunk{index=, start=, end=}}形式输出分片信息，便于日志与问题定位
     *
     * @return 分片信息描述
     */
    @Override
    public String toString() {
        return "Chunk{" +
                "index=" + index +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
