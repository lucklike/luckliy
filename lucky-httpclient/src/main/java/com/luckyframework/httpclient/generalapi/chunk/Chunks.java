package com.luckyframework.httpclient.generalapi.chunk;

import java.util.List;

/**
 * 分片信息集合。
 * <pre>
 *     一次切分的完整描述，除分片列表外还保留了切分依据，便于上层实现分片上传协议时上报总量信息：
 *     1.total：被切分数据的总大小；
 *     2.chunkSize：切分时使用的单片大小（最后一片可能小于该值）；
 *     3.chunks：按start递增排序的分片列表，空数据时为空集合而非null。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 04:05
 * @see Chunk
 * @see ChunkUtils#shard(long, long)
 */
public class Chunks {

    /**
     * 被切分数据的总大小
     */
    private long total;

    /**
     * 切分时使用的单片大小
     */
    private long chunkSize;

    /**
     * 分片列表，按开始位置递增排列
     */
    private List<Chunk> chunks;

    /**
     * 获取分片数量
     *
     * @return 分片数量
     * @throws NullPointerException 对象未通过{@link ChunkUtils#shard(long, long)}创建、分片列表为null时
     */
    public int getShardCount() {
        return chunks.size();
    }

    /**
     * 获取被切分数据的总大小
     *
     * @return 总大小
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置被切分数据的总大小
     *
     * @param total 总大小
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 获取切分时使用的单片大小
     *
     * @return 单片大小
     */
    public long getChunkSize() {
        return chunkSize;
    }

    /**
     * 设置切分时使用的单片大小
     *
     * @param chunkSize 单片大小
     */
    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * 获取分片列表
     *
     * @return 分片列表，按开始位置递增排列
     */
    public List<Chunk> getChunks() {
        return chunks;
    }

    /**
     * 设置分片列表
     *
     * @param chunks 分片列表
     */
    public void setChunks(List<Chunk> chunks) {
        this.chunks = chunks;
    }
}
