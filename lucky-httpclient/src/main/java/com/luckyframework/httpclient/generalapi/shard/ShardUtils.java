package com.luckyframework.httpclient.generalapi.shard;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 分片工具类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 03:33
 */
public class ShardUtils {

    /**
     * 获取某个文件的分片信息
     *
     * @param file      文件
     * @param shardSize 分片大小
     * @return 分片信息
     */
    @NonNull
    public static List<ShardFile> shard(@NonNull File file, long shardSize) {

        Assert.notNull(file, "File must not be null");
        Assert.isTrue(file.exists(), "File does not exist");
        Assert.isTrue(file.isFile(), "File is not a file");

        Shards shards = shard(file.length(), shardSize);
        List<ShardFile> shardFiles = new ArrayList<>(shards.getShardCount());
        for (Shard shard : shards.getShards()) {
            shardFiles.add(new ShardFile(file, shard));
        }
        return shardFiles;
    }

    /**
     * 获取分片信息
     *
     * @param totalSize 总大小
     * @param shardSize 分片大小
     * @return 分片信息
     */
    @NonNull
    public static Shards shard(long totalSize, long shardSize) {

        Assert.isTrue(totalSize > 0L, "Total size must be greater than 0");
        Assert.isTrue(shardSize > 0L, "Shard size must be greater than 0");

        Shards shardList = new Shards();
        shardList.setShardSize(shardSize);
        shardList.setTotal(totalSize);

        List<Shard> shards = new ArrayList<>();
        long begin = 0;
        long index = 0;
        while (begin <= totalSize) {
            final long end = begin + shardSize;
            Shard shard = new Shard();
            shard.setIndex(++index);
            shard.setStart(begin);
            shard.setEnd(Math.min(end, totalSize));
            shards.add(shard);
            begin = end;
        }

        shardList.setShards(shards);
        return shardList;
    }
}
