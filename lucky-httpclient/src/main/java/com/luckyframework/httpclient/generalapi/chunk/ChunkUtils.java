package com.luckyframework.httpclient.generalapi.chunk;

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
public class ChunkUtils {

    /**
     * 获取某个文件的分片信息
     *
     * @param file      文件
     * @param chunkSize 分片大小
     * @return 分片信息
     */
    @NonNull
    public static List<FileChunk> shard(@NonNull File file, long chunkSize) {

        Assert.notNull(file, "File must not be null");
        Assert.isTrue(file.exists(), "File does not exist");
        Assert.isTrue(file.isFile(), "File is not a file");

        Chunks chunks = shard(file.length(), chunkSize);
        List<FileChunk> fileChunks = new ArrayList<>(chunks.getShardCount());
        for (Chunk chunk : chunks.getChunks()) {
            fileChunks.add(new FileChunk(file, chunk));
        }
        return fileChunks;
    }

    /**
     * 获取分片信息
     *
     * @param totalSize 总大小
     * @param chunkSize 分片大小
     * @return 分片信息
     */
    @NonNull
    public static Chunks shard(long totalSize, long chunkSize) {

        Assert.isTrue(totalSize > 0L, "Total size must be greater than 0");
        Assert.isTrue(chunkSize > 0L, "Chunk size must be greater than 0");

        Chunks chunkList = new Chunks();
        chunkList.setChunkSize(chunkSize);
        chunkList.setTotal(totalSize);

        List<Chunk> chunks = new ArrayList<>();
        long begin = 0;
        long index = 0;
        while (begin <= totalSize) {
            final long end = begin + chunkSize;
            Chunk chunk = new Chunk();
            chunk.setIndex(++index);
            chunk.setStart(begin);
            chunk.setEnd(Math.min(end, totalSize));
            chunks.add(chunk);
            begin = end;
        }

        chunkList.setChunks(chunks);
        return chunkList;
    }
}
