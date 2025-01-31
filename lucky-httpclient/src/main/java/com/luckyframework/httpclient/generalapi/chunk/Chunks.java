package com.luckyframework.httpclient.generalapi.chunk;

import java.util.List;

/**
 * 分片信息集合
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 04:05
 */
public class Chunks {

    private long total;
    private long chunkSize;
    private List<Chunk> chunks;

    public int getShardCount() {
        return chunks.size();
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<Chunk> chunks) {
        this.chunks = chunks;
    }
}
