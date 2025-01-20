package com.luckyframework.httpclient.generalapi.shard;

import java.util.List;

/**
 * 分片信息集合
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 04:05
 */
public class Shards {

    private long total;
    private long shardSize;
    private List<Shard> shards;

    public int getShardCount() {
        return shards.size();
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getShardSize() {
        return shardSize;
    }

    public void setShardSize(long shardSize) {
        this.shardSize = shardSize;
    }

    public List<Shard> getShards() {
        return shards;
    }

    public void setShards(List<Shard> shards) {
        this.shards = shards;
    }
}
